package org.pitestidea.configuration;

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
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.nio.file.FileSystems;

public class PitIJRunConfiguration extends RunConfigurationBase<PitIJRunConfigurationOptions> {

  private static final String PIT_MAIN_CLASS = "org.pitest.mutationtest.commandline.MutationCoverageReport";

  public PitIJRunConfiguration(Project project,
                               ConfigurationFactory factory,
                               String name) {
    super(project, factory, name);
  }

  @NotNull
  @Override
  protected PitIJRunConfigurationOptions getOptions() {
    return (PitIJRunConfigurationOptions) super.getOptions();
  }

  public String getScriptName() {
    return getOptions().getScriptName();
  }

  public void setScriptName(String scriptName) {
    getOptions().setScriptName(scriptName);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new PitIJSettingsEditor();
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor,
                                  @NotNull ExecutionEnvironment environment) {
    String pkg = IdeaDiscovery.getCurrentPackageName();
    String cn = IdeaDiscovery.getCurrentClassName();
    String tn = IdeaDiscovery.getCurrentTestClassName();


    return new JavaCommandLineState(environment) {
      private String pluginJar(String fe) {
        String fs = FileSystems.getDefault().getSeparator();
        String path = PathManager.getPluginsPath()+fs+"pitest-idea"+fs+"lib"+fs;
        return path + fe + ".jar";
      }

      private String localBuildPath(String fe) {
        String fs = FileSystems.getDefault().getSeparator();
        return IdeaDiscovery.getProjectDirectory()+fs+"target"+fs+fe;
      }

      @Override
      protected JavaParameters createJavaParameters() throws ExecutionException {
        Project project = IdeaDiscovery.getActiveProject();
        JavaParameters javaParameters = new JavaParameters();
        javaParameters.setJdk(ProjectRootManager.getInstance(project).getProjectSdk());
        javaParameters.setUseClasspathJar(true);
        String projectDir = IdeaDiscovery.getProjectDirectory();
        ParametersList params = javaParameters.getProgramParametersList();
        params.add("--reportDir",IdeaDiscovery.getReportDir());
        params.add("--targetClasses",pkg+'.'+cn);
        params.add("--targetTests",pkg+'.'+tn);
        params.add("--sourceDirs", projectDir + "/src/main/java");
        params.add("--outputFormats", "XML,HTML");
        params.add("--exportLineCoverage", "true");
        javaParameters.setWorkingDirectory(project.getBasePath());
        javaParameters.setMainClass(PIT_MAIN_CLASS);
        PathsList classPath = javaParameters.getClassPath();
        String pitestVersion = "1.15.8";
        classPath.add(pluginJar("pitest-"+pitestVersion));
        classPath.add(pluginJar("pitest-command-line-"+pitestVersion));
        classPath.add(pluginJar("pitest-entry-"+pitestVersion));
        classPath.add(pluginJar("pitest-junit5-plugin-1.2.1"));
        classPath.add(pluginJar("junit-platform-launcher-1.9.2"));
        classPath.add(pluginJar("commons-text-1.10.0"));
        classPath.add(pluginJar("commons-lang3-3.12.0"));
        classPath.add(localBuildPath("test-classes"));
        classPath.add(localBuildPath("classes"));

        Application application = ApplicationManager.getApplication();
        System.out.println("Outside " + application.isDispatchThread());
        ApplicationManager.getApplication().runReadAction(()->{
          System.out.println("Inside " + application.isDispatchThread());
          PsiJavaFile file = IdeaDiscovery.getCurrentJavaFile();
          @Nullable Module m = ModuleUtil.findModuleForPsiElement(file);
          addDependencies(javaParameters);
        });
        return javaParameters;
      }

      private static void addDependencies(JavaParameters params) {
        Module module = IdeaDiscovery.getModuleOf(IdeaDiscovery.getCurrentJavaFile());
        VirtualFile[] roots = ModuleRootManager.getInstance(module).orderEntries().classes().getRoots();
        PathsList classPath = params.getClassPath();
        for (VirtualFile vf: roots) {
          String url = vf.getUrl();
          String pfx = "jar://";
          if (url.startsWith(pfx)) {
            url = url.substring(pfx.length(), url.indexOf('!'));
            classPath.add(url);
          }
        }
      }

      @NotNull
      @Override
      protected OSProcessHandler startProcess() throws ExecutionException {
        //System.out.println("startProcess " + ApplicationManager.getApplication().isDispatchThread());
        OSProcessHandler handler = super.startProcess();
        handler.addProcessListener(new ProcessAdapter() {
          public void processTerminated(@NotNull ProcessEvent event) {
            /* TODO REMOVE?
            String fn = IdeaDiscovery.getReportDir() + "/mutations.xml";
            File file = new File(fn);
            ICoverageRecorder recorder = new CoverageGutterRenderer();
            Application app = ApplicationManager.getApplication();
            System.out.println("> " + 1);
            app.runReadAction(()->{
              System.out.println("> " + 2);
              CoverageFileReader.read(file,"todo...!!!",recorder);
              recorder.render();
              System.out.println("> " + 3);
            });

            System.out.println("> " + 4);
            //PitToolWindowFactory.show();
             */
          }
        });
        return handler;
      }
    };



/*
    return new CommandLineState(environment) {
      @NotNull
      @Override
      protected ProcessHandler startProcess() throws ExecutionException {


        GeneralCommandLine commandLine =
            new GeneralCommandLine(getOptions().getScriptName());
        OSProcessHandler processHandler = ProcessHandlerFactory.getInstance()
            .createColoredProcessHandler(commandLine);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
      }
    };
 */
  }



}
