package org.pitestidea.render;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PromptPopup {
    static void showCustomPopup(Project project, String content) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(600, 400));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // Create a header panel with a title and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // Title label
        //JLabel titleLabel = new JLabel(title);
        JLabel titleLabel = new JLabel("Copy and paste the following code into any promptable unit test generator:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(JBUI.Borders.emptyBottom(10));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        String title = "Unit Test Prompt Generator";
        
        // Create and show the popup first to get reference
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, null)
            .setTitle(title)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup();

        // The close icon button is positioned in the top right
        JButton closeButton = createIconButton(AllIcons.Actions.Close, "Close", popup::cancel);
        JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeButtonPanel.add(closeButton);
        headerPanel.add(closeButtonPanel, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content panel with a distinct background
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIUtil.getTextFieldBackground());
        contentPanel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1),
            JBUI.Borders.empty(8)
        ));

        // Control bar (now only contains a copy button)
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlBar.setBackground(UIUtil.getTextFieldBackground());

        // Copy button
        JButton copyButton = new JButton("Copy");
        copyButton.setPreferredSize(new Dimension(70, 28));
        copyButton.setFocusPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("Copy content to clipboard");

        // Button hover effect
        // Button hover effect
        Color originalColor = copyButton.getBackground();
        // JBColor constructor: JBColor(lightThemeColor, darkThemeColor)
        Color hoverColor = new JBColor(
            new Color(Math.max(0, originalColor.getRed() - 20),
                     Math.max(0, originalColor.getGreen() - 20),
                     Math.max(0, originalColor.getBlue() - 20)),  // Light theme color
            originalColor.brighter()  // Dark theme color
        );

        copyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                copyButton.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                copyButton.setBackground(originalColor);
            }
        });

        // Text area
        JBTextArea textArea = new JBTextArea(content);
        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(UIUtil.getTextFieldBackground());

        // Copy action
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

            // Brief visual feedback
            copyButton.setText("Copied!");
            Timer timer = new Timer(1000, event -> copyButton.setText("Copy"));
            timer.setRepeats(false);
            timer.start();
        });

        controlBar.add(copyButton);
        contentPanel.add(controlBar, BorderLayout.NORTH);
        contentPanel.add(new JBScrollPane(textArea), BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        popup.showCenteredInCurrentWindow(project);
    }

    private static JButton createIconButton(Icon icon, String tooltip, Runnable onClick) {
        JButton button = new JButton(icon);
        Dimension d = new Dimension(30, 30);
        button.setPreferredSize(d);
        button.setMinimumSize(d);
        button.setMaximumSize(d);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> onClick.run());
        return button;
    }
}
