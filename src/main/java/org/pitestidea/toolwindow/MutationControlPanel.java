package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * Results tool window.
 */
public class MutationControlPanel {

    private final JPanel panel;
    private final ClickTree tree = new ClickTree();

    public MutationControlPanel() {
        panel = new JPanel(new BorderLayout());

        panel.add(new JScrollPane(tree), BorderLayout.CENTER);

        JButton button = new JButton("Remove PITest icons");
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons());
        panel.add(button, BorderLayout.NORTH);
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

        public void setLine(Project project, VirtualFile file, String fileName, float score) {
            treeLevel.addClickableFileRow(project, file, createLine(fileName, score));
        }

        public Level setLine(Project project, String pkgName, float score) {
            return new Level(treeLevel.addPackageRow(project, createLine(pkgName, score)));

        }
    }

    private static String createLine(String text, float score) {
        String space = score==100 ? "" : score > 10 ? "&nbsp;" : "&nbsp;&nbsp;";
        return String.format("<html>%d%%%s&nbsp;%s</html>",(int)score,space,text);
    }
}


