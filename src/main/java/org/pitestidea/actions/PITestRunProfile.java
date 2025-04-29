package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.*;
import org.pitestidea.psi.IPackageCollector;
import org.pitestidea.reader.MutationsFileReader;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import javax.swing.*;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.List;

/**
 * Defines how the PIT process is run, in accordance with the IJ framework. This includes the steps taken when
 * the process completes. See {@link ExecutionUtils} for how the process is initiated.
 */
public class PITestRunProfile implements ModuleRunProfile, IPackageCollector {
    private static final String PIT_MAIN_CLASS = "org.pitest.mutationtest.commandline.MutationCoverageReport";
    private static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/pitest.svg", CoverageGutterRenderer.class);

    private final Project project;
    private final com.intellij.openapi.module.Module module;

    private final CachedRun cachedRun;
    private final StringBuilder codeClasses = new StringBuilder();
    private final StringBuilder testClasses = new StringBuilder();

    private ConsoleView consoleView;

    PITestRunProfile(Project project, Module module, List<VirtualFile> virtualFiles) {
        this.project = project;
        this.module = module;
        List<String> inputs = virtualFiles.stream().map(VirtualFile::getPath).toList();
        ExecutionRecord record = new ExecutionRecord(inputs);
        //VirtualFile vf = getVirtualFile(module, virtualFiles, record);
        this.cachedRun = PitRepo.register(module, record);
    }

    void setOutputConsole(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    private static StringBuilder appending(StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append(',');
        }
        return sb;
    }

    @Override
    public void acceptCodePackage(String pkg) {
        cachedRun.setIncludesPackages();
        StringBuilder sb = appending(codeClasses);
        sb.append(pkg);
        sb.append(".*");
    }

    @Override
    public void acceptCodeClass(String qualifiedClassName, String fileName) {
        appending(codeClasses).append(qualifiedClassName);
    }

    @Override
    public void acceptTestPackage(String pkg) {
        StringBuilder sb = appending(testClasses);
        sb.append(pkg);
        sb.append(".*");
    }

    @Override
    public void acceptTestClass(String qualifiedClassName) {
        appending(testClasses).append(qualifiedClassName);
    }

    private String pluginJar(String fe) {
        String fs = FileSystems.getDefault().getSeparator();
        String path = PathManager.getPluginsPath() + fs + "pitest-idea" + fs + "lib" + fs;
        return path + fe + ".jar";
    }

