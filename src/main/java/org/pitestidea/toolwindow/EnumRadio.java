package org.pitestidea.toolwindow;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

interface Displayable {
    public String getDisplayName();
    default public String getTooltip() {return null;}
}

/**
 * A UI widget that exposes the set of values of an enum and keeps track of the currently selected enum value.
 * @param <T> enum type
 */
class EnumRadio<T extends Enum<?> & Displayable> {
    private T selected;
    private final JPanel panel = new JPanel();

    /**
     * Creates a new radio.
     *
     * @param values all values of the enum
     * @param borderTitle if not null then a border title with this string value will be added
     * @param consumer called when a new value is selected
     */
    EnumRadio(T[] values, String borderTitle, Consumer<T> consumer) {
        panel.setLayout(new FlowLayout());
        ButtonGroup group = new ButtonGroup();
        int width = 50;
        for (T value : values) {
            JRadioButton button = new JRadioButton(value.getDisplayName());
            String tooltip = value.getTooltip();
            if (tooltip != null) {
                button.setToolTipText(value.getTooltip());
            }
            group.add(button);
            button.addActionListener(e -> {
                selected = value;
                consumer.accept(value);
            });
            width += button.getPreferredSize().width;
            panel.add(button);
        }
        // Set component size so that it does not get maximally stretched in its container.
        Dimension d = new Dimension(width, 60);
        panel.setMinimumSize(d);
        panel.setMaximumSize(d);
        panel.setPreferredSize(d);

        if (borderTitle != null) {
            panel.setBorder(BorderFactory.createTitledBorder(borderTitle));
        }

        this.setSelected(values[0]); // Default is first value
    }

    JPanel getPanel() {
        return panel;
    }

    void setSelected(T value) {
        selected = value;
        String match = value.getDisplayName();
        for (Component component : panel.getComponents()) {
            if (component instanceof JRadioButton button) {
                button.setSelected(button.getText().equals(match));
            }
        }
    }

    T getSelected() {
        return selected;
    }
}
