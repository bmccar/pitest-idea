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

    /**
     * Creates a new radio.
     *
     * @param values all values of the enum
     * @param displayFn what to display
     * @param consumer called when a new value is selected
     */
    EnumRadio(T[] values, Function<T,String> displayFn, Consumer<T> consumer) {
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
    }

    JPanel getPanel() {
        return panel;
    }

    T getSelected() {
        return selected;
    }
}
