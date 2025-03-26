package org.pitestidea.model;

public enum CoverageImpact {
    KILLED,
    SURVIVED,
    NO_COVERAGE,
    TIMED_OUT;

    final public String displayString;

    CoverageImpact() {
        this.displayString = name().toLowerCase();
    }
}
