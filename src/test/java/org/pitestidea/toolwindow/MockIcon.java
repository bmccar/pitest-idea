package org.pitestidea.toolwindow;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

// Helper class for testing
class MockIcon implements Icon {
    private final String id;

    public MockIcon(String id) {
        this.id = id;
    }

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

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockIcon mockIcon = (MockIcon) o;
        return Objects.equals(id, mockIcon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MockIcon{" + "id='" + id + '\'' + '}';
    }
}