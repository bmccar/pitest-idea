package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.*;
import org.pitestidea.model.InputBundle;
import org.pitestidea.psi.PackageWalker;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.util.Comparator;
import java.util.List;

public class ExecutionUtils {
    private static final Logger LOGGER = Logger.getInstance(ExecutionUtils.class);

    public static void execute(Module module, InputBundle bundle) {
        Project project = module.getProject();
        List<VirtualFile> vfs = bundle.asPath()
                .transform(_c->true, s-> {
                    VirtualFile file = IdeaDiscovery.findVirtualFileByRQN(project,s);
                    if (file == null) {
                        throw new IllegalArgumentException("Unable to find file for " + s);
                    }
                    return file;
                });
        execute(module,vfs);
    }

    public static void execute(Module module, List<VirtualFile> unsortedFiles) {
        // Sort so that there is a consistently ordered list of files no matter what order they came in from the menu selection
        final List<VirtualFile> sortedFiles = unsortedFiles.stream().sorted(Comparator.comparing(VirtualFile::getPath)).toList();

        Project project = module.getProject();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    ApplicationManager.getApplication().runReadAction(() -> {
                    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                    VirtualFile sourceRoot = fileIndex.getSourceRootForFile(sortedFiles.get(0));
                    });
                });

        InputBundle inputBundle = new InputBundle();
        ReadAction.run(() -> {
            PackageWalker.read(project, sortedFiles, inputBundle);
        });

        if (inputBundle.isEmpty(InputBundle.Category::isSource)) {
            Messages.showErrorDialog(project, "Unable to find any matching source files for this input request. Please multi-select with at least one source entry from Project view.", "Pointless Execution");
            return;
        }
        if (inputBundle.isEmpty(InputBundle.Category::isTest)) {
            Messages.showErrorDialog(project, "Unable to find any matching test files for this input request. Please multi-select with at least one test entry from Project view.", "Pointless Execution");
            return;
        }

        PITestRunProfile runProfile = new PITestRunProfile(project, module, inputBundle);
        execute(project, module, runProfile);
    }

    /**
     * Compiles the given module and if successful initiates external execution of the given runProfile.
     *
     * @param project    owning module
     * @param module     to compile
     * @param runProfile to run
     */
    private static void execute(Project project, Module module, PITestRunProfile runProfile) {
        MutationControlPanel mutationControlPanel = PitToolWindowFactory.getOrCreateControlPanel(project);
        CachedRun cachedRun = runProfile.getCachedRun();
        cachedRun.setRunState(RunState.RUNNING);
        mutationControlPanel.reloadHistory(project);
        if (cachedRun.isCurrent()) {
            mutationControlPanel.clearScores(cachedRun);
        }

        CompilerManager compilerManager = CompilerManager.getInstance(project);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteAction.run(() -> {
                compilerManager.make(module, (aborted, errors, warnings, compileContext) -> {
                    if (aborted || errors > 0) {
                        LOGGER.error(String.format("Pre-PIT compilation aborted, errors=%d, warnings=%d", errors, warnings));
                        cachedRun.setRunState(RunState.FAILED);
                        mutationControlPanel.reloadHistory(project);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            String msg = errors > 0 ? "There are compilation errors, please fix these before running PIT" : "Compilation aborted";
                            Messages.showErrorDialog(project, msg, "Pre-PIT Compilation");
                        });
                    } else {
                        try {
                            executePlugin(project, runProfile);
                        } catch (RuntimeException | ExecutionException e) {
                            LOGGER.error("Failed to execute PIT", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
            });
        });
    }

    private static void executePlugin(Project project, PITestRunProfile runProfile) throws ExecutionException {
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(project, executor, runProfile);
        ProgramRunner.Callback callBack = descriptor -> {
            descriptor.setActivateToolWindowWhenAdded(false);
            ExecutionConsole ec = descriptor.getExecutionConsole();
            MutationControlPanel mutationControlPanel = PitToolWindowFactory.getOrCreateControlPanel(project);

            mutationControlPanel.setRightPaneContent(ec.getComponent());
            runProfile.setOutputConsole((ConsoleView) ec.getComponent());
        };
        ExecutionEnvironment env = builder.build(callBack);
        env.getRunner().execute(env);
    }
}
