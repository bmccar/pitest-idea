package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PathsList;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.GradleUtils;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.constants.PluginVersions;
import org.pitestidea.model.*;
import org.pitestidea.reader.InvalidMutatedFileException;
import org.pitestidea.reader.MutationsFileReader;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines how the PIT process is run, in accordance with the IJ framework. This includes the steps taken when
 * the process completes. See {@link ExecutionUtils} for how a PIT execution is initiated.
 */
public class PITestRunProfile implements ModuleRunProfile {
    private static final Logger LOGGER = Logger.getInstance(PITestRunProfile.class);

    private static final String PIT_MAIN_CLASS = "org.pitest.mutationtest.commandline.MutationCoverageReport";
    private static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/pitest.svg", CoverageGutterRenderer.class);

    private final Project project;
    private final com.intellij.openapi.module.Module module;

    private final CachedRun cachedRun;
    private final InputBundle inputBundle;

    private ConsoleView consoleView;
    private String junitVersion = null;

    PITestRunProfile(Project project, Module module, InputBundle inputBundle) {
        this.project = project;
        this.module = module;
        ExecutionRecord record = new ExecutionRecord(inputBundle);
        this.cachedRun = PitRepo.register(module, record);
        this.inputBundle = inputBundle;
    }

    void setOutputConsole(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    private static String starAll(String s) {
        return s.isEmpty() ? "*" : s + ".*";
    }

    private static String windozePath(String s) {
        if (File.separatorChar == '\\') {
            s = s.replace('/', '\\');
        }
        return s;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        String codeClasses = Stream.concat(
                inputBundle.asQn().get(c -> c == InputBundle.Category.SOURCE_FILE).stream(),
                inputBundle.asQn().transform(c -> c == InputBundle.Category.SOURCE_PKG, PITestRunProfile::starAll).stream()
        ).collect(Collectors.joining(","));

        String testClasses = Stream.concat(
                inputBundle.asQn().get(c -> c == InputBundle.Category.TEST_FILE).stream(),
                inputBundle.asQn().transform(c -> c == InputBundle.Category.TEST_PKG, PITestRunProfile::starAll).stream()
        ).collect(Collectors.joining(","));

        return new JavaCommandLineState(environment) {
            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                try {
                    final JavaParameters javaParameters = createJavaParametersInternal();
                    final List<String> classPathsAdded = ClassPathConfigurator.updateClassPathBundles(javaParameters.getClassPath());
                    cachedRun.setClassPath(new ClassPaths(javaParameters.getClassPath().getPathList(), classPathsAdded));
                    return javaParameters;
                } catch (RuntimeException e) {
                    String msg = e.getMessage();
                    if (msg == null) {
                        LOGGER.warn("Unable to create JavaParameters on exception", e);
                    } else {
                        LOGGER.warn("Unable to create JavaParameters on exception: " + msg);
                    }
                    cachedRun.setRunState(RunState.FAILED);
                    // Activate here because there doesn't seem to be any other way to synchronously handle
                    // this exception update the screen
                    cachedRun.activate();
                    throw new ExecutionException(e);
                }
            }

            private JavaParameters createJavaParametersInternal() {

                JavaParameters javaParameters = new JavaParameters();
                javaParameters.setJdk(ProjectRootManager.getInstance(project).getProjectSdk());
                javaParameters.setUseClasspathJar(true);

                PathsList classPath = javaParameters.getClassPath();

                String mutableCodePath = windozePath(IdeaDiscovery.getAndSetClassPathOptions(module, classPath));

                String projectDir = IdeaDiscovery.getAbsolutePathOfModule(module);
                ParametersList params = javaParameters.getProgramParametersList();
                File reportDir = cachedRun.getReportFileDir();
                params.add("--reportDir", reportDir.getPath());
                params.add("--targetClasses", codeClasses);
                params.add("--targetTests", testClasses);
                params.add("--mutableCodePaths", mutableCodePath);
                params.add("--sourceDirs", windozePath(projectDir + "/src/main/java"));
                params.add("--outputFormats", "XML,HTML");
                params.add("--exportLineCoverage", "true");
                MutationControlPanel mutationControlPanel = PitToolWindowFactory.getOrCreateControlPanel(project);
                if (mutationControlPanel.isPitVerboseEnabled()) {
                    params.add("--verbose", "true");
                }
                javaParameters.setWorkingDirectory(IdeaDiscovery.getAbsolutePathOfModule(module));
                javaParameters.setMainClass(PIT_MAIN_CLASS);

                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        // For Gradle, JavaParametersUtil call doesn't include Provided elements like with Maven,
                        // so first try to set Gradle-style, but fall-back to Maven-style if the current project is not a Gradle project
                        if (!GradleUtils.configureFromGradleClasspath(module, javaParameters)) {
                            JavaParametersUtil.configureModule(PITestRunProfile.this.module, javaParameters, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null);
                        }
                        junitVersion = extractJUnitVersion(javaParameters);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }

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
                cachedRun.prepareForRun();  // Clean house to avoid confusion in case PIT fails or is canceled

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
                            if (code == 0 && reportDirectory.exists() && reportDirectory.isDirectory()) {
                                if (onSuccess(cachedRun, mutationControlPanel)) {
                                    writeConsoleReportLink();
                                    newRunState = RunState.COMPLETED;
                                } else {
                                    newRunState = RunState.FAILED;
                                }
                            } else {
                                String msg = createErrorMessage();
                                react(msg, "View output", () -> {
                                    // Activate this row since console output is exposed and should be consistent
                                    // with scores even though it's unlikely a user would examine the latter
                                    cachedRun.activate();
                                    PitToolWindowFactory.showPitExecutionOutputOnly(project);
                                }, () -> {
                                    if (cachedRun.isCurrent()) {
                                        // If cachedRun is current, then ensure scores it has the proper message displayed
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

                    private boolean onSuccess(CachedRun cachedRun, MutationControlPanel mutationControlPanel) {
                        final PitExecutionRecorder recorder = cachedRun.getRecorder();
                        final Application app = ApplicationManager.getApplication();
                        final AtomicBoolean anyErrors = new AtomicBoolean(false);
                        app.executeOnPooledThread(() -> {
                            File src = cachedRun.getMutationsFile();
                            app.runReadAction(() -> {
                                try {
                                    MutationsFileReader.read(project, src, recorder);
                                } catch (InvalidMutatedFileException e) {
                                    anyErrors.set(true);
                                }
                            });
                            if (!anyErrors.get()) {
                                app.invokeLater(() -> app.runWriteAction(() -> cachedRun.getExecutionRecord().writeToDirectory(cachedRun.getReportFileDir())));
                            }
                            displayResultPopup(cachedRun, mutationControlPanel, app, anyErrors.get());
                        });
                        return !anyErrors.get();
                    }

                    private void displayResultPopup(CachedRun cachedRun, MutationControlPanel mutationControlPanel, Application app, boolean errors) {
                        String text = cachedRun.getExecutionRecord().getHtmlListOfInputs();
                        String pfx = errors ? "&nbsp;&nbsp;<i>(with errors, see log)</i><br>" : "";
                        String msg = pfx + text;

                        app.invokeLater(() -> react(msg, "Show Report", () -> {
                            cachedRun.activate();
                            mutationControlPanel.setFullScores();
                        }, () -> {
                            if (cachedRun.isCurrent()) {
                                mutationControlPanel.markScoresInvalid();
                            }
                        }));
                    }
                });
                return handler;
            }
        };
    }

    private String createErrorMessage() {
        String msg = "PIT execution error";
        if (isLowerThanBundledVersion(junitVersion)) {
            msg += ".<p>It appears you're using an older version ("
                    + junitVersion
                    + ") of JUnit than supported out of the box by this plugin."
                    + " You can either upgrade your JUnit version to one greater than "
                    + PluginVersions.LOWEST_BUNDLED_JUNIT_VERSION
                    + " or follow the configuration advice <a href='https://bmccar.github.io/pitest-idea/configuration.html'>here</a>.";
        }
        return msg;
    }

    private static boolean isLowerThanBundledVersion(String junitVersion) {
        if (junitVersion != null) {
            SemVer versionUsed = SemVer.parseFromText(junitVersion);
            SemVer lowestBundledVersion = SemVer.parseFromText(PluginVersions.LOWEST_BUNDLED_JUNIT_VERSION);
            return versionUsed != null && lowestBundledVersion != null && lowestBundledVersion.isGreaterThan(versionUsed);
        }
        return false;
    }

    private static String extractJUnitVersion(JavaParameters javaParameters) {
        String fs = File.separator;
        final String match = "junit-jupiter-api" + fs;
        @NotNull List<String> ps = javaParameters.getClassPath().getPathList();
        for (String next : ps) {
            int ix = next.indexOf(match);
            if (ix >= 0) {
                int ij = next.indexOf(fs, ix + match.length());
                return next.substring(ix + match.length(), ij);
            }
        }
        return null;
    }

    private void writeConsoleReportLink() {
        consoleView.print("\n*** Open results in browser ", ConsoleViewContentType.NORMAL_OUTPUT);
        consoleView.printHyperlink("here", project -> IdeaDiscovery.openBrowserTo(IdeaDiscovery.getUrl(cachedRun)));
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
