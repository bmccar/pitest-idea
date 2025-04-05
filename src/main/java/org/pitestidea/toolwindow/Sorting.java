package org.pitestidea.toolwindow;

public class Sorting {

    public enum By {
        PROJECT("Project"),
        SCORE("Score");

        final String displayName;

        By(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Direction {
        ASC("Asc"),
        DESC("Desc");

        final String displayName;

        Direction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
