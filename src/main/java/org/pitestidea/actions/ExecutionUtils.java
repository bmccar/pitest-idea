package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import org.pitestidea.toolwindow.PitToolWindowFactory;

public class ExecutionUtils {
    public static void execute(Project project, RunProfile runProfile) {
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        try {
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(project, executor, runProfile);
            ProgramRunner.Callback x = new ProgramRunner.Callback() {
                @Override
                public void processStarted(RunContentDescriptor descriptor) {
                    descriptor.setActivateToolWindowWhenAdded(false);
                    ExecutionConsole ec = descriptor.getExecutionConsole();
                    PitToolWindowFactory.mutationControlPanel.setRightPaneContent(ec.getComponent());
                }
            };
            ExecutionEnvironment env = builder.build(x);
            env.getRunner().execute(env);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }
}
