package org.pitestidea.toolwindow;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.pitestidea.model.ExecutionRecord;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.awt.event.MouseAdapter;

/**
 * A list of JPanels arranged vertically within a scrollbar.
 */
public class HistoryList {
    private static final Font font = new Font("Andale Mono", Font.PLAIN, 10);

    private final JPanel panel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel();

    public HistoryList() {
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

    public JPanel addRow(ExecutionRecord record, boolean highlight, boolean valid, Runnable onClick) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(row);
        // Prevent layout from stretching rows across the page vertically
        row.setPreferredSize(new Dimension(300, 34));
        row.setMaximumSize(new Dimension(300, 34));
        if (highlight) {
            row.setBorder(BorderFactory.createLineBorder(JBColor.BLACK));
        }
        JPanel left = new JPanel(new FlowLayout());
        left.add(pfx(record.getFormattedStart()));
        left.add(pfx(record.getFormattedDuration()));

        JLabel label = new JLabel(record.getReportName());
        left.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onClick.run();
            }
        });
        if (!valid) {
            setInvalid(label);
        }
        left.setToolTipText(formatToolTip(record));
        left.add(label);
        row.add(left);
        return row;
    }

    private String formatToolTip(ExecutionRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("PIT Execution run<br>&nbsp;&nbsp;Started at ");
        sb.append(record.getFormattedStart()).append("<br>&nbsp;&nbsp;Duration: ");
        sb.append(record.getFormattedDuration());
        sb.append(record.getHtmlListOfInputs("<br><br>Inputs for this run", true));
        return sb.toString();
    }

    private JComponent pfx(String s) {
        JLabel label = new JLabel(s);
        label.setFont(font);
        return label;
    }


    void setInvalid(JLabel label) {
        Font font = label.getFont();

        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        Font strikethroughFont = new Font(attributes);

        label.setFont(strikethroughFont);
    }
}
