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
    private Consumer<MutationControlPanel> packageSelectionChangeFn = null;
    private EnumRadio<PackageType> radioSelector;

    public enum PackageType {
        PACKAGE("All"),
        CODE("Coded"),
        NONE("None");
        final String displayName;

        PackageType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public MutationControlPanel() {
        panel = new JPanel(new BorderLayout());

        panel.add(new JScrollPane(tree), BorderLayout.CENTER);

        panel.add(createHeaderPanel(), BorderLayout.NORTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.add(createRemoveButton(), BorderLayout.EAST);
        header.add(createPackagePanel(), BorderLayout.WEST);
        return header;
    }

    private static JButton createRemoveButton() {
        JButton button = new JButton("Remove PITest icons");
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons());
        return button;
    }

    private JPanel createPackagePanel() {
        JPanel packagePanel = new JPanel(new BorderLayout());
        packagePanel.setBorder(BorderFactory.createTitledBorder("Display"));
        radioSelector = new EnumRadio<>(PackageType.values(),
                PackageType::getDisplayName,
                type -> {
                    packageSelectionChangeFn.accept(this);
                });
        packagePanel.add(radioSelector.getPanel(), BorderLayout.NORTH);
        return packagePanel;
    }

    public void setPackageSelectionChangeFn(Consumer<MutationControlPanel> packageSelectionChangeFn) {
        this.packageSelectionChangeFn = packageSelectionChangeFn;
    }

    public PackageType getPackageSelection() {
        return radioSelector.getSelected();
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


