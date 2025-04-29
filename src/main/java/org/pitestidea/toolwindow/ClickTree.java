package org.pitestidea.toolwindow;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

/**
 * A tree with text lines that are clickable.
 */
public class ClickTree extends JPanel implements TreeSelectionListener {
    private final JTree tree;
    private final DefaultMutableTreeNode root;
    private final TreeLevel rootTreeLevel;

    public ClickTree() {
        super(new GridLayout(1, 0));

        // Default message will be overridden on 1st child add
        root = new DefaultMutableTreeNode("No PIT runs yet. Please select one from a drop-down menu or the history list on the left if any are there.");
        rootTreeLevel = new TreeLevel(root) {
            @Override
            TreeLevel addPackageRow(String pkg) {
                if (pkg == null || pkg.isEmpty()) {
                    pkg = "No package selected"; // TODO works?
                }
                root.setUserObject(pkg);
                return new TreeLevel(root);
            }
        };

        tree = new Tree(root);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.setCellRenderer(new PreserveHtmlTreeCellRenderer());

        tree.addTreeSelectionListener(this);

        Dimension minimumSize = new Dimension(100, 50);

        JScrollPane treeView = new JBScrollPane(tree);
        treeView.setMinimumSize(minimumSize);
        treeView.setPreferredSize(new Dimension(500, 300));

        add(treeView);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (e.getNewLeadSelectionPath() == null) {
            // Handle the case where no selection exists
            return;
        }
        Object selectedNode = e.getPath().getLastPathComponent();
        if (selectedNode instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
            Object userObject = defaultMutableTreeNode.getUserObject();
            if (userObject instanceof ClickableNode clickableNode) {
                clickableNode.onClick();
            }
        }
    }

    public void resetToRootMessage(String s) {
        root.setUserObject(s);
        refresh();
    }

    abstract static class ClickableNode {
        abstract void onClick();
    }

    static class ClickableFileNode extends ClickableNode {
        private final Project project;
        private final VirtualFile file;
        private final String fileName;

        ClickableFileNode(Project project, VirtualFile file, String fileName) {
            this.project = project;
            this.file = file;
            this.fileName = fileName;
        }

        @Override
        public void onClick() {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.openFile(file, true); // true to focus the file
        }

        @Override
        public String toString() {
            // Set a fixed-width font
            return "<html><body style='font-family: Andale Mono;'" + fileName + "</body></html>";
        }
    }

    void refresh() {
        SwingUtilities.invokeLater(() -> {
            int childCount = root.getChildCount();

            // Auto-expand the first level of the tree
            for (int i = 0; i < childCount; i++) {
                tree.expandRow(i);
            }
            tree.updateUI();
        });
    }

    static class TreeLevel {
        private final DefaultMutableTreeNode node;

        TreeLevel(DefaultMutableTreeNode node) {
            this.node = node;
        }

        void addClickableFileRow(Project project, VirtualFile file, String fileName) {
            node.add(new DefaultMutableTreeNode(new ClickableFileNode(project, file, fileName)));
        }

        TreeLevel addPackageRow(String pkg) {
            System.out.println("addPackageRow: " + pkg);
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(pkg);
            this.node.add(child);
            return new TreeLevel(child);
        }
    }

    void clearExistingRows() {
        root.removeAllChildren();
    }

    TreeLevel getRootTreeLevel() {
        return rootTreeLevel;
    }

    /**
     * Needed because setting the font on tree disables html rendering.
     */
    private static class PreserveHtmlTreeCellRenderer extends JEditorPane implements TreeCellRenderer {
        public PreserveHtmlTreeCellRenderer() {
            setContentType("text/html");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setText(value.toString());
            return this;
        }
    }
}
