package org.pitestidea.toolwindow;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class ClickTree extends JPanel implements TreeSelectionListener {
    private final JTree tree;
    private final DefaultMutableTreeNode root;

    public ClickTree() {
        super(new GridLayout(1, 0));

        root = new DefaultMutableTreeNode("Results");

        tree = new JTree(root);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(this);

        Dimension minimumSize = new Dimension(100, 50);
        tree.setMinimumSize(minimumSize);
        tree.setPreferredSize(new Dimension(500, 300));

        JScrollPane treeView = new JScrollPane(tree);
        add(treeView);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof ClickableNode clickableNode) {
            //System.out.println("Clicked on: " + clickableNode.getClass().getSimpleName() + ", leaf=" + node.isLeaf());
            clickableNode.onClick();
        }
    }

    abstract static class ClickableNode {
        abstract void onClick();
    }

    static class ClickableFileNode extends ClickableNode {
        private final String pathName;
        private final String fileName;

        ClickableFileNode(String pathName, String fileName) {
            this.pathName = pathName;
            this.fileName = fileName;
        }

        @Override
        public void onClick() {
            System.out.println("Open " + pathName);
        }

        public String toString() {
            return fileName;
        }
    }

    void addClickableFileRow(String pathName, String fileName) {
        root.add(new DefaultMutableTreeNode(new ClickableFileNode(pathName, fileName)));
    }
}
