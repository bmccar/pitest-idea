package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PitExecutionRecorderTest {

    private record FileTrack(
            String pkg,
            VirtualFile file,
            MutationImpact impact,
            int lineNumber,
            String description
    ) {
    }

    private static class Tracker implements PitExecutionRecorder.FileVisitor {
        private final PitExecutionRecorder recorder = new PitExecutionRecorder(null, null);
        private final List<FileTrack> expectedTracks = new ArrayList<>();
        private final Set<String> expectedPackages = new HashSet<>();
        private final Map<String, VirtualFile> files = new HashMap<>();

        Tracker() {
            expectedPackages.add(PitExecutionRecorder.ROOT_PACKAGE_NAME);
        }

        void record(String pkg, String file, MutationImpact impact, int lineNumber) {
            String description = "";
            VirtualFile vf = files.computeIfAbsent(file, _k -> Mockito.mock(VirtualFile.class));
            when(vf.getName()).thenReturn(file);
            FileTrack track = new FileTrack(pkg, vf, impact, lineNumber, description);
            expectedTracks.add(track);
            expectedPackages.remove(pkg);
            recorder.record(pkg, vf, impact, lineNumber, description);
        }

        void verify() {
            recorder.visit(this);
            assertTrue(expectedTracks.isEmpty(), "Missed files");
            assertTrue(expectedPackages.isEmpty(), "Missed packages");
        }

        @Override
        public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
            System.out.println("[TEST] Visiting file " + fileMutations.getPkg() + '.' + file.getName());
            fileMutations.visit((lineNumber, lineImpactSummary, mutations) -> {
                System.out.println("[TEST] Visiting ln " + lineNumber + ", impactSummary " + lineImpactSummary);
                mutations.forEach(mutation -> {
                    System.out.println("[TEST] Visiting mutation " + lineNumber + " " + mutation.mutationImpact());
                    FileTrack match = new FileTrack(fileMutations.getPkg(), file, mutation.mutationImpact(), lineNumber, "");
                    FileTrack fileTrack = expectedTracks.stream().filter(t -> t.equals(match)).findFirst().orElse(null);
                    assertNotNull(fileTrack, "Unexpected mutation on file " + file.getName() + " line " + lineNumber + " " + mutation.mutationImpact());
                    assertEquals(fileTrack.pkg, fileMutations.getPkg());
                    expectedTracks.remove(fileTrack);
                });
            });
        }

        @Override
        public void visit(String pkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
            System.out.println("[TEST] Visiting package " + pkg);
            expectedPackages.remove(pkg);
            diver.apply(this);
        }
    }

    @Test
    void oneMutation() {
        Tracker tracker = new Tracker();
        tracker.record("aaa", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.verify();
    }

    @Test
    void twoMutationsOneFileOneLine() {
        Tracker tracker = new Tracker();
        String pkg = "aaa";
        String file = "f1.java";
        tracker.record(pkg, file, MutationImpact.SURVIVED, 11);
        tracker.record(pkg, file, MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void threeMutationsOneFileTwoLines() {
        Tracker tracker = new Tracker();
        String pkg = "aaa";
        String file = "f1.java";
        tracker.record(pkg, file, MutationImpact.SURVIVED, 11);
        tracker.record(pkg, file, MutationImpact.KILLED, 11);
        tracker.record(pkg, file, MutationImpact.KILLED, 17);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoFiles() {
        Tracker tracker = new Tracker();
        tracker.record("aaa", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.record("bbb", "f1.java", MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoDeepFiles() {
        Tracker tracker = new Tracker();
        tracker.record("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.record("aaa.ccc", "f1.java", MutationImpact.KILLED, 12);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoShallowFiles() {
        Tracker tracker = new Tracker();
        tracker.record("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.record("bbb.ccc", "f1.java", MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void fourMutationsTwoShallowFiles() {
        Tracker tracker = new Tracker();
        tracker.record("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.record("aaa.bbb", "f1.java", MutationImpact.KILLED, 11);
        tracker.record("aaa.bbb", "f1.java", MutationImpact.KILLED, 13);
        tracker.record("aaa.bbb", "f2.java", MutationImpact.NO_COVERAGE, 6);
        tracker.verify();
    }

}