package org.pitestidea.toolwindow;

import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A JTree that allows for independently selectable components in a row.
 */
public class ClickTree {
    private final TreeRow rootTreeRow = new TreeRow(0);

    private final JTree tree = new Tree(rootTreeRow.node);
    private final Font rowFont = tree.getFont();
    private final CustomTreeCellRenderer renderer;

    private RowSegment hoverSegment = null;
    private int hoveredRow = -1;    // Index of the row currently under the mouse
    private int rowXPos = -1;       // X position of the mouse
    private int rowXStart = -1;     // X position of the row

    private void setHoveredRow(int row, int x) {
        hoveredRow = row;
        rowXPos = x;
    }

    public ClickTree() {
        renderer = new CustomTreeCellRenderer();
        tree.setCellRenderer(renderer);

        rootTreeRow.addSegment("");

        clearExistingRows();

        tree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tree.getClosestRowForLocation(e.getX(), e.getY());

                if (row != -1) {
                    Rectangle rowBounds = tree.getRowBounds(row);
                    if (rowBounds != null) {
                        rowXStart = rowBounds.x;
                    }
                }

                setHoveredRow(row, e.getX());
                hoverSegment = null;
                tree.repaint();
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mods = e.getModifiersEx();
                boolean button = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;

                if (hoverSegment != null && hoverSegment.action != null) {
                    hoverSegment.action.accept(e.getComponent(), new Point(e.getX(), e.getY()), button);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredRow(-1, -1); // Reset hover state when the mouse exits
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

    @VisibleForTesting
    static int contentLength(@NotNull String s) {
        int count = 0;
        char exp = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c=='&') {
                count++;
                exp = ';';
            } else if (c == exp) {
                exp = 0;
            } else if (c == '<') {
                exp = '>';
            } else if (exp == 0) {
                count++;
            }
        }
        return count;

    }

    public enum Hover {
        NONE,  // Normal text, no change on hover
        UNDERLINE,   // Normal text that is underlined on hover
        ITALICS,  // Italic text whether hovering or not
        FLASH  // Italic, only active on hover
    }

    private static class RowSegment {
        @Nullable
        final private String text;  // either text, altText, or both are not null
        @Nullable
        final private String altText;
        @NotNull
        final private Dimension dim;
        @Nullable
        final private TriConsumer<Component,Point,Boolean> action;
        @NotNull
        private Font font;

        // When a delegate is set, the hover behavior of this segment will activate based on whether the
        // delegate is the current hover segment
        @Nullable
        private RowSegment delegate = null;

        private RowSegment(@NotNull String text, @NotNull Hover hover, @Nullable TriConsumer<Component,Point,Boolean> action, @NotNull Font font, boolean first) {
            this.action = action;
            this.font = font;
            //String coreText = text;
            final String html = first ? "<html>" : "<html>&nbsp;";
            final boolean isHtml;
            if (text.startsWith("<html>")) {
                isHtml = true;
                text = text.substring(6, text.length() - 7);
            } else {
                isHtml = false;
                if (!first) {
                    text = ' ' + text;
                }
            }

            this.dim = new Dimension(8 * (contentLength(text)+1), 16);
            if (hover == Hover.FLASH) {
                this.text = null;
                this.altText = fmts(text,isHtml,html);
            } else {
                this.text = fmts(text,isHtml,html);
                //final String coreText = text.startsWith("<html>") ? text.substring(6, text.length() - 7) : text;
                //String htmlSpace = first ? "" : "&nbsp;";
                if (hover == Hover.ITALICS) {
                    this.altText = fmts("<i>" + text + "</i>", true, html);
                } else if (hover == Hover.UNDERLINE) {
                    this.altText = fmts("<u>" + text + "</u>", true, html);
                } else {
                    this.altText = null;
                }
            }
        }

        private static String fmts(String text, boolean isHtml, String html) {
            if (isHtml) {
                return html + text + "</html>";
            } else {
                return text;
            }
        }

        private int apply(JLabel label, boolean hoveredRow, int labelXPos, ClickTree tree) {
            final boolean hit;
            if (delegate==null) {
                hit = hoveredRow && labelXPos >= 0 && labelXPos <= dim.width;
                if (hit) {
                    tree.hoverSegment = this;
                }
            } else {
                hit = tree.hoverSegment == delegate;
            }

            /*
            String t = (hit && altText != null) ? altText : text;
            if (t != null && !Character.isDigit(t.charAt(0))) {
                if (t.startsWith("<html>")) {
                    t = "<html>&nbsp;" + t.substring(6);
                } else {
                    t = " " + t;
                }
            }
            label.setText(t);
             */
            label.setText((hit && altText != null) ? altText : text);
            /*
            Dimension dim = label.getSize();
            dim.setSize(dim.width+6, dim.height);
            label.setMinimumSize(dim);
            label.setPreferredSize(dim);
            label.setSize(dim);
             */
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

        @SuppressWarnings("UnusedReturnValue")
        public TreeRow setSingleSegment(@NotNull String text) {
            rowSegments.clear();
            return addSegment(text);
        }

        public TreeRow addSegment(@NotNull String text) {
            return addSegment(text, Hover.NONE, null);
        }

        public TreeRow addSegment(@NotNull String text, @NotNull Hover hover, @Nullable TriConsumer<Component, Point, Boolean> action) {
            rowSegments.add(new RowSegment(text, hover, action, rowFont, rowSegments.isEmpty()));
            if (renderer.labels.size() < rowSegments.size()) {
                JLabel label = new JLabel();
                label.setOpaque(false);
                label.setVisible(false);
                renderer.add(label);
                renderer.labels.add(label);
            }
            return this;
        }

        private static final Font FLASH_FONT = new Font("Skia", Font.ITALIC, 10);

        @SuppressWarnings("UnusedReturnValue")
        public TreeRow addDelegatedSegment(@NotNull String text, @NotNull Hover hover) {
            if (rowSegments.isEmpty()) {
                throw new IllegalStateException("Cannot delegate to empty row");
            } else {
                RowSegment target = rowSegments.get(rowSegments.size() - 1);
                addSegment(text,hover,null);
                RowSegment addedSegment = rowSegments.get(rowSegments.size() - 1);
                addedSegment.delegate = target;
                addedSegment.font = FLASH_FONT;
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
                    int boundaryXPos = rowXStart;
                    for (int i = 0; i < labels.size(); i++) {
                        JLabel label = labels.get(i);
                        if (i >= treeRow.rowSegments.size()) {
                            label.setVisible(false);
                        } else {
                            label.setVisible(true);
                            RowSegment segment = treeRow.rowSegments.get(i);
                            label.setFont(segment.font);
                            boundaryXPos += segment.apply(label, hoveredRow == row && rowXPos >= 0, rowXPos - boundaryXPos, ClickTree.this);
                        }
                    }
                }
            }
            return this;
        }
    }
}