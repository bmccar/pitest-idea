package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Results tool window.
 */
public class MutationControlPanel {

    private final JPanel panel;
    private final ClickTree tree = new ClickTree();
    private Consumer<Boolean> optionsChangeFn = null;
    private EnumRadio<Viewing.PackageChoice> packageSelector;
    private EnumRadio<Sorting.By> sortSelector;
    private EnumRadio<Sorting.Direction> dirSelector;

    public MutationControlPanel() {
        panel = new JPanel(new BorderLayout());

        panel.add(new JScrollPane(tree), BorderLayout.CENTER);

        panel.add(createHeaderPanel(), BorderLayout.NORTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.add(createRemoveButton(), BorderLayout.EAST);
        header.add(createOptionsPanel(), BorderLayout.WEST);
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

    public JPanel getPanel() {
        return panel;
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


