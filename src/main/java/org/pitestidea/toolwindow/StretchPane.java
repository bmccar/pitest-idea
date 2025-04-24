package org.pitestidea.toolwindow;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * A horizontal two-pane panel that can be transitioned between full-views (for either side) with buttons.
 */
public class StretchPane {
    private static PaneState currentState = PaneState.SCORES;

    private final JSplitPane splitPane;
    private final JLabel scoresButton;
    private final JLabel consoleButton;
    private final Consumer<PaneState> onStateChangeFn;


    enum PaneState {
        SCORES(1, "<", null),  // Scores is maximized
        MIXED(0.5, ">", "<"), // Split between scores and console
        CONSOLE(0, null, ">"); // Console is maximized

        private final double dividerLocation;
        private final String inScores;
        private final String inConsole;

        PaneState(double dividerLocation, String inScores, String inConsole) {
            this.dividerLocation = dividerLocation;
            this.inScores = inScores;
            this.inConsole = inConsole;
        }

        String getScoresText() {
            return inScores;
        }

        String getConsoleText() {
            return inConsole;
        }

        PaneState goScores() {
            return this==SCORES ? MIXED : /* Already MIXED */ SCORES;
        }

        PaneState goConsole() {
            return this==CONSOLE ? MIXED : /* Already MIXED */ CONSOLE;
        }

        public boolean isVisibleInState(PaneState state) {
            return this==MIXED || this == state;
        }
    }

    StretchPane(Consumer<PaneState> onStateChangeFn) {
        this.onStateChangeFn = onStateChangeFn;

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        scoresButton = DisplayUtils.createHoverLabel(LEFTWARD, ()->setState(currentState.goScores()));
        consoleButton = DisplayUtils.createHoverLabel(LEFTWARD, ()->setState(currentState.goConsole()));
    }

    void setLeft(Component component) {
        splitPane.setLeftComponent(component);
    }

    void setRight(Component component) {
        splitPane.setRightComponent(component);
    }

    public JComponent getComponent() {
        return splitPane;
    }

    public JLabel getScoresButton() {
        return scoresButton;
    }

    public JLabel getConsoleButton() {
        return consoleButton;
    }

    // TODO
    private static final String LEFTWARD = "<";
    private static final String RIGHTWARD = ">";

    public void setFullConsole() {
        setState(PaneState.CONSOLE);
    }

    public void setFullScores() {
        setState(PaneState.SCORES);
    }

    void setState(PaneState state) {
        currentState = state;
        scoresButton.setText(currentState.getScoresText());
        consoleButton.setText(currentState.getConsoleText());
        splitPane.setDividerLocation(currentState.dividerLocation);
        splitPane.setResizeWeight(currentState.dividerLocation);
        splitPane.getRightComponent().setVisible(currentState.isVisibleInState(currentState));
        if (onStateChangeFn != null) {
            onStateChangeFn.accept(currentState);
        }
    }
}
