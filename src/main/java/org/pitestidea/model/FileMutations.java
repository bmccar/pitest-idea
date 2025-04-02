package org.pitestidea.model;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the outcome of PITest for a given file.
 */
public class FileMutations {
    private final String pkg;
    private final Map<Integer, List<Mutation>> lineMutations = new HashMap<>();
    private int survived = 0;
    private int killed = 0;
    private int noCoverage = 0;
    private int timedOut = 0;

    public FileMutations(String pkg) {
        this.pkg = pkg;
    }

    public String getPkg() {
        return pkg;
    }

    public int getSurvived() {
        return survived;
    }

    public int getKilled() {
        return killed;
    }

    public int getNoCoverage() {
        return noCoverage;
    }

    public int getTimedOut() {
        return timedOut;
    }

    public int getMutationsTotal() {
        return survived + killed + noCoverage + timedOut;
    }

    public float getMutationCoverageScore() {
        return 100*(float)killed/(float)getMutationsTotal();
    }

    public void add(int lineNumber, Mutation mutation) {
        List<Mutation> mutations = lineMutations.computeIfAbsent(lineNumber, _x -> new ArrayList<>());
        mutations.add(mutation);
        switch (mutation.mutationImpact()) {
            case KILLED -> killed++;
            case SURVIVED -> survived++;
            case NO_COVERAGE -> noCoverage++;
            case TIMED_OUT -> timedOut++;
        }
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
        for (Mutation record : records) {
            switch (record.mutationImpact()) {
                case KILLED -> killed++;
                case SURVIVED -> survived++;
                case NO_COVERAGE -> no_coverage++;
                case TIMED_OUT -> timed_out++;
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
        } else {
            throw new RuntimeException("No coverage for any line");
        }
    }
}
