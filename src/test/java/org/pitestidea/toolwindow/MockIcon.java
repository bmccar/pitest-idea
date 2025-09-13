package org.pitestidea.toolwindow;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

// Helper class for testing
record MockIcon(String id) implements Icon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // No-op for testing
    }

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockIcon mockIcon = (MockIcon) o;
        return Objects.equals(id, mockIcon.id);
    }

    @Override
    public String toString() {
        return "MockIcon{" + "id='" + id + '\'' + '}';
    }
}