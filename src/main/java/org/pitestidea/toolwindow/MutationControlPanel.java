package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Results tool window.
 */
public class MutationControlPanel {

    private final JSplitPane splitPane;
    private final JPanel rightPaneEmpty = new JPanel();
    private final JScrollPane rightScrollPane;
    private final ClickTree tree = new ClickTree();
    private Consumer<Boolean> optionsChangeFn = null;
    private EnumRadio<Viewing.PackageChoice> packageSelector;
    private EnumRadio<Sorting.By> sortSelector;
    private EnumRadio<Sorting.Direction> dirSelector;

    enum PaneState {
        SCORES(1),  // Scores is maximized
        MIXED(0.5), // Split between scores and console
        CONSOLE(0); // Console is maximized

        private final double dividerLocation;

        PaneState(double dividerLocation) {
            this.dividerLocation = dividerLocation;
        }

        PaneState goScores() {
            return this==SCORES ? MIXED : /* Already MIXED */ SCORES;
        }

        PaneState goConsole() {
            return this==CONSOLE ? MIXED : /* Already MIXED */ CONSOLE;
        }
    }

    private final JLabel scoresButton;
    private final JLabel consoleButton;
    private static PaneState currentState = PaneState.SCORES;

    private static final String LEFTWARD = "<";
    private static final String RIGHTWARD = ">";

    private void expandConsole() {
        currentState = currentState.goConsole();
        consoleButton.setText(currentState==PaneState.MIXED?LEFTWARD:RIGHTWARD);
        splitPane.setDividerLocation(currentState.dividerLocation);
    }

    private void expandScores() {
        currentState = currentState.goScores();
        splitPane.getRightComponent().setVisible(true);
        scoresButton.setText(currentState==PaneState.MIXED?RIGHTWARD:LEFTWARD);
        splitPane.setDividerLocation(currentState.dividerLocation);
    }

    private void setDefaultState() {
        currentState = PaneState.SCORES;
        scoresButton.setText(LEFTWARD);
        splitPane.setDividerLocation(currentState.dividerLocation);
        splitPane.setResizeWeight(currentState.dividerLocation);
        // If not set then component is partially visible at the start
        splitPane.getRightComponent().setVisible(false);
    }

    public MutationControlPanel() {

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        scoresButton = DisplayUtils.createHoverLabel(LEFTWARD, this::expandScores);
        consoleButton = DisplayUtils.createHoverLabel(LEFTWARD, this::expandConsole);

        JPanel scoresPanel = new JPanel(new BorderLayout());
        scoresPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        scoresPanel.add(createScoresHeaderPanel(), BorderLayout.NORTH);

        rightScrollPane = new JBScrollPane();

        splitPane.setLeftComponent(scoresPanel);
        splitPane.setRightComponent(createConsolePane(rightScrollPane));

        setDefaultState();
    }

    public void setRightPaneContent(JComponent component) {
        rightScrollPane.setViewportView(component);
    }

    private JPanel createConsolePane(JScrollPane scrollPane) {
        JPanel fullPanel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.add(consoleButton, BorderLayout.WEST);
        fullPanel.add(header, BorderLayout.NORTH);

        fullPanel.add(new JScrollPane(scrollPane), BorderLayout.CENTER);

        return fullPanel;
    }

    private JPanel createScoresHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());

        JPanel actionButtonPanel = new JPanel(new FlowLayout());
        actionButtonPanel.add(createRemoveButton());
        actionButtonPanel.add(scoresButton, BorderLayout.EAST);

        header.add(createOptionsPanel(), BorderLayout.WEST);
        header.add(actionButtonPanel, BorderLayout.EAST);
        return header;
    }

    private static JButton createRemoveButton() {
        JButton button = new JButton("Remove PITest icons");
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons());
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
        panel.setBorder(BorderFactory.createTitledBorder("Display"));
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
        return splitPane;
    }

    public void clear() {
        tree.clearExistingRows();
    }

    public void refresh() {
        tree.refresh();
    }

    public Level getLevel() {
        return new Level(tree.getRootTreeLevel());
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


