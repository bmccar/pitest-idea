package org.pitestidea.toolwindow;

import com.google.common.base.Strings;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import org.pitestidea.render.CoverageGutterRenderer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MutationControlPanel {

    private final JPanel panel;
    private final DefaultMutableTreeNode rootNode;

    public MutationControlPanel() {
        panel = new JPanel(new BorderLayout());
        JTree tree = createTreeWithPanels();

        //JLabel header = new JLabel("Package Contents");
        //header.setHorizontalAlignment(SwingConstants.CENTER);
        //panel.add(header, BorderLayout.NORTH);

        panel.add(new JScrollPane(tree), BorderLayout.CENTER);

        JButton button = new JButton("Remove PITest icons");
        button.addActionListener(e -> CoverageGutterRenderer.removeGutterIcons());
        panel.add(button, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("blah");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.setModel(model);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setLine(String text, float score) {
        rootNode.add(new DefaultMutableTreeNode(createLine(text, score)));
    }

    /**
     * Creates a row in the tree model, left-aligned with the first columns being fixed-width
     * so that the start of the right-most column is aligned.
     *
     * @param text for right-most column
     * @param score for this entry
     * @return panel for adding
     */
    private static JPanel createLine(String text, float score) {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT));
        line.setBorder(BorderFactory.createLineBorder(JBColor.BLUE));

        String scoreText = String.valueOf((int)score)+'%';
        int spaces = 1 + 4 - scoreText.length();
        scoreText = scoreText + Strings.repeat(" ", spaces);
        JLabel scoreCell = new JLabel(scoreText);
        scoreCell.setBorder( new LineBorder(JBColor.GREEN, 1, true));
        line.add(scoreCell);

        JLabel main = new JLabel(text);
        main.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                System.out.println("Clicked");
            }
        });
        line.add(main);

        return line;
    }

    private JTree createTreeWithPanels() {
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

        JTree tree = new Tree(treeModel);

        tree.setCellRenderer(new JPanelTreeCellRenderer());

        /*
        // Expand all nodes by default
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
         */

        return tree;
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


