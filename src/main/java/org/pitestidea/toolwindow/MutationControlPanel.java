package org.pitestidea.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.actions.ExecutionUtils;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.*;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.FileOpenCloseListener;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Manages execution results tool window. Consists of several panes with different levels of interactivity.
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
    private JButton clearAllButton;
    private boolean isGutterIconsEnabled = true;

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

        scoresPanel.setLeftComponent(createHistoryPanel());

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(createScoresHeaderPanel(), BorderLayout.NORTH);
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        scoresPanel.setRightComponent(treePanel);

        double split = 0.5;
        scoresPanel.setDividerLocation(split);
        scoresPanel.setResizeWeight(split);
        return scoresPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createHistoryHeaderPanel(), BorderLayout.NORTH);
        panel.add(historyList.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryHeaderPanel() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel options = new JPanel();
        options.setBorder(BorderFactory.createLineBorder(JBColor.BLACK));
        clearAllButton = createClearAllButton();
        options.add(clearAllButton);
        header.add(options);
        return header;
    }

    private @NotNull JButton createClearAllButton() {
        JButton button = new JButton("Clear All");
        Project project = IdeaDiscovery.getActiveProject();
        button.addActionListener(e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (MessageDialogBuilder.okCancel("Really delete all PIT reports for this project?", "Cannot be undone")
                        .yesText("Confirm")
                        .noText("Cancel")
                        .ask(project)) {
                    PitRepo.deleteHistory(project);
                    reloadReports(project);
                }
            });
            clearHistory();
        });
        button.setEnabled(false);  // enabled on report load
        return button;
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

    public void reloadReports(Project project) {
        CachedRun current = reloadHistory(project);
        if (current==null) {
            reloadScoresMsg(project, "No current history. Initiate PIT execution from a drop-down menu.");
        } else {
            reloadScores(current);
        }
    }

    public CachedRun reloadHistory(Project project) {
        clearHistory();
        AtomicReference<CachedRun> currentRun = new AtomicReference<>();
        AtomicBoolean any = new AtomicBoolean(false);
        PitRepo.apply(project, (c,current)->{
            any.set(true);
            if (current) {
                currentRun.set(c);
            }
            addHistory(c);
        });
        clearAllButton.setEnabled(any.get());
        historyList.getComponent().updateUI();
        return currentRun.get();
    }

    private void reloadScoresMsg(Project project, String msg) {
        CoverageGutterRenderer.removeGutterIcons(project);
        clearScores();
        tree.resetToRootMessage(msg);
    }

    public void reloadScores(CachedRun cachedRun) {
        if (cachedRun.isCurrent()) {
            RunState runState = cachedRun.getRunState();
            switch (runState) {
                case RUNNING:
                    tree.resetToRootMessage("PIT execution is still running. Please wait.");
                    return;
                case FAILED:
                    tree.resetToRootMessage("PIT execution failed. See console for details.");
                    return;
                case CANCELLED:
                    tree.resetToRootMessage("Run was cancelled. Rerun in order to see report");
                    return;
            }
            Project project = IdeaDiscovery.getActiveProject();
            CoverageGutterRenderer.removeGutterIcons(project);
            clearScores();
            PitToolWindowFactory.addAll(project,this,cachedRun.getRecorder());
        }
    }

    private static final Icon runIcon = IconLoader.getIcon("/icons/run.svg", MutationControlPanel.class);
    private static final Icon killIcon = IconLoader.getIcon("/icons/killProcess.svg", MutationControlPanel.class);
    private static final Icon deleteIcon = IconLoader.getIcon("/icons/delete.svg", MutationControlPanel.class);


    /**
     * Adds a row in the history pane representing the state of the supplied CachedRun.
     * Does not check for duplicates.
     *
     * @param cachedRun to read from
     */
    public void addHistory(CachedRun cachedRun) {
        ExecutionRecord record = cachedRun.getExecutionRecord();
        boolean isCurrent = cachedRun.isCurrent();
        RunState runState = cachedRun.getRunState();
        System.out.println("addHistory runState="+runState+" isCurrent="+isCurrent+" record="+record.getReportName());
        boolean valid = runState.isValid();
        JPanel row = historyList.addRow(record.getReportName(), isCurrent, valid, cachedRun::activate);
        TransitionButton button = new TransitionButton();
        boolean readyToCancel = runState == RunState.RUNNING;
        TransitionButton.State running = button.addState("Run", runIcon, "Rerun this report", !readyToCancel, () -> run(cachedRun, button));
        button.addState("Cancel", killIcon, "Cancel this report execution", readyToCancel, () -> cancel(cachedRun, button, running));
        row.add(button);

        JButton trashButton = createDeleteButton(cachedRun);
        row.add(trashButton);
    }

    private @NotNull JButton createDeleteButton(CachedRun cachedRun) {
        JButton button = new JButton(deleteIcon);
        button.setToolTipText("Delete this report");
        Project project = IdeaDiscovery.getActiveProject();
        button.addActionListener(e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (MessageDialogBuilder.okCancel("Really delete this report?", "Cannot be undone")
                        .yesText("Confirm")
                        .noText("Cancel")
                        .ask(project)) {
                    if (cachedRun.isCurrent()) {
                        reloadScoresMsg(project, "Select a different report to the left.");
                    }
                    cachedRun.deleteFilesForThisRun();
                    reloadHistory(project);
                }
            });
        });
        button.setOpaque(false); // Don't fill the button background
        button.setContentAreaFilled(false); // Transparent content area
        button.setBorderPainted(false); // Optionally remove border
        button.setPreferredSize(TransitionButton.BUTTON_SIZE);
        button.setMinimumSize(TransitionButton.BUTTON_SIZE);
        button.setMaximumSize(TransitionButton.BUTTON_SIZE);
        return button;
    }

    private boolean run(CachedRun cachedRun, TransitionButton button) {
        Module module = cachedRun.ensureLoaded().getModule();
        ExecutionRecord record = cachedRun.getExecutionRecord();
        List<VirtualFile> vfs = record.getInputFiles().stream().map(file ->
                LocalFileSystem.getInstance().findFileByPath(file)).toList();
        ExecutionUtils.execute(module, vfs);
        return true;
    }

    public void handleCompletion(CachedRun cachedRun) {
        reloadHistory(cachedRun.getProject());
    }

    private boolean cancel(CachedRun cachedRun, TransitionButton button, TransitionButton.State state) {
        if (cachedRun.cancel()) {
            reloadScores(cachedRun);
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


