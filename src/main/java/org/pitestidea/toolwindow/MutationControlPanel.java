package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.CachedRun;
import org.pitestidea.model.ExecutionRecord;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Results tool window.
 */
public class MutationControlPanel {

    private final StretchPane stretchPane = new StretchPane();
    private final JScrollPane rightScrollPane = new JBScrollPane();
    private final ClickTree tree = new ClickTree();
    private Consumer<Boolean> optionsChangeFn = null;
    private EnumRadio<Viewing.PackageChoice> packageSelector;
    private EnumRadio<Sorting.By> sortSelector;
    private EnumRadio<Sorting.Direction> dirSelector;
    private final HistoryPane historyPane = new HistoryPane();

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

        JComponent historyPanel = historyPane.getComponent();
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

        JPanel actionButtonPanel = new JPanel(new FlowLayout());
        actionButtonPanel.add(createRemoveButton());
        actionButtonPanel.add(stretchPane.getScoresButton(), BorderLayout.EAST);

        header.add(createOptionsPanel(), BorderLayout.WEST);
        header.add(actionButtonPanel, BorderLayout.EAST);
        return header;
    }

    private static JButton createRemoveButton() {
        JButton button = new JButton("Remove PITest icons");
        Project project = IdeaDiscovery.getActiveProject();
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons(project));
        return button;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(createPackagePanel());
        panel.add(createSortPanel());
        panel.add(createDirPanel());
        return panel;
    }

    private JPanel createPackagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Packages"));
        packageSelector = new EnumRadio<>(Viewing.PackageChoice.values(),
                Viewing.PackageChoice::getDisplayName,
                type -> optionsChangeFn.accept(false));
        packageSelector.setSelected(Viewing.PackageChoice.PACKAGE); // Default value
        panel.add(packageSelector.getPanel(), BorderLayout.NORTH);
        return panel;
    }

    private JPanel createSortPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Sort"));
        sortSelector = new EnumRadio<>(Sorting.By.values(),
                Sorting.By::getDisplayName,
                type -> optionsChangeFn.accept(true));
        sortSelector.setSelected(Sorting.By.PROJECT); // Default value
        panel.add(sortSelector.getPanel(), BorderLayout.NORTH);
        return panel;
    }

    private JPanel createDirPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Sort Direction"));
        dirSelector = new EnumRadio<>(Sorting.Direction.values(),
                Sorting.Direction::getDisplayName,
                type -> optionsChangeFn.accept(true));
        dirSelector.setSelected(Sorting.Direction.ASC); // Default value
        panel.add(dirSelector.getPanel(), BorderLayout.NORTH);
        return panel;
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
        historyPane.clear();
    }

    public void addHistory(CachedRun cachedRun) {
        ExecutionRecord record = cachedRun.getExecutionRecord();
        System.out.println("Adding history for " + record.getReportDirectoryName());
        JPanel row = historyPane.addRow();
        JLabel label = new JLabel(record.getReportName());
        row.add(label);
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


