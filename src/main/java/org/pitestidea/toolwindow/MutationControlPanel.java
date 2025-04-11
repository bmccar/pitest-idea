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
        SCORES(1, "<", null),  // Scores is maximized
        MIXED(0.5, ">", "<"), // Split between scores and console
        CONSOLE(0, null, ">"); // Console is maximized

        private final double dividerLocation;
        private final String inScores;
        private final String inConsole;

        PaneState(double dividerLocation, String inScores, String inConsole) {
            this.dividerLocation = dividerLocation;
            this.inScores = inScores;
            this.inConsole = inConsole;
        }

        String getScoresText() {
            return inScores;
        }

        String getConsoleText() {
            return inConsole;
        }

        PaneState goScores() {
            return this==SCORES ? MIXED : /* Already MIXED */ SCORES;
        }

        PaneState goConsole() {
            return this==CONSOLE ? MIXED : /* Already MIXED */ CONSOLE;
        }

        public boolean isVisibleInState(PaneState state) {
            return this==MIXED || this == state;
        }
    }

    private final JLabel scoresButton;
    private final JLabel consoleButton;
    private static PaneState currentState = PaneState.SCORES;

    // TODO
    private static final String LEFTWARD = "<";
    private static final String RIGHTWARD = ">";

    public void setFullConsole() {
        setState(PaneState.CONSOLE);
    }

    void setState(PaneState state) {
        currentState = state;
        scoresButton.setText(currentState.getScoresText());
        consoleButton.setText(currentState.getConsoleText());
        splitPane.setDividerLocation(currentState.dividerLocation);
        splitPane.setResizeWeight(currentState.dividerLocation);
        splitPane.getRightComponent().setVisible(currentState.isVisibleInState(currentState));
    }

    public MutationControlPanel() {

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        scoresButton = DisplayUtils.createHoverLabel(LEFTWARD, ()->setState(currentState.goScores()));
        consoleButton = DisplayUtils.createHoverLabel(LEFTWARD, ()->setState(currentState.goConsole()));

        JPanel scoresPanel = new JPanel(new BorderLayout());
        scoresPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        scoresPanel.add(createScoresHeaderPanel(), BorderLayout.NORTH);

        rightScrollPane = new JBScrollPane();

        splitPane.setLeftComponent(scoresPanel);
        splitPane.setRightComponent(createConsolePane(rightScrollPane));

        setState(PaneState.SCORES);
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


