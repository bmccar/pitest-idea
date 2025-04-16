package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

public class ExecutionUtils {
    /**
     * Compiles the given module and if successful initiates external execution of the given runProfile.
     *
     * @param project owning module
     * @param module to compile
     * @param runProfile to run
     */
    public static void execute(Project project, Module module, RunProfile runProfile) {
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.make(module, (aborted, errors, warnings, compileContext) -> {
            if (!aborted) {
                executePlugin(project, runProfile);
            }
        });
    }

    public static void executePlugin(Project project, RunProfile runProfile) {
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        try {
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(project, executor, runProfile);
            ProgramRunner.Callback callBack = new ProgramRunner.Callback() {
                @Override
                public void processStarted(RunContentDescriptor descriptor) {
                    descriptor.setActivateToolWindowWhenAdded(false);
                    ExecutionConsole ec = descriptor.getExecutionConsole();
                    MutationControlPanel mutationControlPanel = PitToolWindowFactory.getOrCreateControlPanel(project);
                    mutationControlPanel.setRightPaneContent(ec.getComponent());
                }
            };
            ExecutionEnvironment env = builder.build(callBack);
            env.getRunner().execute(env);
        } catch (ExecutionException ex) {
            ex.printStackTrace();  // TODO
        }
    }
}
