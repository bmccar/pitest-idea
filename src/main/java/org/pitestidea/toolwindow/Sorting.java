package org.pitestidea.toolwindow;

public class Sorting {

    public enum By implements Displayable {
        PROJECT("Project", "Sort by project structure"),
        //NAME("Name", "Sort alphabetically by file name"),
        SCORE("Score", "Sort by mutation score");

        final String displayName;
        final String tooltipText;

        By(String displayName, String tooltipText) {
            this.displayName = displayName;
            this.tooltipText = tooltipText;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTooltip() {
            return tooltipText;
        }
    }

    public enum Direction implements Displayable {
        ASC("Asc", "Sort lowest to highest"),
        DESC("Desc", "Sort highest to lowest");

        final String displayName;
        final String tooltipText;

        Direction(String displayName, String tooltipText) {
            this.displayName = displayName;
            this.tooltipText = tooltipText;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTooltip() {
            return tooltipText;
        }
    }
}
