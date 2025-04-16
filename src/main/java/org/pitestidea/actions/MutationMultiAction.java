package org.pitestidea.actions;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.psi.PackageWalker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MutationMultiAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length == 0) return;

        Object @Nullable [] selectedNodes = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
        if (selectedNodes != null) {
            List<VirtualFile> virtualFiles = Arrays.stream(selectedNodes).map(n -> {
                if (n instanceof ProjectViewNode) {
                    return ((ProjectViewNode<?>) n).getVirtualFile();
                }
                return null;
            }).filter(Objects::nonNull).toList();

            Module module = getModuleForVirtualFile(project,virtualFiles.get(0));

            PITestRunProfile runProfile = new PITestRunProfile(project, module);
            PackageWalker.read(project, virtualFiles, runProfile);

            ExecutionUtils.execute(project, module, runProfile);
        }
    }

    public static Module getModuleForVirtualFile(Project project, VirtualFile file) {
        if (project == null || file == null) {
            return null; // Make sure project and file are not null
        }

        // Use ModuleUtilCore to find the module
        return ModuleUtilCore.findModuleForFile(file, project);
    }



    @Override
    public void update(@NotNull AnActionEvent e) {
        /*
        // Enable only when clicking on a package node
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        ProjectViewNode<?> selectedNode = e.getData(ProjectViewNode.DATA_KEY);
        boolean isPackage = selectedNode != null && PsiManager.getInstance(project).findDirectory(selectedNode.getVirtualFile())
                .getPackage() != null;

        e.getPresentation().setEnabledAndVisible(isPackage);
         */
    }
}
