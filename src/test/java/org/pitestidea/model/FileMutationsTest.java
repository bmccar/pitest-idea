package org.pitestidea.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileMutationsTest {

    private Mutation mutation(int lineNumber, MutationImpact impact) {
        return new Mutation(impact, String.format("%d-%s", lineNumber, impact));
    }

    @Test
    public void lineSummary() {
        Mutation survived = mutation(1, MutationImpact.SURVIVED);
        Mutation killed = mutation(1, MutationImpact.KILLED);
        Mutation no_coverage = mutation(1, MutationImpact.NO_COVERAGE);
        Mutation timed_out = mutation(1, MutationImpact.TIMED_OUT);

        assertEquals(MutationImpact.SURVIVED, FileMutations.lineSummary(List.of(survived)));
        assertEquals(MutationImpact.KILLED, FileMutations.lineSummary(List.of(killed)));
        assertEquals(MutationImpact.NO_COVERAGE, FileMutations.lineSummary(List.of(no_coverage)));
        assertEquals(MutationImpact.TIMED_OUT, FileMutations.lineSummary(List.of(timed_out)));

        assertEquals(MutationImpact.SURVIVED, FileMutations.lineSummary(Arrays.asList(survived, survived)));
        assertEquals(MutationImpact.SURVIVED, FileMutations.lineSummary(Arrays.asList(survived, killed)));
        assertEquals(MutationImpact.TIMED_OUT, FileMutations.lineSummary(Arrays.asList(killed, timed_out)));
    }

    private void mutate(FileMutations fm, int lineNumber, MutationImpact impact) {
        Mutation mutation = mutation(lineNumber, impact);
        fm.add(lineNumber, mutation);
        new Track(lineNumber, mutation(lineNumber, impact));
    }

    @BeforeEach
    public void init() {
        Track.tracks = new ArrayList<>();
    }

    private static class Track {
        static List<Track> tracks;
        final Mutation mutation;
        final int lineNumber;
        boolean found = false;

        private Track(int lineNumber, Mutation mutation) {
            this.lineNumber = lineNumber;
            this.mutation = mutation;
            tracks.add(this);
        }

        static Optional<Track> find(int lineNumber, MutationImpact impact) {
            return tracks.stream().filter(track -> track.lineNumber == lineNumber && track.mutation.mutationImpact() == impact).findFirst();
        }

        @Override
        public String toString() {
            return String.format("%d[%s]", lineNumber, mutation);
        }
    }

    @Test
    public void mutationTypes() {
        FileMutations fm = new FileMutations("somePkg");
        mutate(fm, 1, MutationImpact.SURVIVED);
        mutate(fm, 2, MutationImpact.KILLED);
        mutate(fm, 3, MutationImpact.NO_COVERAGE);
        mutate(fm, 4, MutationImpact.TIMED_OUT);
    }

    @Test
    public void testGetMutationsTotalWithNoMutations() {
        FileMutations fm = new FileMutations("somePkg");
        mutate(fm, 5, MutationImpact.KILLED);
        mutate(fm, 5, MutationImpact.SURVIVED);
        mutate(fm, 8, MutationImpact.KILLED);

        fm.visit((lineNumber, lineImpact, mutations) -> mutations.forEach(mutation -> {
            Optional<Track> track = Track.find(lineNumber, mutation.mutationImpact());
            Assertions.assertTrue(track.isPresent());
            track.get().found = true;
        }));

        Track.tracks.stream().filter(track -> !track.found).forEach(track -> Assertions.fail("Missing track " + track));
    }
}