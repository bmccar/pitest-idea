package org.pitestidea.toolwindow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A JTree that allows for independently-selectable components in a row.
 */
public class DTree {
    private final TreeRow rootTreeRow = new TreeRow(0);

    //private final JTree tree = new Tree(root);
    private final JTree tree = new JTree(rootTreeRow.node); // TODO fix once in plugin
    private final CustomTreeCellRenderer renderer;

    private RowSegment hoverSegment = null;
    private int hoveredRow = -1;    // Index of row currently under the mouse
    private int rowXPos = -1;      // X position of the mouse

    private void setHoveredRow(int row, int x) {
        hoveredRow = row;
        rowXPos = x;
    }

    public DTree() {
        renderer = new CustomTreeCellRenderer();
        tree.setCellRenderer(renderer);

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
                    System.out.println("Clicked " + hoverSegment.text + " at row " + hoveredRow);
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

    public void clear() {
        rootTreeRow.node.removeAllChildren();
        renderer.labels.clear();
    }

    public enum Hover {NONE, UNDERLINE}

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
        private Runnable action;

        private RowSegment(@NotNull String text, @NotNull Hover hover, @Nullable Runnable action) {
            this.text = text;
            this.hover = hover;
            this.action = action;
            this.dim = new Dimension(8 * (text.length() + 1), 16);
            if (hover == Hover.UNDERLINE) {
                this.altText = "<html><u>" + text + "</u></html>";
            }
        }

        private int apply(JLabel label, boolean hoveredRow, int labelXPos, DTree dTree) {
            final boolean hit = hoveredRow && labelXPos >= 0 && labelXPos <= dim.width;

            label.setText((hit && altText != null) ? altText : text);

            if (hit) {
                dTree.hoverSegment = this;
            }
            label.setMinimumSize(dim);
            label.setPreferredSize(dim);
            label.setSize(dim);
            return dim.width;
        }
    }

    public class TreeRow {
        private final DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
        private final List<RowSegment> rowSegments = new ArrayList<>();
        private final int level;

        private TreeRow(int level) {
            this.level = level;
        }

        public TreeRow addChildRow() {
            return addRowTo(this);
        }

        public TreeRow addSegment(@NotNull String text, @NotNull Hover hover, @Nullable Runnable action) {
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

    public void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
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
                    // TODO verify it works with font resize
                    int boundaryXPos = treeRow.level * 16;
                    for (int i = 0; i < labels.size(); i++) {
                        JLabel label = labels.get(i);
                        if (i >= treeRow.rowSegments.size()) {
                            label.setVisible(false);
                        } else {
                            label.setVisible(true);
                            RowSegment segment = treeRow.rowSegments.get(i);
                            boundaryXPos += segment.apply(label, hoveredRow == row && rowXPos >= 0, rowXPos - boundaryXPos, DTree.this);
                        }
                    }
                }
            }
            return this;
        }
    }
}