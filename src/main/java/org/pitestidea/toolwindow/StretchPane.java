package org.pitestidea.toolwindow;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * A horizontal two-pane panel that can be transitioned between full-views (for either side) with buttons.
 */
public class StretchPane {
    private static final Icon stretchLeft = IconLoader.getIcon("/icons/playBack.svg", StretchPane.class);
    private static final Icon stretchRight = IconLoader.getIcon("/icons/playForward.svg", StretchPane.class);

    private static PaneState currentState = PaneState.SCORES;

    private final JSplitPane splitPane;
    private final JButton scoresButton;
    private final JButton consoleButton;
    private final Consumer<PaneState> onStateChangeFn;


    enum PaneState {
        SCORES(1, stretchLeft, null),  // Scores is maximized
        MIXED(0.5, stretchRight, stretchLeft), // Split between scores and console
        CONSOLE(0, null, stretchRight); // Console is maximized

        private final double dividerLocation;
        private final Icon inScores;
        private final Icon inConsole;

        PaneState(double dividerLocation, Icon inScores, Icon inConsole) {
            this.dividerLocation = dividerLocation;
            this.inScores = inScores;
            this.inConsole = inConsole;
        }

        Icon getScoresIcon() {
            return inScores;
        }

        Icon getConsoleIcon() {
            return inConsole;
        }

        PaneState goScores() {
            return this==SCORES ? MIXED : /* Already MIXED */ SCORES;
        }

        PaneState goConsole() {
            return this==CONSOLE ? MIXED : /* Already MIXED */ CONSOLE;
        }

        public boolean isVisibleInState(PaneState state) {
            return currentState==MIXED || this == state;
        }
    }

    StretchPane(Consumer<PaneState> onStateChangeFn) {
        this.onStateChangeFn = onStateChangeFn;

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        scoresButton = createIconButton(stretchLeft,"Stretch left", ()->setState(currentState.goScores()));
        consoleButton = createIconButton(stretchRight,"Stretch right", ()->setState(currentState.goConsole()));
    }

    private JButton createIconButton(Icon icon, String tooltip, Runnable onClick) {
        JButton button = new JButton(icon);
        Dimension d = new Dimension(30,30);
        button.setPreferredSize(d);
        button.setMinimumSize(d);
        button.setMaximumSize(d);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> {
            onClick.run();
        });
        return button;
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

    public JComponent getScoresButton() {
        return scoresButton;
    }

    public JComponent getConsoleButton() {
        return consoleButton;
    }

    public void setFullConsole() {
        setState(PaneState.CONSOLE);
    }

    public void setFullScores() {
        setState(PaneState.SCORES);
    }

    void setState(PaneState state) {
        currentState = state;
        scoresButton.setIcon(currentState.getScoresIcon());
        consoleButton.setIcon(currentState.getConsoleIcon());
        splitPane.setDividerLocation(currentState.dividerLocation);
        splitPane.setResizeWeight(currentState.dividerLocation);
        splitPane.getLeftComponent().setVisible(PaneState.SCORES.isVisibleInState(currentState));
        splitPane.getRightComponent().setVisible(PaneState.CONSOLE.isVisibleInState(currentState));
        if (onStateChangeFn != null) {
            onStateChangeFn.accept(currentState);
        }
    }
}