    private String localBuildPath(String fe) {
        String fs = FileSystems.getDefault().getSeparator();
        return IdeaDiscovery.getProjectDirectory() + fs + "target" + fs + fe;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        //System.out.println("*** codeClasses: " + codeClasses.toString());
        //System.out.println("*** testClasses: " + testClasses.toString());
        return new JavaCommandLineState(environment) {
            @Override
            protected JavaParameters createJavaParameters() {
                JavaParameters javaParameters = new JavaParameters();
                javaParameters.setJdk(ProjectRootManager.getInstance(project).getProjectSdk());
                javaParameters.setUseClasspathJar(true);
                String projectDir = IdeaDiscovery.getAbsolutePathOfModule(module);
                ParametersList params = javaParameters.getProgramParametersList();
                File reportDir = cachedRun.getReportFileDir();
                params.add("--reportDir", reportDir.getPath());
                params.add("--targetClasses", codeClasses.toString());
                params.add("--targetTests", testClasses.toString());
                params.add("--sourceDirs", projectDir + "/src/main/java");
                params.add("--outputFormats", "XML,HTML");
                params.add("--exportLineCoverage", "true");
                //params.add("--verbose", "true");
                javaParameters.setWorkingDirectory(project.getBasePath());
                javaParameters.setMainClass(PIT_MAIN_CLASS);
                PathsList classPath = javaParameters.getClassPath();

                // Versions must match build.gradle.kts
                String pitestVersion = "1.15.8";
                String pitJunit5PluginVersion = "1.2.1";

                // TODO Newer versions not working
                // String pitestVersion = "1.18.2";
                // String pitJunit5PluginVersion = "1.2.2";
                // classPath.add(pluginJar("junit-platform-launcher-1.12.2"));

                classPath.add(pluginJar("pitest-" + pitestVersion));
                classPath.add(pluginJar("pitest-command-line-" + pitestVersion));
                classPath.add(pluginJar("pitest-entry-" + pitestVersion));
                classPath.add(pluginJar("pitest-junit5-plugin-"+pitJunit5PluginVersion));
                classPath.add(pluginJar("junit-platform-launcher-1.9.2"));
                classPath.add(pluginJar("commons-text-1.10.0"));
                classPath.add(pluginJar("commons-lang3-3.12.0"));
                classPath.add(localBuildPath("test-classes"));
                classPath.add(localBuildPath("classes"));

                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        JavaParametersUtil.configureModule(PITestRunProfile.this.module, javaParameters, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                return javaParameters;
            }

            @Override
            protected @NotNull OSProcessHandler startProcess() throws ExecutionException {
                // Avoiding leaving previous icons while executing, else users may be confused that they represent the current result
                //CoverageGutterRenderer.removeGutterIcons(project);
                cachedRun.prepareForRun();  // Clean house to avoid confusion in case PIT fails or is cancelled

                OSProcessHandler handler = super.startProcess();
                cachedRun.setProcessHandler(handler);

                handler.addProcessListener(new ProcessAdapter() {
                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        final RunState runState = cachedRun.getRunState();
                        cachedRun.getExecutionRecord().markFinished();
                        RunState newRunState = runState;
                        final MutationControlPanel mutationControlPanel = PitToolWindowFactory.getControlPanel(project);
                        if (runState != RunState.CANCELLED) {
                            File reportDirectory = cachedRun.getReportFileDir();
                            int code = event.getExitCode();
                            //boolean status;
                            if (code == 0 && reportDirectory.exists() && reportDirectory.isDirectory()) {
                                onSuccess(cachedRun, mutationControlPanel);
                                writeConsoleReportLink();
                                newRunState = RunState.COMPLETED;
                            } else {
                                react("PIT execution error", "View output", () -> {
                                    // Activate this row since console output is exposed and should be consistent
                                    // with scores even though it's unlikely user would examine the latter
                                    cachedRun.activate();
                                    PitToolWindowFactory.showPitExecutionOutputOnly(project);
                                }, () -> {
                                    if (cachedRun.isCurrent()) {
                                        // If current then ensure scores has proper message displayed
                                        mutationControlPanel.reloadReports(project);
                                    }
                                });
                                newRunState = RunState.FAILED;
                            }
                        }
                        if (runState != newRunState) {
                            cachedRun.setRunState(newRunState);
                        }
                        mutationControlPanel.handleCompletion(cachedRun);
                    }

                    private void react(String msg, String yesText, Runnable yesFn, Runnable noFn) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (MessageDialogBuilder.okCancel("PIT Execution Completed", msg)
                                    .yesText(yesText)
                                    .noText("Ignore")
                                    .icon(PLUGIN_ICON)
                                    .ask(project)) {
                                yesFn.run();
                            } else {
                                noFn.run();
                            }
                        });
                    }

                    private void onSuccess(CachedRun cachedRun, MutationControlPanel mutationControlPanel) {
                        PitExecutionRecorder recorder = cachedRun.getRecorder();
                        Application app = ApplicationManager.getApplication();
                        ExecutionUtils.dumpThreads("onSuccess OUTER");
                        app.executeOnPooledThread(() -> {
                            ExecutionUtils.dumpThreads("onSuccess MIDDLE");
                            app.invokeLater(() -> {
                                ExecutionUtils.dumpThreads("onSuccess INNER");
                                app.runWriteAction(() -> cachedRun.getExecutionRecord().writeToDirectory(cachedRun.getReportFileDir()));
                            });
                            File src = cachedRun.getMutationsFile();
                            app.runReadAction(() -> MutationsFileReader.read(project, src, recorder));

                            String msg = cachedRun.getExecutionRecord().getHtmlListOfInputs("PIT execution completed for", false);

                            app.invokeLater(() -> {
                                react(msg, "Show Report", () -> {
                                    cachedRun.activate();
                                    mutationControlPanel.setFullScores();
                                }, () -> {
                                    if (cachedRun.isCurrent()) {
                                        mutationControlPanel.markScoresInvalid();
                                    }
                                });
                            });
                        });
                    }
                });
                return handler;
            }
        };
    }

    public static String simpleNameOfPath(String path) {
        int start = path.lastIndexOf('/');
        int end = path.lastIndexOf('.');
        if (end < 0) end = path.length();
        return path.substring(start+1,end);
    }

    private void writeConsoleReportLink() {
        consoleView.print("\n*** Open results in browser ", ConsoleViewContentType.NORMAL_OUTPUT);
        String url = "file://" + cachedRun.getReportDir() + "/index.html";
        consoleView.printHyperlink("here", new HyperlinkInfo() {
            @Override
            public void navigate(@NotNull Project project) {
                com.intellij.ide.BrowserUtil.browse(url);
            }
        });
        consoleView.print("\n\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public CachedRun getCachedRun() {
        return cachedRun;
    }

    @Override
    public @NotNull String getName() {
        return "Run PITest";
    }

    @Override
    public javax.swing.Icon getIcon() {
        return null;
    }

    @Override
    public com.intellij.openapi.module.Module @NotNull [] getModules() {
        return new Module[]{module};
    }
}
