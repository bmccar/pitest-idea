package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.pitestidea.actions.ExecutionUtils;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.*;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.FileOpenCloseListener;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Results tool window. Consists of several panes with different levels of interactivity.
 */
public class MutationControlPanel {

    private final VerticalList historyList = new VerticalList();
    private final StretchPane stretchPane = new StretchPane();
    private final JScrollPane rightScrollPane = new JBScrollPane();
    private final ClickTree tree = new ClickTree();
    private Consumer<Boolean> optionsChangeFn = null;
    private EnumRadio<Viewing.PackageChoice> packageSelector;
    private EnumRadio<Sorting.By> sortSelector;
    private EnumRadio<Sorting.Direction> dirSelector;
    private boolean isGutterIconsEnabled = true;
    JBColor runButtonColor = new JBColor(new Color(67, 117, 68), new Color(71, 145, 72));
    JBColor cancelButtonColor = new JBColor(new Color(161, 45, 55), new Color(204, 102, 102));

    public MutationControlPanel() {
        stretchPane.setLeft(createScoresPanel());
        stretchPane.setRight(createConsolePane());

        stretchPane.setState(StretchPane.PaneState.SCORES);
    }

    private JPanel createConsolePane() {
        JPanel fullPanel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.add(stretchPane.getConsoleButton(), BorderLayout.WEST);
        fullPanel.add(header, BorderLayout.NORTH);
        fullPanel.add(rightScrollPane, BorderLayout.CENTER);

        return fullPanel;
    }

    private JComponent createScoresPanel() {
        JSplitPane scoresPanel = new JSplitPane();

        JComponent historyPanel = historyList.getComponent();
        scoresPanel.setLeftComponent(historyPanel);

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(createScoresHeaderPanel(), BorderLayout.NORTH);
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        scoresPanel.setRightComponent(treePanel);

        double split = 0.5;
        scoresPanel.setDividerLocation(split);
        scoresPanel.setResizeWeight(split);
        return scoresPanel;
    }

