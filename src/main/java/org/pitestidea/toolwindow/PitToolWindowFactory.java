package org.pitestidea.toolwindow;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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
import org.pitestidea.model.*;

import java.util.HashMap;
import java.util.Map;
import com.intellij.openapi.diagnostic.Logger;

public final class PitToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOGGER = Logger.getInstance(PitToolWindowFactory.class);


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

    /**
     * Updates the scores and execution history in the toolwindow. The scores content is set to reflect the
     * values in the provided recorder.
     *
     * @param project project
     * @param cachedRun to update scores from
     * @param includesPackages true if there was at least one package in the inputs selected by the user
     */
    public static void show(Project project, CachedRun cachedRun, boolean includesPackages) {
        Viewing.PackageChoice packageChoice = includesPackages
                ? Viewing.PackageChoice.PACKAGE
                : Viewing.PackageChoice.NONE;
        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        mutationControlPanel.setPackageSelection(packageChoice);
        mutationControlPanel.setSortSelection(Sorting.By.PROJECT);
        mutationControlPanel.setDirSelection(Sorting.Direction.ASC);
        mutationControlPanel.setOptionsChangeFn(resort -> reshow(project, mutationControlPanel, cachedRun, resort));
        mutationControlPanel.reloadHistory(project);
        reshow(project, mutationControlPanel, cachedRun, false);
    }

    public static void showPitExecutionOutputOnly(Project project) {
        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        mutationControlPanel.clearScores();
        mutationControlPanel.setFullConsole();
        ToolWindow tw = getToolWindow(project);
        if (tw != null) {
            tw.activate(()->{});
        }
    }

    private static void reshow(Project project, MutationControlPanel mutationControlPanel, CachedRun cachedRun, boolean resort) {
        if (resort) {
            PitExecutionRecorder recorder = cachedRun.getRecorder();
            recorder.sort(mutationControlPanel.getSortSelection(),mutationControlPanel.getDirSelection());
        }
        mutationControlPanel.clearScores();

        ToolWindow tw = getToolWindow(project);
        if (tw != null) {
            if (tw.isActive()) {
                mutationControlPanel.reloadScores(cachedRun);
            } else {
                tw.activate(() -> mutationControlPanel.onFirstActivation(cachedRun));
            }
        }
    }

    private static @Nullable ToolWindow getToolWindow(Project project) {
        String id = "PITest";
        return ToolWindowManager.getInstance(project).getToolWindow(id);
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

    public static void addAll(Project project, MutationControlPanel mutationControlPanel, PitExecutionRecorder recorder) {
        recorder.visit(new HierarchyPlanner(project, mutationControlPanel.getLevel()));
        mutationControlPanel.refresh();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOGGER.info("Creating tool window content for project " + project.getName());
        MutationControlPanel mutationControlPanel = getOrCreateControlPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mutationControlPanel.getContentPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
        Application application = ApplicationManager.getApplication();
        application.executeOnPooledThread(() -> {
            try {
                PitRepo.reloadReports(project);
                mutationControlPanel.reloadReports(project);
                // Update the UI on the Event Dispatch Thread
                application.invokeLater(() -> {
                    mutationControlPanel.getContentPanel().updateUI();
                });
            } catch (Exception e) {
                LOGGER.error("Failed to reload reports", e);
            }
        });
    }
}
