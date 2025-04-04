// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.pitestidea.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.model.PitExecutionRecorder;

public final class PitToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final MutationControlPanel mutationControlPanel = new MutationControlPanel();

    public static void show(Project project, PitExecutionRecorder recorder) {
        mutationControlPanel.setPackageSelectionChangeFn(_mcp -> reshow(project, recorder));
        reshow(project, recorder);
    }

    private static void reshow(Project project, PitExecutionRecorder recorder) {
        mutationControlPanel.clear();

        String id = "PITest tool window";
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
        if (tw != null) {
            if (tw.isActive()) {
                addAll(project, recorder);
            } else {
                tw.activate(() -> addAll(project, recorder));
            }
        }
    }

    /**
     * Accepts directory/file lines and, based on user selections, filters/groups/orders them
     * into a set of lines for display.
     */
    private record HierarchyPlanner(Project project,
                                    MutationControlPanel.Level level) implements PitExecutionRecorder.FileVisitor {

        @Override
        public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
            String filePath = file.getPath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            level.setLine(project, file, fileName, score);
        }

        @Override
        public void visit(String pkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
            MutationControlPanel.PackageType pkgSelection = mutationControlPanel.getPackageSelection();
            if (pkgSelection== MutationControlPanel.PackageType.NONE) {
                diver.apply(new HierarchyPlanner(project, level));
            } else {
                MutationControlPanel.Level nested = level.setLine(project, pkg, score);
                diver.apply(new HierarchyPlanner(project, nested));
            }
        }
    }

    private static void addAll(Project project, PitExecutionRecorder recorder) {
        recorder.visit(new HierarchyPlanner(project, mutationControlPanel.getLevel()));
        mutationControlPanel.refresh();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("Creating tool window content !!!");

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mutationControlPanel.getPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
    }
}
