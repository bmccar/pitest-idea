package org.pitestidea.actions;

//import com.intellij.execution.JavaCommandLineState;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;

public class MutationAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length == 0) return;

        String pkg = IdeaDiscovery.getCurrentPackageName();
        String cn = IdeaDiscovery.getCurrentClassName();
        String tn = IdeaDiscovery.getCurrentTestClassName();

        PITestRunProfile runProfile = new PITestRunProfile(project);
        String qn = pkg + "." + cn;
        runProfile.acceptCodeClass(qn,null);
        runProfile.acceptTestClass(qn + "Test");

        ExecutionUtils.execute(project, modules[0], runProfile);
    }

}

