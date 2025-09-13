package org.pitestidea.render;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

class MutationGutterIconographer extends GutterIconRenderer {
    private final @NotNull Icon icon;
    private final int lineNumber;
    private final String toolTip;

    MutationGutterIconographer(@NotNull Icon icon, int lineNumber, String toolTip) {
        this.icon = icon;
        this.lineNumber = lineNumber;
        this.toolTip = toolTip;
    }

    @Override
    public @Nullable AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AiChatter.generateUnitTests(e, lineNumber);
            }
        };
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
