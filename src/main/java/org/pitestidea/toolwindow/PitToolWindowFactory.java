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
import org.jetbrains.annotations.Nullable;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.model.PitExecutionRecorder;

import java.util.HashMap;
import java.util.Map;

public final class PitToolWindowFactory implements ToolWindowFactory, DumbAware {

    public static final Map<String, MutationControlPanel> controlPanels = new HashMap<>();

    public static MutationControlPanel getControlPanel(Project project) {
        MutationControlPanel panel = controlPanels.get(project.getName());
        if (panel == null) {
            throw new IllegalStateException("Internal error: no control panel for project " + project.getName());
        }
        return panel;
    }

    public static MutationControlPanel getOrCreateControlPanel(Project project) {
        return controlPanels.computeIfAbsent(project.getName(), k -> new MutationControlPanel());
    }

    public static void show(Project project, PitExecutionRecorder recorder, boolean includesPackages) {
        Viewing.PackageChoice packageChoice = includesPackages
                ? Viewing.PackageChoice.PACKAGE
                : Viewing.PackageChoice.NONE;
        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        mutationControlPanel.setPackageSelection(packageChoice);
        mutationControlPanel.setSortSelection(Sorting.By.PROJECT);
        mutationControlPanel.setDirSelection(Sorting.Direction.ASC);
        mutationControlPanel.setOptionsChangeFn(resort -> reshow(project, mutationControlPanel, recorder, resort));
        reshow(project, mutationControlPanel, recorder, false);
    }

    public static void showPitExecutionOutputOnly(Project project) {
        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        mutationControlPanel.clear();
        mutationControlPanel.setFullConsole();
        ToolWindow tw = getToolWindow(project);
        if (tw != null) {
            tw.activate(()->{});
        }
    }

    private static void reshow(Project project, MutationControlPanel mutationControlPanel, PitExecutionRecorder recorder, boolean resort) {
        System.out.println("reshowing " + resort);
        if (resort) {
            recorder.sort(mutationControlPanel.getSortSelection(),mutationControlPanel.getDirSelection());
        }
        mutationControlPanel.clear();

        ToolWindow tw = getToolWindow(project);
        if (tw != null) {
            if (tw.isActive()) {
                addAll(project, mutationControlPanel, recorder);
            } else {
                tw.activate(() -> addAll(project, mutationControlPanel, recorder));
            }
        }
    }

    private static @Nullable ToolWindow getToolWindow(Project project) {
        String id = "PITest";
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
        return tw;
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
            MutationControlPanel.Level nextLevel = level;
            MutationControlPanel mutationControlPanel = getControlPanel(project);
            Viewing.PackageChoice pkgSelection = mutationControlPanel.getPackageSelection();
            boolean includeLine = diver.isTopLevel();
            includeLine |= pkgSelection == Viewing.PackageChoice.PACKAGE;
            includeLine |= pkgSelection == Viewing.PackageChoice.CODE && diver.hasCodeFileChildren();
            if (includeLine) {
                nextLevel = level.setLine(project, pkg, score);
            }
            diver.apply(new HierarchyPlanner(project, nextLevel));
        }
    }

    private static void addAll(Project project, MutationControlPanel mutationControlPanel, PitExecutionRecorder recorder) {
        recorder.visit(new HierarchyPlanner(project, mutationControlPanel.getLevel()));
        mutationControlPanel.refresh();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("Creating tool window content !!!");

        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mutationControlPanel.getContentPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
    }
}
