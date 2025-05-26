package org.pitestidea.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.actions.ExecutionUtils;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.*;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.FileOpenCloseListener;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Manages the plugin tool window. Consists of several panes with different levels of interactivity:
 * <ul>
 *     <li>Execution history that saves each PIT run
 *     <li>Scores result for a given PIT execution run
 *     <li>Console output from the last PIT run
 * </ul>
 * These panes are stretchable and include buttons to expand or collapse the panes.
 */
public final class MutationControlPanel {
    private static final Logger LOGGER = Logger.getInstance(MutationControlPanel.class);

    private final HistoryList historyList = new HistoryList();
    private final StretchPane stretchPane = new StretchPane(null);

    private final JScrollPane rightScrollPane = new JBScrollPane();
    private final ClickTree tree = new ClickTree();
    private @Nullable Consumer<DisplayChoices> optionsChangeFn = null;
    private EnumRadio<Viewing.PackageChoice> packageSelector;
    private EnumRadio<Sorting.By> sortSelector;
    private EnumRadio<Sorting.Direction> dirSelector;
    private JButton clearAllButton;
    private boolean isGutterIconsEnabled = true;
    private boolean isPitVerbose = false;
    private int headerHeight;  // For aligning headers across different panes
    private final AtomicInteger activeRuns = new AtomicInteger(0);

    public MutationControlPanel() {
        stretchPane.setLeft(createScoresPanel());  // Run 1st to set headerHeight;
        stretchPane.setRight(createConsolePane());
        stretchPane.setState(StretchPane.PaneState.SCORES);
    }

    private JPanel createConsolePane() {
        JPanel fullPanel = new JPanel(new BorderLayout());

        JPanel header = createConsoleHeaderPanel();

        header.setPreferredSize(new Dimension(20, headerHeight + 4));
        fullPanel.add(header, BorderLayout.NORTH);
        fullPanel.add(rightScrollPane, BorderLayout.CENTER);

        return fullPanel;
    }

