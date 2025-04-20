package org.pitestidea.toolwindow;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

/**
 * A list of JPanels arranged vertically within a scrollbar.
 */
public class VerticalList {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel();

    public VerticalList() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        panel.add(scrollPane, BorderLayout.WEST);
    }

    public JComponent getComponent() {
        return panel;
    }

    public void clear() {
        contentPanel.removeAll();
    }

    public JPanel addRow(String mainText, boolean highlight, Runnable onClick) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(row);
        // Prevent layout from stretching rows across the page vertically
        row.setPreferredSize(new Dimension(300, 30));
        row.setMaximumSize(new Dimension(300, 30));
        if (highlight) {
            row.setBorder(BorderFactory.createLineBorder(JBColor.BLACK));
        }
        JLabel label = new JLabel(mainText);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onClick.run();
            }
        });
        row.add(label);
        return row;
    }
}
