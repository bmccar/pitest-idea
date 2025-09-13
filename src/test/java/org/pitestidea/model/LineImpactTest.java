package org.pitestidea.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LineImpactTest {

    private Mutation mutation(MutationImpact impact) {
        return new Mutation("etc", 0, impact, String.format("%s", impact));
    }

    @Test
    public void lineSummary() {
        Mutation survived = mutation(MutationImpact.SURVIVED);
        Mutation killed = mutation(MutationImpact.KILLED);
        Mutation no_coverage = mutation(MutationImpact.NO_COVERAGE);
        Mutation timed_out = mutation(MutationImpact.TIMED_OUT);

        assertEquals(MutationImpact.SURVIVED, LineImpact.lineSummary(List.of(survived)));
        assertEquals(MutationImpact.KILLED, LineImpact.lineSummary(List.of(killed)));
        assertEquals(MutationImpact.NO_COVERAGE, LineImpact.lineSummary(List.of(no_coverage)));
        assertEquals(MutationImpact.TIMED_OUT, LineImpact.lineSummary(List.of(timed_out)));

        assertEquals(MutationImpact.SURVIVED, LineImpact.lineSummary(Arrays.asList(survived, survived)));
        assertEquals(MutationImpact.SURVIVED, LineImpact.lineSummary(Arrays.asList(survived, killed)));
        assertEquals(MutationImpact.TIMED_OUT, LineImpact.lineSummary(Arrays.asList(killed, timed_out)));
    }
}