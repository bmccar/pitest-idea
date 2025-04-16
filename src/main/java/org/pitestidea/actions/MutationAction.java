package org.pitestidea.actions;

//import com.intellij.execution.JavaCommandLineState;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.lang3.StringUtils;
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

        PsiJavaFile javaFile = IdeaDiscovery.getCurrentJavaFile();
        Module module = ModuleUtilCore.findModuleForPsiElement(javaFile);

        String pkg = IdeaDiscovery.getCurrentPackageName();
        String cn = IdeaDiscovery.getCurrentClassName();
        String tn = IdeaDiscovery.getCurrentTestClassName();

        PITestRunProfile runProfile = new PITestRunProfile(project, module);
        String pfx = StringUtils.isEmpty(pkg) ? "" : pkg + ".";
        String qn = pfx + cn;

        runProfile.acceptCodeClass(qn,null);
        runProfile.acceptTestClass(qn + "Test");

        ExecutionUtils.execute(project, module, runProfile);
    }
}

