package org.pitestidea.toolwindow;

import com.google.common.base.Strings;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MutationControlPanel {

    private final JPanel panel;
    private final DefaultMutableTreeNode rootNode;
    private ClickTree tree = new ClickTree();

    public MutationControlPanel() {
        panel = new JPanel(new BorderLayout());
        //JTree tree = createTreeWithPanels();
        //FlexTree tree = new FlexTree();

        //JLabel header = new JLabel("Package Contents");
        //header.setHorizontalAlignment(SwingConstants.CENTER);
        //panel.add(header, BorderLayout.NORTH);

        panel.add(new JScrollPane(tree), BorderLayout.CENTER);

        JButton button = new JButton("Remove PITest icons");
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons());
        panel.add(button, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("blah");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        //tree.setModel(model);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setLine(String pathName, String fileName, float score) {
        tree.addClickableFileRow(pathName, createLine(fileName, score));
    }

    /**
     * Creates a row in the tree model, left-aligned with the first columns being fixed-width
     * so that the start of the right-most column is aligned.
     *
     * @param text for right-most column
     * @param score for this entry
     * @return panel for adding
     */
    private static JPanel createLine1(String text, float score) {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT));
        line.setBorder(BorderFactory.createLineBorder(JBColor.BLUE));

        String scoreText = String.valueOf((int)score)+'%';
        int spaces = 1 + 4 - scoreText.length();
        scoreText = scoreText + Strings.repeat(" ", spaces);
        JLabel scoreCell = new JLabel(scoreText);
        scoreCell.setBorder( new LineBorder(JBColor.GREEN, 1, true));
        line.add(scoreCell);

        JLabel main = new JLabel(text);
        /*
        main.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Object s = e.getSource();
                Component c = e.getComponent();
                System.out.println("Clicked");
            }
        });
         */
        line.add(main);

        return line;
    }

    private static String createLine(String text, float score) {
        String space = score==100 ? "" : score > 10 ? "&nbsp;" : "&nbsp;&nbsp;";
        return String.format("<html>%d%%%s&nbsp;%s</html>",(int)score,space,text);
    }

    private JTree createTreeWithPanels() {
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

        JTree tree = new Tree(treeModel);

        tree.setCellRenderer(new JPanelTreeCellRenderer());

        //tree.addMouseListener(new TreeLabelMouseListener(tree));

        /*
        // Expand all nodes by default
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
         */

        return tree;
    }

    public static class TreeLabelMouseListener extends MouseAdapter {
        private final JTree tree;

        public TreeLabelMouseListener(JTree tree) {
            this.tree = tree;
            tree.setEditable(true);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Get clicked row
            int row = tree.getClosestRowForLocation(e.getX(), e.getY());

            TreePath x = tree.getPathForRow(row);
            Object y = x.getLastPathComponent();
            // Get the corresponding node
            Object node = tree.getPathForRow(row).getLastPathComponent();
            //LabelNode node = (LabelNode) tree.getPathForRow(row).getLastPathComponent();

            if (node instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode) node;
                TreeNode[] z  = defaultMutableTreeNode.getPath();
                tree.setSelectionPath(x);
                tree.scrollPathToVisible(x);
            }

            // Print the label text
            if (node != null) {
                //System.out.println(node.getLabel());
                System.out.println(node);
            }
        }
    }

    private static class JPanelTreeCellRenderer implements TreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            // Extract the user object (our node's data)
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof JPanel) {
                // Return the custom JPanel directly for rendering
                return (JPanel) userObject;
            } else {
                // Default rendering for non-JPanel nodes
                return new JLabel(userObject.toString());
            }
        }
    }
}


