package org.pitestidea.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;

public class ExecutionUtils {
    public static void execute(Project project, RunProfile runProfile) {
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();

        try {
            ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(project, executor, runProfile).build();
            env.getRunner().execute(env);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }
}
