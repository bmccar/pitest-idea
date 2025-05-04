package org.pitestidea.toolwindow;

import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * A JTree that allows for independently-selectable components in a row.
 */
public class ClickTree {
    private final TreeRow rootTreeRow = new TreeRow(0);

    private final JTree tree = new Tree(rootTreeRow.node);
    private final CustomTreeCellRenderer renderer;

    // How much horizontal space in each row before first segment
    private final int rowPrefixWidth;

    private RowSegment hoverSegment = null;
    private int hoveredRow = -1;    // Index of row currently under the mouse
    private int rowXPos = -1;      // X position of the mouse

    private void setHoveredRow(int row, int x) {
        hoveredRow = row;
        rowXPos = x;
    }

    public ClickTree() {
        renderer = new CustomTreeCellRenderer();
        tree.setCellRenderer(renderer);

        rootTreeRow.addSegment("");
        Insets insets = tree.getInsets();
        rowPrefixWidth = tree.getMaximumSize().width - (insets.left + insets.right);

        clearExistingRows();

        tree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tree.getClosestRowForLocation(e.getX(), e.getY());
                setHoveredRow(row, e.getX());
                hoverSegment = null;
                tree.repaint();
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hoverSegment != null && hoverSegment.action != null) {
                    hoverSegment.action.accept(e.getComponent(), new Point(e.getX(), e.getY()));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredRow(-1, -1); // Reset hover state when mouse exits
                hoverSegment = null;
                tree.repaint();
            }
        });
    }

    public JComponent getComponent() {
        return tree;
    }

    public void clearExistingRows() {
        rootTreeRow.node.removeAllChildren();
        rootTreeRow.rowSegments.clear();
        renderer.labels.clear();
        renderer.removeAll();
    }

    void refresh(boolean expandAll) {
        SwingUtilities.invokeLater(() -> {
            int childCount = rootTreeRow.node.getChildCount();

            // Auto-expand the first level of the tree
            for (int i = 0; i < childCount; i++) {
                tree.expandRow(i);
            }
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.reload();  // Loses which rows are expanded
            if (expandAll) {
                expandAll();
            }
        });
    }

    public enum Hover {
        NONE,  // Normal text, no change on hover
        UNDERLINE,   // Normal text that is underlined on hover
        ITALICS  // Italic text whether hovering or not
    }

    private static class RowSegment {
        @NotNull
        private String text;
        @Nullable
        private String altText;
        @NotNull
        private Hover hover;
        @NotNull
        private final Dimension dim;
        @Nullable
        private BiConsumer<Component,Point> action;

        private RowSegment(@NotNull String text, @NotNull Hover hover, @Nullable BiConsumer<Component,Point> action) {
            this.text = hover==Hover.ITALICS ? ("<html><i>" + text + "</i></html>") : text;
            this.hover = hover;
            this.action = action;
            this.dim = new Dimension(8 * (text.length() + 1), 16);
            if (hover == Hover.UNDERLINE) {
                this.altText = "<html><u>" + text + "</u></html>";
            } else {
                this.altText = null;
            }
        }

        private int apply(JLabel label, boolean hoveredRow, int labelXPos, ClickTree tree) {
            final boolean hit = hoveredRow && labelXPos >= 0 && labelXPos <= dim.width;

            label.setText((hit && altText != null) ? altText : text);

            if (hit) {
                tree.hoverSegment = this;
            }
            label.setMinimumSize(dim);
            label.setPreferredSize(dim);
            label.setSize(dim);
            return dim.width;
        }
    }

    public class TreeRow {
        private final DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
        private final java.util.List<RowSegment> rowSegments = new ArrayList<>();
        private final int level;

        private TreeRow(int level) {
            this.level = level;
        }

        public TreeRow addChildRow() {
            return addRowTo(this);
        }

        public TreeRow setSingleSegment(@NotNull String text) {
            rowSegments.clear();
            return addSegment(text);
        }

        public TreeRow addSegment(@NotNull String text) {
            return addSegment(text, Hover.NONE, null);
        }

        public TreeRow addSegment(@NotNull String text, @NotNull Hover hover, @Nullable BiConsumer<Component, Point> action) {
            rowSegments.add(new RowSegment(text, hover, action));
            if (renderer.labels.size() < rowSegments.size()) {
                JLabel label = new JLabel();
                label.setOpaque(false);
                label.setVisible(false);
                renderer.add(label);
                renderer.labels.add(label);
            }
            return this;
        }
    }

    public TreeRow addRootRow() {
        return rootTreeRow;
    }

    private TreeRow addRowTo(TreeRow parentRow) {
        TreeRow childRow = new TreeRow(parentRow.level + 1);
        parentRow.node.add(childRow.node);
        return childRow;
    }

    private void expandAll() {
        // Start with the root row (row 0)
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }


    class CustomTreeCellRenderer extends JPanel implements TreeCellRenderer {
        private final List<JLabel> labels = new ArrayList<>();

        private CustomTreeCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode node) {
                TreeRow treeRow = (TreeRow) node.getUserObject();
                if (treeRow != null) {
                    // Unfortunately there appears to be no way to get the start of the row (after drop-down markerts)
                    // without calling a method that does not recursively call this method. So, hardwire it.
                    int boundaryXPos = treeRow.level * rowPrefixWidth;
                    //System.out.println("Row " + row + " has " + treeRow.rowSegments.size() + " segments and " + labels.size() + " labels");
                    for (int i = 0; i < labels.size(); i++) {
                        JLabel label = labels.get(i);
                        if (i >= treeRow.rowSegments.size()) {
                            label.setVisible(false);
                        } else {
                            label.setVisible(true);
                            RowSegment segment = treeRow.rowSegments.get(i);
                            boundaryXPos += segment.apply(label, hoveredRow == row && rowXPos >= 0, rowXPos - boundaryXPos, ClickTree.this);
                        }
                    }
                }
            }
            return this;
        }
    }
}