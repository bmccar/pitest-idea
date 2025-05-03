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
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.*;
import org.pitestidea.psi.PackageWalker;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.util.List;

public class ExecutionUtils {
    private static final Logger LOGGER = Logger.getInstance(ExecutionUtils.class);

    public static void execute(Module module, List<VirtualFile> virtualFiles) {
        Project project = module.getProject();
        PITestRunProfile runProfile = new PITestRunProfile(project, module, virtualFiles);
        ReadAction.run(()-> {
                    PackageWalker.read(project, virtualFiles, runProfile);
                });
        execute(project, module, runProfile);
    }

    /**
     * Compiles the given module and if successful initiates external execution of the given runProfile.
     *
     * @param project owning module
     * @param module to compile
     * @param runProfile to run
     */
    public static void execute(Project project, Module module, PITestRunProfile runProfile) {
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
                    if (!aborted) {
                        try {
                            executePlugin(project, runProfile);
                        } catch (RuntimeException e) {
                            LOGGER.error("Failed to execute PIT", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
            });
        });
    }

    private static void executePlugin(Project project, PITestRunProfile runProfile) {
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        try {
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
        } catch (ExecutionException ex) {
            // TODO restore state
            throw new RuntimeException(ex);
        }
    }
}
