package org.pitestidea.toolwindow;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A custom button that transitions between states.
 */
public class TransitionButton extends JLabel {

    private record State(String text, JBColor color, Supplier<Boolean> action) {
    }

    private final List<State> states = new ArrayList<>();
    private int currentStateIndex = -1;

    public TransitionButton() {
        setOpaque(true);
        Border lineBorder = BorderFactory.createLineBorder(JBColor.BLACK, 1); // Black border with 2px thickness
        Border padding = BorderFactory.createEmptyBorder(0, 6, 0, 6);
        setBorder(BorderFactory.createCompoundBorder(lineBorder, padding));

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                transition();
            }
        });
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

    private void update() {
        State state = states.get(currentStateIndex);
        setText(state.text);
        setBackground(state.color);
    }

    /**
     * Add a new state that will be transitioned to from the last-added state. The default
     * appearance of the button is set by the first-added state.
     *
     * @param text for this state
     * @param color background for this state
     * @param action if not-null, called before transitioning and may prevent transition by returning false
     */
    void addState(String text, JBColor color, boolean makeCurrent, Supplier<Boolean> action) {
        State state = new State(text, color, action);
        states.add(state);
        if (makeCurrent || states.size() == 1) {
            currentStateIndex = states.size() - 1;
            update();
        }
    }
}
