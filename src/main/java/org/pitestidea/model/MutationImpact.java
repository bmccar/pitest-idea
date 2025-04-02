package org.pitestidea.model;

public enum MutationImpact {
    KILLED,
    SURVIVED,
    NO_COVERAGE,
    TIMED_OUT;

    final public String displayString;

    MutationImpact() {
        this.displayString = name().toLowerCase();
    }
}
