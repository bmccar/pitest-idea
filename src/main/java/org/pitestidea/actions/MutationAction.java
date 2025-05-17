package org.pitestidea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;

import java.util.List;

public class MutationAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PsiJavaFile javaFile = IdeaDiscovery.getCurrentJavaFile(project);
        Module module = ModuleUtilCore.findModuleForPsiElement(javaFile);

        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        VirtualFile currentFile = IdeaDiscovery.getCurrentFile();

        if (module != null && currentFile != null) {
            ExecutionUtils.execute(module, List.of(currentFile));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || selectedFile == null) {
            e.getPresentation().setEnabledAndVisible(false);
        } else {
            String loc = IdeaDiscovery.onLocationOf(project, selectedFile,
                    "Run PITest against this file using its test",
                    "Run PITest for this test file against its source");
            if (loc == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setText(loc);
            e.getPresentation().setEnabledAndVisible(true);
        }
    }
}

