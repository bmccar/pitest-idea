package org.pitestidea.toolwindow;

public class Viewing {
    public enum PackageChoice {
        PACKAGE("All"),
        CODE("Coded"),
        NONE("Files only");
        final String displayName;

        PackageChoice(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
