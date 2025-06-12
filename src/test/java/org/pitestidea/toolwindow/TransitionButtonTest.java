package org.pitestidea.toolwindow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.intellij.ui.JBColor;

import static org.junit.jupiter.api.Assertions.*;

class TransitionButtonTest {

    private TransitionButton button;
    private MockIcon icon1;
    private MockIcon icon2;
    private MockIcon icon3;

    @BeforeEach
    void setUp() {
        button = new TransitionButton();
        icon1 = new MockIcon("icon1");
        icon2 = new MockIcon("icon2");
        icon3 = new MockIcon("icon3");
    }

    @Test
    @DisplayName("Button should have correct default visual properties")
    void testDefaultVisualProperties() {
        assertFalse(button.isOpaque(), "Button should not be opaque by default");
        assertFalse(button.isContentAreaFilled(), "Button content area should not be filled by default");
        assertFalse(button.isBorderPainted(), "Button border should not be painted by default");
        assertEquals(TransitionButton.BUTTON_SIZE, button.getPreferredSize(), "Preferred size mismatch");
        assertEquals(TransitionButton.BUTTON_SIZE, button.getMinimumSize(), "Minimum size mismatch");
        assertEquals(TransitionButton.BUTTON_SIZE, button.getMaximumSize(), "Maximum size mismatch");
    }

    @Test
    @DisplayName("Adding the first state should make it current and update UI")
    void testAddFirstState() {
        button.addState("State1", icon1, "Tooltip1", false, null);
        assertEquals(icon1, button.getIcon(), "Icon should be set to the first state's icon");
        assertEquals("Tooltip1", button.getToolTipText(), "Tooltip should be set to the first state's tooltip");
    }

    @Test
    @DisplayName("Adding a state with makeCurrent=true should make it current")
    void testAddStateAndMakeCurrent() {
        button.addState("State1", icon1, "Tooltip1", false, null);
        button.addState("State2", icon2, "Tooltip2", true, null);
        assertEquals(icon2, button.getIcon(), "Icon should be updated to the new current state");
        assertEquals("Tooltip2", button.getToolTipText(), "Tooltip should be updated to the new current state");
    }

    @Test
    @DisplayName("Adding a state with makeCurrent=false should not change current state (if not first)")
    void testAddStateNotMakeCurrent() {
        button.addState("State1", icon1, "Tooltip1", true, null); // Becomes current
        button.addState("State2", icon2, "Tooltip2", false, null); // Add but don't make current
        assertEquals(icon1, button.getIcon(), "Icon should remain from the initial current state");
        assertEquals("Tooltip1", button.getToolTipText(), "Tooltip should remain from the initial current state");
    }

    @Test
    @DisplayName("Transition should do nothing if no states are added")
    void testTransitionWithNoStates() {
        // currentStateIndex is -1 initially
        assertDoesNotThrow(() -> button.transition());
        assertNull(button.getIcon(), "Icon should remain null");
        assertNull(button.getToolTipText(), "Tooltip should remain null");
    }

    @Test
    @DisplayName("Transition should cycle to next state if action is null")
    void testTransitionWithNullAction() {
        button.addState("State1", icon1, "Tooltip1", true, null);
        button.addState("State2", icon2, "Tooltip2", false, null);
        button.transition();
        assertEquals(icon2, button.getIcon(), "Should transition to State2's icon");
        assertEquals("Tooltip2", button.getToolTipText(), "Should transition to State2's tooltip");
    }

    @Test
    @DisplayName("Transition should cycle to next state if action returns true")
    void testTransitionWhenActionReturnsTrue() {
        Supplier<Boolean> allowTransition = () -> true;
        button.addState("State1", icon1, "Tooltip1", true, allowTransition);
        button.addState("State2", icon2, "Tooltip2", false, null); // Next state
        button.transition();
        assertEquals(icon2, button.getIcon(), "Should transition to State2's icon because action returned true");
        assertEquals("Tooltip2", button.getToolTipText(), "Should transition to State2's tooltip");
    }

    @Test
    @DisplayName("Transition should not occur if action returns false")
    void testNoTransitionWhenActionReturnsFalse() {
        Supplier<Boolean> preventTransition = () -> false;
        button.addState("State1", icon1, "Tooltip1", true, preventTransition);
        button.addState("State2", icon2, "Tooltip2", false, null);
        button.transition();
        assertEquals(icon1, button.getIcon(), "Should not transition from State1 because action returned false");
        assertEquals("Tooltip1", button.getToolTipText(), "Tooltip should remain for State1");
    }

    @Test
    @DisplayName("Transition should wrap around from last state to first")
    void testTransitionWrapsAround() {
        button.addState("State1", icon1, "Tooltip1", true, null);
        button.addState("State2", icon2, "Tooltip2", false, null);
        button.transition(); // Now on State2
        assertEquals(icon2, button.getIcon());
        button.transition(); // Should wrap to State1
        assertEquals(icon1, button.getIcon(), "Should wrap around to State1's icon");
        assertEquals("Tooltip1", button.getToolTipText(), "Should wrap around to State1's tooltip");
    }
    
    @Test
    @DisplayName("Transition on a single state should re-evaluate action but remain on same state visually")
    void testTransitionWithSingleState() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        Supplier<Boolean> action = () -> {
            actionCalled.set(true);
            return true; // Allow "transition" (which means staying in place)
        };
        button.addState("State1", icon1, "Tooltip1", true, action);
        button.transition();
        assertTrue(actionCalled.get(), "Action should be called on transition even with a single state");
        assertEquals(icon1, button.getIcon(), "Icon should remain the same for a single state");
        assertEquals("Tooltip1", button.getToolTipText(), "Tooltip should remain the same");
    }


    @Test
    @DisplayName("setEnabled should update foreground color")
    void testSetEnabledUpdatesForeground() {
        // Note: Direct comparison with JBColor might be tricky in pure unit tests
        // if JBColor itself has complex behavior (like theme changes).
        // This test assumes JBColor.BLACK and JBColor.GRAY are distinct and accessible.
        button.setEnabled(true);
        assertEquals(JBColor.BLACK, button.getForeground(), "Foreground should be JBColor.BLACK when enabled");

        button.setEnabled(false);
        assertEquals(JBColor.GRAY, button.getForeground(), "Foreground should be JBColor.GRAY when disabled");
    }

    @Test
    @DisplayName("Transitioning updates icon and tooltip correctly after action prevents then allows")
    void testTransitionAfterActionBlock() {
        AtomicBoolean canTransition = new AtomicBoolean(false);
        Supplier<Boolean> conditionalAction = canTransition::get;

        button.addState("State1", icon1, "Tooltip1", true, conditionalAction);
        button.addState("State2", icon2, "Tooltip2", false, null);
        button.addState("State3", icon3, "Tooltip3", false, null);

        // Attempt transition from State1, action returns false
        button.transition();
        assertEquals(icon1, button.getIcon(), "Should remain on State1 (icon)");
        assertEquals("Tooltip1", button.getToolTipText(), "Should remain on State1 (tooltip)");

        // Allow transition
        canTransition.set(true);
        button.transition();
        assertEquals(icon2, button.getIcon(), "Should transition to State2 (icon)");
        assertEquals("Tooltip2", button.getToolTipText(), "Should transition to State2 (tooltip)");

        // Transition from State2 (null action)
        button.transition();
        assertEquals(icon3, button.getIcon(), "Should transition to State3 (icon)");
        assertEquals("Tooltip3", button.getToolTipText(), "Should transition to State3 (tooltip)");
    }
}