    private JPanel createScoresHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS)); // Horizontal layout
        header.add(createPackagePanel());
        header.add(createSortPanel());
        header.add(createDirPanel());

        header.add(Box.createHorizontalGlue());
        header.add(createRemoveButton());
        header.add(Box.createHorizontalGlue());
        header.add(stretchPane.getScoresButton());
        return header;
    }

    private JComponent createRemoveButton() {
        Project project = IdeaDiscovery.getActiveProject();
        JCheckBox checkBox = new JCheckBox("Show PIT icons");
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.setSelected(isGutterIconsEnabled);
        checkBox.addActionListener(e -> {
            isGutterIconsEnabled = checkBox.isSelected();
            if (isGutterIconsEnabled) {
                FileOpenCloseListener.replayOpenFiles(project);
            } else {
                CoverageGutterRenderer.removeGutterIcons(project);
            }
        });
        return checkBox;
    }

    private JPanel createPackagePanel() {
        packageSelector = new EnumRadio<>(Viewing.PackageChoice.values(),"Filter",
                Viewing.PackageChoice::getDisplayName,
                type -> optionsChangeFn.accept(false));
        packageSelector.setSelected(Viewing.PackageChoice.PACKAGE); // Default value
        return packageSelector.getPanel();
    }

    private JPanel createSortPanel() {
        sortSelector = new EnumRadio<>(Sorting.By.values(),"Sort",
                Sorting.By::getDisplayName,
                type -> optionsChangeFn.accept(true));
        sortSelector.setSelected(Sorting.By.PROJECT); // Default value
        return sortSelector.getPanel();
    }

    private JPanel createDirPanel() {
        dirSelector = new EnumRadio<>(Sorting.Direction.values(),"Sort Direction",
                Sorting.Direction::getDisplayName,
                type -> optionsChangeFn.accept(true));
        dirSelector.setSelected(Sorting.Direction.ASC); // Default value

        return dirSelector.getPanel();
    }

    public void setOptionsChangeFn(Consumer<Boolean> optionsChangeFn) {
        this.optionsChangeFn = optionsChangeFn;
    }

    public Viewing.PackageChoice getPackageSelection() {
        return packageSelector.getSelected();
    }

    public void setPackageSelection(Viewing.PackageChoice packageChoice) {
        packageSelector.setSelected(packageChoice);
    }

    public Sorting.By getSortSelection() {
        return sortSelector.getSelected();
    }

    public void setSortSelection(Sorting.By sortChoice) {
        sortSelector.setSelected(sortChoice);
    }

    public Sorting.Direction getDirSelection() {
        return dirSelector.getSelected();
    }

    public void setDirSelection(Sorting.Direction dirChoice) {
        dirSelector.setSelected(dirChoice);
    }

    public JComponent getContentPanel() {
        return stretchPane.getComponent();
    }

    public void clearScores() {
        tree.clearExistingRows();
        tree.refresh();
    }

    public void refresh() {
        tree.refresh();
    }

    public Level getLevel() {
        return new Level(tree.getRootTreeLevel());
    }

    /**
     * Sets the body of the right panel. This is provided because the console component is
     * generated externally on PIT execution.
     *
     * @param component to set as right component (inside an already existing scroll pane)
     */
    public void setRightPaneContent(JComponent component) {
        rightScrollPane.setViewportView(component);
    }

    public void setFullConsole() {
        stretchPane.setFullConsole();
    }

    public void clearHistory() {
        historyList.clear();
    }

    public void resetHistory(Project project) {
        // TODO reuse existing row data rather than starting from scratch each time
        clearHistory();
        PitRepo.apply(project, (c,_current)->addHistory(c));
        historyList.getComponent().updateUI();
    }

    /**
     * Adds a row in the history pane representing the state of the supplied CachedRun.
     * Does not check for duplicates.
     *
     * @param cachedRun to read from
     */
    public void addHistory(CachedRun cachedRun) {
        ExecutionRecord record = cachedRun.getExecutionRecord();
        boolean highlightRow = cachedRun.isCurrent();
        boolean valid = cachedRun.getRunState() != RunState.FAILED;
        JPanel row = historyList.addRow(record.getReportName(),highlightRow, valid, ()->{
            cachedRun.activate();
        });
        TransitionButton button = new TransitionButton();
        RunState runState = cachedRun.getRunState();
        boolean readyToCancel = runState == RunState.RUNNING;
        button.addState("Run", runButtonColor, !readyToCancel, () -> run(cachedRun, button));
        button.addState("Cancel", cancelButtonColor, readyToCancel, () -> cancel(cachedRun, button));
        row.add(button);
    }

    private boolean run(CachedRun cachedRun, TransitionButton button) {
        Module module = cachedRun.ensureLoaded().getModule();
        ExecutionRecord record = cachedRun.getExecutionRecord();
        List<VirtualFile> vfs = record.getInputFiles().stream().map(file ->
                LocalFileSystem.getInstance().findFileByPath(file)).toList();
        ExecutionUtils.execute(module, vfs, (_success) -> {
            button.transition();
        });
        return true;
    }

    private boolean cancel(CachedRun cachedRun, TransitionButton button) {
        if (cachedRun.cancel()) {
            button.transition();
            return true;
        }
        return false;
    }

    public void markScoresInvalid() {
        tree.resetToRootMessage("Select a run from the history list to the left.");
    }

    public boolean isGutterIconsEnabled() {
        return isGutterIconsEnabled;
    }

    public static class Level {
        private final ClickTree.TreeLevel treeLevel;

        Level(ClickTree.TreeLevel treeLevel) {
            this.treeLevel = treeLevel;
        }

        public void setLine(Project project, VirtualFile file, String fileName, IMutationScore score) {
            treeLevel.addClickableFileRow(project, file, createLine(fileName, score.getScore()));
        }

        public Level setLine(Project project, String pkgName, IMutationScore score) {
            return new Level(treeLevel.addPackageRow(createLine(pkgName, score.getScore())));

        }
    }

    private static String createLine(String text, float score) {
        String space = score == 100 ? "" : score > 10 ? "&nbsp;" : "&nbsp;&nbsp;";
        return String.format("<html>%d%%%s&nbsp;%s</html>", (int) score, space, text);
    }
}


