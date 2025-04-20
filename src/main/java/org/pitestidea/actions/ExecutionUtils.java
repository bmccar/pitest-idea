package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.compiler.CompilerManager;
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
import java.util.function.Consumer;

public class ExecutionUtils {
    public static void execute(Module module, List<VirtualFile> virtualFiles, Consumer<Boolean> onComplete) {
        Project project = module.getProject();
        PITestRunProfile runProfile = new PITestRunProfile(project, module, virtualFiles, onComplete);
        PackageWalker.read(project, virtualFiles, runProfile);
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
        mutationControlPanel.addHistory(cachedRun, false);
        mutationControlPanel.resetHistory(project);
        if (cachedRun.isCurrent()) {
            mutationControlPanel.clearScores();
        }

        CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.make(module, (aborted, errors, warnings, compileContext) -> {
            if (!aborted) {
                executePlugin(project, runProfile);
            }
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
