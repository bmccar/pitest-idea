package org.pitestidea.model;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the outcome of PITest for a given file.
 */
public class FileMutations /*extends BaseMutationsScore*/ {
    private final String pkg;
    private final Map<Integer, List<Mutation>> lineMutations = new HashMap<>();

    public FileMutations(String pkg) {
        this.pkg = pkg;
    }

    public String getPkg() {
        return pkg;
    }

    public void add(int lineNumber, Mutation mutation) {
        List<Mutation> mutations = lineMutations.computeIfAbsent(lineNumber, _x -> new ArrayList<>());
        mutations.add(mutation);
    }

    public interface LineVisitor {
        void visit(int lineNumber, MutationImpact lineImpact, List<Mutation> mutations);
    }

    public void visit(LineVisitor visitor) {
        lineMutations.forEach((lineNumber,lines)->visitor.visit(lineNumber,lineSummary(lines),lines));
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
