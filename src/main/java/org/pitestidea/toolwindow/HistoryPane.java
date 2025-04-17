package org.pitestidea.toolwindow;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

public class HistoryPane {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel();

    public HistoryPane() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        panel.add(scrollPane, BorderLayout.WEST);
    }

    public JComponent getComponent() {
        System.out.println("HistoryPane.getComponent()");
        return panel;
    }

    public void clear() {
        System.out.println("HistoryPane.clear()");
        contentPanel.removeAll();
    }

    public JPanel addRow() {
        System.out.println("HistoryPane.addRow()");
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(row);
        return row;
    }
}
