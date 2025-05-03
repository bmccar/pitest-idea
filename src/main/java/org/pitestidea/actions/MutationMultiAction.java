package org.pitestidea.actions;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            ExecutionUtils.execute(module, virtualFiles);
        }
    }

    public static Module getModuleForVirtualFile(Project project, VirtualFile file) {
        if (project == null || file == null) {
            return null;
        }

        return ModuleUtilCore.findModuleForFile(file, project);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isInCodeBase(e));
    }

    private static boolean isInCodeBase(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return false;
        }

        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedFile == null) {
            return false;
        }
        Module module = getModuleForVirtualFile(project,selectedFile);
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

        for (ContentEntry entry : moduleRootManager.getContentEntries()) {
            for (SourceFolder sourceFolder : entry.getSourceFolders()) {
                VirtualFile directory = sourceFolder.getFile();
                if (directory != null) {
                    String path = directory.getPath();
                    if (selectedFile.getPath().startsWith(path)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
