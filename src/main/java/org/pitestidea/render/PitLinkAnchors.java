package org.pitestidea.render;

public class PitLinkAnchors {
    /**
     * Returns anchors for some mutation descriptions on <a href="https://pitest.org/quickstart/mutators">PITest.org</a>.
     * Only provide answers for less descriptive cases.
     *
     * @param mutationDescription read from mutations.xml
     * @return anchor or null if none match
     */
    public static String linkFor(String mutationDescription) {
        return switch (mutationDescription) {
            case "changed conditional boundary" -> "CONDITIONALS_BOUNDARY";
            case "negated conditional" -> "NEGATE_CONDITIONALS";
            default -> null;
        };
    }
}
