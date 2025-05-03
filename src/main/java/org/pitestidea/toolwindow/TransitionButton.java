package org.pitestidea.toolwindow;

import com.intellij.ui.JBColor;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A custom button that transitions between states.
 */
public class TransitionButton extends JButton {

    static final Dimension BUTTON_SIZE = new Dimension(20, 20);

    public record State(String name, Icon icon, String tooltipText, Supplier<Boolean> action) {
        @Override
        public String toString() {
            return String.format("State(%s)", name);
        }
    }

    private final List<State> states = new ArrayList<>();
    private int currentStateIndex = -1;

    public TransitionButton() {
        setOpaque(false); // Don't fill the button background
        setContentAreaFilled(false); // Transparent content area
        setBorderPainted(false); // Optionally remove border
        setPreferredSize(BUTTON_SIZE);
        setMinimumSize(BUTTON_SIZE);
        setMaximumSize(BUTTON_SIZE);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                transition();
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(enabled ? JBColor.BLACK : JBColor.GRAY);
    }

    /**
     * Force the next state transition.
     */
    void transition() {
        if (currentStateIndex >= 0) {
            State currentState = states.get(currentStateIndex);
            boolean transition = currentState.action == null || currentState.action.get();
            if (transition) {
                if (currentStateIndex == states.size() - 1) {
                    currentStateIndex = 0;
                } else {
                    currentStateIndex++;
                }
            }
            update();
        }
    }

    void restore(State state) {
        currentStateIndex = states.indexOf(state);
        update();
    }

    State getState() {
        return currentStateIndex >= 0 ? states.get(currentStateIndex) : null;
    }

    private void update() {
        State state = states.get(currentStateIndex);
        setIcon(state.icon);
        setToolTipText(state.tooltipText);
    }

    /**
     * Add a new state that will be transitioned to from the last-added state. The default
     * appearance of the button is set by the first-added state.
     *
     * @param name for debugging
     * @param icon to display
     * @param tooltipText to display
     * @param action if not-null, called before transitioning and may prevent transition by returning false
     */
    State addState(String name, Icon icon, String tooltipText, boolean makeCurrent, Supplier<Boolean> action) {
        State state = new State(name,icon, tooltipText, action);
        states.add(state);
        if (makeCurrent || states.size() == 1) {
            currentStateIndex = states.size() - 1;
            update();
        }
        return state;
    }

}
