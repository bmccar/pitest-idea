package org.pitestidea.model;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LineImpact {
    private final int lineNumber;
    private final MutationImpact lineImpactSummary;
    private final List<Mutation> mutations;
    private final MutationImpact lastMutationImpactSummary;
    private final List<Mutation> lastMutations;

    public LineImpact(int lineNumber, List<Mutation> mutations, List<Mutation> lastMutations) {
        this.lineNumber = lineNumber;
        this.lineImpactSummary = lineSummary(mutations);
        this.mutations = mutations;
        this.lastMutations = resolveSameOrDifferent(mutations, lastMutations);
        this.lastMutationImpactSummary = this.lastMutations==null ? lineImpactSummary : lineSummary(lastMutations);
    }

    /**
     * Compares lists and returns null if the same.
     *
     * @param mutations compare
     * @param lastMutations compare
     * @return null if same, else lastMutations
     */
    private static @Nullable List<Mutation> resolveSameOrDifferent(List<Mutation> mutations, List<Mutation> lastMutations) {
        return !mutations.equals(lastMutations) ? lastMutations : null;
    }

    public enum LineImpactPoint {
        CURRENT,
        PREVIOUS;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public MutationImpact getLineImpactSummary(LineImpactPoint point) {
        return point==LineImpactPoint.CURRENT?lineImpactSummary: lastMutationImpactSummary;
    }

    public List<Mutation> getMutations(LineImpactPoint point) {
        return point==LineImpactPoint.CURRENT?mutations: lastMutations;
    }

    @VisibleForTesting
    static MutationImpact lineSummary(List<Mutation> records) {
        int survived = 0;
        int killed = 0;
        int no_coverage = 0;
        int timed_out = 0;
        int run_error = 0;
        for (Mutation record : records) {
            switch (record.mutationImpact()) {
                case KILLED -> killed++;
                case SURVIVED -> survived++;
                case NO_COVERAGE -> no_coverage++;
                case TIMED_OUT -> timed_out++;
                case RUN_ERROR -> run_error++;
            }
        }
        if (survived > 0) {
            return MutationImpact.SURVIVED;
        } else if (timed_out > 0) {
            return MutationImpact.TIMED_OUT;
        } else if (killed > 0) {
            return MutationImpact.KILLED;
        } else if (no_coverage > 0) {
            return MutationImpact.NO_COVERAGE;
        } else if (run_error > 0) {
            return MutationImpact.RUN_ERROR;
        } else {
            throw new RuntimeException("No coverage for any line");
        }
    }
}
