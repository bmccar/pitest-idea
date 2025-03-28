package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.PitExecutionRecorder;
import org.pitestidea.psi.IPackageCollector;
import org.pitestidea.reader.CoverageFileReader;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.ICoverageRenderer;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.io.File;
import java.nio.file.FileSystems;

class PITestRunProfile implements ModuleRunProfile, IPackageCollector {
    private static final String PIT_MAIN_CLASS = "org.pitest.mutationtest.commandline.MutationCoverageReport";

    private final Project project;
    private final com.intellij.openapi.module.Module module;

    private final StringBuilder codeClasses = new StringBuilder();
    private final StringBuilder testClasses = new StringBuilder();

    public PITestRunProfile(Project project) {
        //public PITestRunProfile(String pkg, String cn, String tn, Project project, com.intellij.openapi.module.Module module) {
        this.project = project;
        this.module = ModuleManager.getInstance(project).getModules()[0];
    }

    private static StringBuilder appending(StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append(',');
        }
        return sb;
    }

    @Override
    public void acceptCodePackage(String pkg) {
        appending(codeClasses).append(pkg);
    }

    @Override
    public void acceptCodeClass(String className, String fileName) {
        appending(codeClasses).append(className);
    }

    @Override
    public void acceptTestPackage(String pkg) {
        appending(testClasses).append(pkg);
    }

    @Override
    public void acceptTestClass(String className) {
        appending(testClasses).append(className);
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
                String projectDir = IdeaDiscovery.getProjectDirectory();
                ParametersList params = javaParameters.getProgramParametersList();
                params.add("--reportDir", IdeaDiscovery.getReportDir());
                params.add("--targetClasses", codeClasses.toString());
                params.add("--targetTests", testClasses.toString());
                params.add("--sourceDirs", projectDir + "/src/main/java");
                params.add("--outputFormats", "XML,HTML");
                params.add("--exportLineCoverage", "true");
                javaParameters.setWorkingDirectory(project.getBasePath());
                javaParameters.setMainClass(PIT_MAIN_CLASS);
                PathsList classPath = javaParameters.getClassPath();
                String pitestVersion = "1.15.8";
                classPath.add(pluginJar("pitest-" + pitestVersion));
                classPath.add(pluginJar("pitest-command-line-" + pitestVersion));
                classPath.add(pluginJar("pitest-entry-" + pitestVersion));
                classPath.add(pluginJar("pitest-junit5-plugin-1.2.1"));
                classPath.add(pluginJar("junit-platform-launcher-1.9.2"));
                classPath.add(pluginJar("commons-text-1.10.0"));
                classPath.add(pluginJar("commons-lang3-3.12.0"));
                classPath.add(localBuildPath("test-classes"));
                classPath.add(localBuildPath("classes"));

                ApplicationManager.getApplication().runReadAction(() -> {
                    //PsiJavaFile file = IdeaDiscovery.getCurrentJavaFile();
                    //@Nullable com.intellij.openapi.module.Module m = ModuleUtil.findModuleForPsiElement(file);
                    addDependencies(javaParameters);
                });
                return javaParameters;
            }

            @Override
            protected @NotNull OSProcessHandler startProcess() throws ExecutionException {
                OSProcessHandler handler = super.startProcess();
                handler.addProcessListener(new ProcessAdapter() {
                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        String fn = IdeaDiscovery.getReportDir() + "/mutations.xml";
                        File file = new File(fn);
                        PitExecutionRecorder recorder = new PitExecutionRecorder();
                        ICoverageRenderer renderer = new CoverageGutterRenderer();
                        Application app = ApplicationManager.getApplication();
                        app.executeOnPooledThread(() -> {
                            app.runReadAction(() -> {
                                CoverageFileReader.read(project, file, recorder);
                            });
                            ApplicationManager.getApplication().invokeLater(() -> {
                                renderer.render(project, recorder);
                                PitToolWindowFactory.show(project, recorder);
                            });
                        });
                    }
                });
                return handler;
            }
        };
    }

    private static void addDependencies(JavaParameters params) {
        com.intellij.openapi.module.Module module = IdeaDiscovery.getModuleOf(IdeaDiscovery.getCurrentJavaFile());
        VirtualFile[] roots = ModuleRootManager.getInstance(module).orderEntries().classes().getRoots();
        PathsList classPath = params.getClassPath();
        for (VirtualFile vf : roots) {
            String url = vf.getUrl();
            String pfx = "jar://";
            if (url.startsWith(pfx)) {
                url = url.substring(pfx.length(), url.indexOf('!'));
                classPath.add(url);
            }
        }
    }

    @Override
    public String getName() {
        return "Run PITest";
    }

    @Override
    public javax.swing.Icon getIcon() {
        return null;
    }

    @Override
    public com.intellij.openapi.module.Module[] getModules() {
        return new Module[]{module};
    }
}
