package org.pitestidea.toolwindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

class DisplayUtils {
    private static final Font hover = new Font("Verdana", Font.BOLD, 24);
    private static final Font unhover = new Font("Verdana", Font.PLAIN, 24);

    static JLabel createHoverLabel(String text, Runnable consumer) {
        text = ' ' + text + ' ';
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setFont(unhover);
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setFont(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setFont(unhover);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                consumer.run();
            }
        });
        return label;
    }
}
