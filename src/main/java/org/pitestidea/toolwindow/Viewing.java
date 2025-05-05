package org.pitestidea.toolwindow;

public class Viewing {
    public enum PackageChoice implements Displayable {
        PACKAGE("All","Display complete package hierarchy"),
        CODE("Coded", "Display only packages with direct code files"),
        NONE("Flat", "Code files only, no packages");
        final String displayName;
        final String tooltipText;

        PackageChoice(String displayName, String tooltipText) {
            this.displayName = displayName;
            this.tooltipText = tooltipText;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTooltip() {return tooltipText;}
    }
}