    private JPanel createConsoleHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        Box box = Box.createHorizontalBox();
        box.add(stretchPane.getConsoleButton());
        box.add(Box.createHorizontalGlue());
        box.add(createPitVerboseButton());
        header.add(box, BorderLayout.CENTER);
        return header;
    }



    private JSplitPane scoresSplitPane;

    private JComponent createScoresPanel() {
        JPanel scoresTreePanel = new JPanel();
        scoresTreePanel.setLayout(new BoxLayout(scoresTreePanel, BoxLayout.Y_AXIS));

        JPanel scoresHeaderPanel = createScoresHeaderPanel();
        JComponent comp = tree.getComponent();

        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMinimumSize(scoresHeaderPanel.getMinimumSize());
        comp.setPreferredSize(scoresHeaderPanel.getPreferredSize());
        comp.setMaximumSize(scoresHeaderPanel.getMaximumSize());

        scoresTreePanel.setMinimumSize(scoresHeaderPanel.getMinimumSize());

        // Make sure buttonPanel fills the entire width
        scoresHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoresHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                scoresHeaderPanel.getPreferredSize().height));

        scoresTreePanel.add(scoresHeaderPanel);
        scoresTreePanel.add(comp);

        scoresSplitPane = new JSplitPane();

        scoresSplitPane.setLeftComponent(createHistoryPanel());
        scoresSplitPane.setRightComponent(scoresTreePanel);

        setDefaultScoresSplit();

        return new JBScrollPane(scoresSplitPane);
    }

    public void setDefaultScoresSplit() {
        double split = 0.3;
        scoresSplitPane.setDividerLocation(split);
        scoresSplitPane.setResizeWeight(split);
    }

    public void onFirstActivation(CachedRun cachedRun) {
        reloadScores(cachedRun);
        // Quirk: the values in this method do not take effect until the split is visible
        setDefaultScoresSplit();
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
        button.setToolTipText("Delete all PIT reports for this project that were generated by this plugin. Does <i>not</i> delete reports generated by other plugins or by the PIT CLI.");
        Project project = IdeaDiscovery.getActiveProject();
        button.addActionListener(e -> ApplicationManager.getApplication().invokeLater(() -> {
            if (MessageDialogBuilder.okCancel("Really delete all PIT reports for this project?", "Cannot be undone")
                    .yesText("Confirm")
                    .noText("Cancel")
                    .ask(project)) {
                PitRepo.deleteHistory(project);
                clearHistory();
                clearScores(project);
                PitRepo.reloadReports(project);
            }
        }));
        button.setEnabled(false);  // enabled when a report is loaded
        return button;
    }

    private JPanel createScoresHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        Box box = Box.createHorizontalBox();
        box.add(createPackagePanel());
        box.add(createSortPanel());
        box.add(createDirPanel());

        box.add(Box.createHorizontalGlue());
        box.add(createRemoveButton());
        box.add(Box.createHorizontalGlue());
        box.add(stretchPane.getScoresButton());
        header.add(box, BorderLayout.CENTER);

        headerHeight = header.getPreferredSize().height;

        return header;
    }

    private JComponent createRemoveButton() {
        Project project = IdeaDiscovery.getActiveProject();
        JCheckBox checkBox = new JCheckBox("Show PIT icons");
        checkBox.setToolTipText("Show/hide PIT icons in editor windows");
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
        return getConstrainedPanel(checkBox);
    }

    private JComponent createPitVerboseButton() {
        JCheckBox checkBox = new JCheckBox("Verbose");
        checkBox.setToolTipText("Sets 'verbose' tag on next PIT run");
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.setSelected(isPitVerbose);
        checkBox.addActionListener(e -> {
            isPitVerbose = checkBox.isSelected();
        });

        return getConstrainedPanel(checkBox);
    }

    private static @NotNull JPanel getConstrainedPanel(JCheckBox checkBox) {
        // Overwrite default behavior on checkbox that defaults to an extra-wide min width
        Dimension d = new Dimension(10, checkBox.getMinimumSize().height);
        checkBox.setMinimumSize(d);
        checkBox.setPreferredSize(d);
        checkBox.setMaximumSize(d);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(checkBox, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPackagePanel() {
        packageSelector = new EnumRadio<>(Viewing.PackageChoice.values(), "Filter",
                type -> callOptionsChangeFn());
        packageSelector.setSelected(Viewing.PackageChoice.PACKAGE); // Default value
        return packageSelector.getPanel();
    }

    private JPanel createSortPanel() {
        sortSelector = new EnumRadio<>(Sorting.By.values(), "Sort",
                type -> callOptionsChangeFn());
        sortSelector.setSelected(Sorting.By.PROJECT); // Default value
        return sortSelector.getPanel();
    }

    private JPanel createDirPanel() {
        dirSelector = new EnumRadio<>(Sorting.Direction.values(), "Sort Direction",
                type -> callOptionsChangeFn());
        dirSelector.setSelected(Sorting.Direction.ASC); // Default value

        return dirSelector.getPanel();
    }

    public DisplayChoices getDisplayChoices() {
        return new DisplayChoices(getPackageSelection(), sortSelector.getSelected(), dirSelector.getSelected());
    }

    public void setOptionsChangeFn(@Nullable Consumer<DisplayChoices> optionsChangeFn) {
        this.optionsChangeFn = optionsChangeFn;
    }

    private void callOptionsChangeFn() {
        if (optionsChangeFn != null) {
            optionsChangeFn.accept(getDisplayChoices());
        }
    }

    public Viewing.PackageChoice getPackageSelection() {
        return packageSelector.getSelected();
    }

    public void setSortSelection(Sorting.By sortChoice) {
        sortSelector.setSelected(sortChoice);
    }

    public void setDirSelection(Sorting.Direction dirChoice) {
        dirSelector.setSelected(dirChoice);
    }

    public JComponent getContentPanel() {
        return stretchPane.getComponent();
    }

    public void clearScores(Project project) {
        clearScores(project, PitRepo.getCurrent(project));
    }

    public void clearScores(CachedRun cachedRun) {
        clearScores(cachedRun.getProject(), cachedRun);
    }

    private void clearScores(Project project, CachedRun cachedRun) {
        tree.clearExistingRows();
        if (project != null) {
            CoverageGutterRenderer.removeGutterIcons(project);
            syncScoresMsg(cachedRun);
            tree.refresh(cachedRun == null || cachedRun.getRecorder().hasMultiplePackages());
        }
    }

    public void refresh(boolean hasMultiplePackages) {
        tree.refresh(!hasMultiplePackages);
    }

    /**
     * Sets the body of the right panel. This is provided because the console component is
     * generated externally on PIT execution.
     *
     * @param component to set as the right component (inside an already existing scroll pane)
     */
    public void setRightPaneContent(JComponent component) {
        rightScrollPane.setViewportView(component);
    }

    public void setFullConsole() {
        stretchPane.setFullConsole();
    }

    public void setFullScores() {
        stretchPane.setFullScores();
    }

    public void clearHistory() {
        historyList.clear();
    }

    public void reloadReports(Project project) {
        CachedRun current = reloadHistory(project);
        if (current != null) {
            clearScores(current);
            reloadScores(current);
        }
    }

    public CachedRun reloadHistory(Project project) {
                clearHistory();
        class CrossHistory {
            boolean anyDeletable = false;
            CachedRun current = null;
            int maxStartWidth = 0;
            int maxDurationWidth = 0;
        }
        final CrossHistory crossHistory = new CrossHistory();

        PitRepo.apply(project, (c, current) -> {
            ExecutionRecord record = c.getExecutionRecord();
            if (record.isRunnable()) {
                crossHistory.anyDeletable = true;
            }
            if (current) {
                crossHistory.current = c;
            }
            crossHistory.maxStartWidth = Math.max(crossHistory.maxStartWidth, record.getFormattedStart().length());
            crossHistory.maxDurationWidth = Math.max(crossHistory.maxDurationWidth, record.getFormattedDuration().length());
        });
        PitRepo.apply(project, (c, current) -> addHistory(c, new HistoryList.Sizing(crossHistory.maxStartWidth, crossHistory.maxDurationWidth)));
        clearAllButton.setEnabled(crossHistory.anyDeletable);
        if (!crossHistory.anyDeletable) {
            reloadScoresMsg(project, "No current history. Initiate PIT execution from a drop-down menu.");
        } else if (crossHistory.current == null) {
            reloadScoresMsg(project, "Choose a report from the history list to the left.");
        }
        historyList.getComponent().updateUI();
        return crossHistory.current;
    }

    private void reloadScoresMsg(Project project, String msg) {
        CoverageGutterRenderer.removeGutterIcons(project);
        clearScores(project);
        resetToRootMessage(msg);
    }

    public void reloadScores(CachedRun cachedRun) {
        if (cachedRun.isCurrent()) {
            Project project = cachedRun.getProject();
            CoverageGutterRenderer.removeGutterIcons(project);
            clearScores(cachedRun);
            PitToolWindowFactory.addAll(cachedRun, this, cachedRun.getRecorder());
            // previous addAll will have update default scores message, make sure it's set properly
            // here for all the non-completion cases
            syncScoresMsg(cachedRun);
            if (isGutterIconsEnabled && cachedRun.getRunState() == RunState.COMPLETED) {
                FileOpenCloseListener.replayOpenFiles(project);
            }
        }
    }

    private void syncScoresMsg(@Nullable CachedRun cachedRun) {
        if (cachedRun != null && cachedRun.isCurrent()) {
            String msg = "...";
            RunState runState = cachedRun.getRunState();
            switch (runState) {
                case COMPLETED:
                    return;
                case RUNNING:
                    msg = "PIT execution is still running. Please wait.";
                    break;
                case FAILED:
                    msg = "PIT execution failed. See console for details.";
                    break;
                case CANCELLED:
                    msg = "Run was cancelled. Rerun in order to see report.";
            }
            resetToRootMessage(msg);
        }
    }


    private static final Icon runIcon = IconLoader.getIcon("/icons/run.svg", MutationControlPanel.class);
    private static final Icon killIcon = IconLoader.getIcon("/icons/killProcess.svg", MutationControlPanel.class);
    private static final Icon deleteIcon = IconLoader.getIcon("/icons/delete.svg", MutationControlPanel.class);
    private static final Icon syncIcon = IconLoader.getIcon("/icons/settingSync.svg", MutationControlPanel.class);


    /**
     * Adds a row in the history pane representing the state of the supplied CachedRun.
     * Does not check for duplicates.
     *
     * @param cachedRun to read from
     */
    public void addHistory(CachedRun cachedRun, HistoryList.Sizing sizing) {
        ExecutionRecord record = cachedRun.getExecutionRecord();
        boolean isCurrent = cachedRun.isCurrent();
        RunState runState = cachedRun.getRunState();
        boolean valid = runState.isValid();
        JPanel row = historyList.addRow(record, isCurrent, valid, sizing, cachedRun::activate);
        TransitionButton button = new TransitionButton();
        boolean readyToCancel = runState == RunState.RUNNING;

        if (record.isRunnable()) {
            button.addState("Run", runIcon, "Rerun this report", !readyToCancel, () -> run(cachedRun));
            button.addState("Cancel", killIcon, "Cancel this report execution", readyToCancel, () -> cancel(cachedRun));
        } else {
            button.addState("Sync", syncIcon, "Reloads this report from external file", !readyToCancel, () -> sync(cachedRun));
            button.addState("CancelSync", killIcon, "Cancel this sync reload", readyToCancel, () -> cancelSync(cachedRun));
        }
        row.add(button);

        if (cachedRun.getExecutionRecord().isRunnable()) {
            row.add(createDeleteButton(cachedRun));
        }

        setRunStateListener(cachedRun, button);
    }

    private @NotNull JButton createDeleteButton(CachedRun cachedRun) {
        JButton button = new JButton(deleteIcon);
        button.setToolTipText("Delete this report");
        Project project = cachedRun.getProject();
        button.addActionListener(e -> ApplicationManager.getApplication().invokeLater(() -> {
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
        }));
        button.setOpaque(false); // Don't fill the button background
        button.setContentAreaFilled(false); // Transparent content area
        button.setBorderPainted(false); // Optionally remove border
        button.setPreferredSize(TransitionButton.BUTTON_SIZE);
        button.setMinimumSize(TransitionButton.BUTTON_SIZE);
        button.setMaximumSize(TransitionButton.BUTTON_SIZE);
        return button;
    }

    private void setRunStateListener(CachedRun cachedRun, TransitionButton button) {
        cachedRun.setRunStateChangedListener((oldState, newState) ->
        {
            final boolean newRowState;
            final Boolean newClearAllState;
            if (oldState == RunState.RUNNING) {  // A run has ended
                newRowState = true;
                if (activeRuns.decrementAndGet() == 0) {
                    AtomicBoolean anyGenerated = new AtomicBoolean(false);
                    PitRepo.apply(cachedRun.getProject(), (c, current) -> {
                        if (c.getExecutionRecord().isRunnable()) {
                            anyGenerated.set(true);
                        }
                    });
                    newClearAllState = anyGenerated.get();
                } else {
                    newClearAllState = null;
                }
            } else if (newState == RunState.RUNNING) {  // A run has started
                newRowState = false;
                newClearAllState = activeRuns.incrementAndGet() == 1;
            } else {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                button.setEnabled(newRowState);
                if (newClearAllState != null) {
                    clearAllButton.setEnabled(newClearAllState);
                }
            });
        });

    }

    private boolean run(CachedRun cachedRun) {
        try {
            Module module = cachedRun.ensureLoaded().getModule();
            ExecutionRecord record = cachedRun.getExecutionRecord();
            ApplicationManager.getApplication().executeOnPooledThread(() -> ExecutionUtils.execute(module, record.getInputBundle()));
            return true;
        } catch (Exception e) {
            LOGGER.error("Execution failed: " + e.getMessage(), e);
            return false;
        }
    }

    public void handleCompletion(@NotNull CachedRun cachedRun) {
        reloadHistory(cachedRun.getProject());
    }

    private boolean cancel(CachedRun cachedRun) {
        if (cachedRun.cancel()) {
            reloadScores(cachedRun);
            return true;
        }
        return false;
    }

    private boolean sync(CachedRun cachedRun) {
        boolean result = true;
        try {
            cachedRun.reload();
        } catch (Exception e) {
            cachedRun.setRunState(RunState.FAILED);
            markScoresInvalid();
            result = false;
        }
        reloadReports(cachedRun.getProject());
        return result;
    }

    private boolean cancelSync(@NotNull CachedRun cachedRun) {
        cachedRun.setRunState(RunState.CANCELLED);
        // Assume no other significant work involved in canceling the reload
        reloadReports(cachedRun.getProject());
        return true;
    }

    public void markScoresInvalid() {
        resetToRootMessage("Select a run from the history list to the left.");
    }

    public void resetToRootMessage(String text) {
        tree.addRootRow().setSingleSegment(text);
    }

    public boolean isGutterIconsEnabled() {
        return isGutterIconsEnabled;
    }

    public Level getLevel() {
        tree.clearExistingRows();
        return new Level(tree.addRootRow(), true);
    }

    public boolean isPitVerboseEnabled() {
        return isPitVerbose;
    }

    public static class Level {
        private final ClickTree.TreeRow treeRow;
        private boolean isTop;

        Level(ClickTree.TreeRow treeRow, boolean isTop) {
            this.treeRow = treeRow;
            this.isTop = isTop;
        }

        public void setLine(CachedRun cachedRun, VirtualFile file, String fileName, IMutationScore score) {
            ClickTree.TreeRow targetRow = isTop ? this.treeRow : this.treeRow.addChildRow();
            isTop = false;
            targetRow
                    .addSegment(formatScore(score), ClickTree.Hover.UNDERLINE, (component, point, _button) -> showScoreDetailPopup(component, point, score.getScoreDescription()))
                    .addSegment(fileName, ClickTree.Hover.NONE, (_c, _p, button) -> {
                        if (button) {
                            IdeaDiscovery.openBrowserTo(IdeaDiscovery.getUrl(cachedRun, file));
                        } else {
                            FileEditorManager fileEditorManager = FileEditorManager.getInstance(cachedRun.getProject());
                            fileEditorManager.openFile(file, true); // true to focus the file
                        }
                    })
                    .addDelegatedSegment("(Click open; Ctrl-click browser)", ClickTree.Hover.FLASH);
        }

        public Level setLine(CachedRun cachedRun, String pkgName, String qualifiedPkgName, IMutationScore score) {
            Level level = isTop ? this : new Level(treeRow.addChildRow(), false);
            boolean isRoot = PitExecutionRecorder.ROOT_PACKAGE_NAME.equals(pkgName);
            ClickTree.Hover hoverRight = isRoot ? ClickTree.Hover.ITALICS : ClickTree.Hover.NONE;
            isTop = false;
            level.treeRow
                    .addSegment(formatScore(score), ClickTree.Hover.UNDERLINE, (component, point, _button) -> showScoreDetailPopup(component, point, score.getScoreDescription()))
                    .addSegment(pkgName, hoverRight, (_c, _p, button) -> {
                        if (button) {
                            final String url;
                            if (isRoot) {
                                url = IdeaDiscovery.getUrl(cachedRun);
                            } else {
                                url = IdeaDiscovery.getPackageUrl(cachedRun, qualifiedPkgName);
                            }
                            IdeaDiscovery.openBrowserTo(url);
                        }
                    })
                    .addDelegatedSegment("(Ctrl-click browser)", ClickTree.Hover.FLASH);
            return level;
        }

        private String formatScore(IMutationScore score) {
            return String.format("%.0f%%", score.getScore());
        }

        public void showScoreDetailPopup(Component component, Point point, String message) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                    .createBalloon()
                    .show(new RelativePoint(component, point), Balloon.Position.above);
        }
    }
}


