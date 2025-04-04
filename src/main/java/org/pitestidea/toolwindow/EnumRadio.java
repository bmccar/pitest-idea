package org.pitestidea.toolwindow;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A UI widget that exposes the set of values of an enum and keeps track of the currently selected enum value.
 * @param <T> enum type
 */
class EnumRadio<T extends Enum<?>> {
    private T selected;
    private final JPanel panel = new JPanel();
    private final Function<T,String> displayFn;

    /**
     * Creates a new radio.
     *
     * @param values all values of the enum
     * @param displayFn what to display
     * @param consumer called when a new value is selected
     */
    EnumRadio(T[] values, Function<T,String> displayFn, Consumer<T> consumer) {
        this.displayFn = displayFn;
        panel.setLayout(new FlowLayout());
        ButtonGroup group = new ButtonGroup();
        for (T value : values) {
            JRadioButton button = new JRadioButton(displayFn.apply(value));
            group.add(button);
            button.addActionListener(e -> {
                selected = value;
                consumer.accept(value);
            });
            panel.add(button);
        }
        this.setSelected(values[0]); // Default is first value
    }

    JPanel getPanel() {
        return panel;
    }

    void setSelected(T value) {
        selected = value;
        String match = displayFn.apply(value);
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
