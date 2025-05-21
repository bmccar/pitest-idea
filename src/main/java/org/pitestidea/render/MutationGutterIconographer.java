package org.pitestidea.render;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

class MutationGutterIconographer extends GutterIconRenderer {
    private final @NotNull Icon icon;
    private final String toolTip;

    MutationGutterIconographer(@NotNull Icon icon, String toolTip) {
        this.icon = icon;
        this.toolTip = toolTip;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MutationGutterIconographer that) {
            return that.icon.equals(this.icon) && that.toolTip.equals(this.toolTip);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, toolTip);
    }

    @Override
    public @NotNull Icon getIcon() {
        return icon;
    }

    @Override
    public String getTooltipText() {
        return toolTip;
    }
}
