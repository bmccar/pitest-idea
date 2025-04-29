package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pitestidea.toolwindow.DisplayChoices;
import org.pitestidea.toolwindow.Sorting;
import org.pitestidea.toolwindow.Viewing;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PitExecutionRecorderTest {

    private record ExpectedFileLine(
            String pkg,
            VirtualFile file,
            MutationImpact impact,
            int lineNumber,
            String description
    ) {
    }

    private static class Tracker implements PitExecutionRecorder.FileVisitor {
        private final PitExecutionRecorder recorder = new PitExecutionRecorder(null);
        private final Map<String, VirtualFile> fileMap = new HashMap<>();

        // These are filled with expected values, and lines are removed one-by-one on each callback from recorder
        private final List<ExpectedFileLine> expectedFileLines = new ArrayList<>();
        private final Set<String> expectedPackages = new HashSet<>();

        Tracker() {
            expectedPackages.add(PitExecutionRecorder.ROOT_PACKAGE_NAME);
        }

        void expect(String pkg, String file, MutationImpact impact, int lineNumber) {
            String description = "";
            VirtualFile vf = fileMap.computeIfAbsent(file, _k -> Mockito.mock(VirtualFile.class));
            when(vf.getName()).thenReturn(file);
            ExpectedFileLine track = new ExpectedFileLine(pkg, vf, impact, lineNumber, description);
            expectedFileLines.add(track);
            expectedPackages.remove(pkg);
            recorder.record(pkg, vf, impact, lineNumber, description);
        }

        void verify() {
            recorder.visit(this);

            // Expect that the collective calls to visit() below would have removed all of these entries
            assertTrue(expectedFileLines.isEmpty(), "Missed files");
            assertTrue(expectedPackages.isEmpty(), "Missed packages");
        }

        @Override
        public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
            System.out.println("[TEST] Visiting file " + fileMutations.getPkg() + '.' + file.getName());
            fileMutations.visit((lineNumber, lineImpactSummary, mutations) -> {
                System.out.println("[TEST] Visiting ln " + lineNumber + ", impactSummary " + lineImpactSummary);
                mutations.forEach(mutation -> {
                    System.out.println("[TEST] Visiting mutation " + lineNumber + " " + mutation.mutationImpact());
                    ExpectedFileLine match = new ExpectedFileLine(fileMutations.getPkg(), file, mutation.mutationImpact(), lineNumber, "");
                    ExpectedFileLine expectedFileLine = expectedFileLines.stream().filter(t -> t.equals(match)).findFirst().orElse(null);
                    assertNotNull(expectedFileLine, "Unexpected mutation on file " + file.getName() + " line " + lineNumber + " " + mutation.mutationImpact());
                    assertEquals(expectedFileLine.pkg, fileMutations.getPkg());
                    expectedFileLines.remove(expectedFileLine);
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
        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.verify();
    }

    @Test
    void twoMutationsOneFileOneLine() {
        Tracker tracker = new Tracker();
        String pkg = "aaa";
        String file = "f1.java";
        tracker.expect(pkg, file, MutationImpact.SURVIVED, 11);
        tracker.expect(pkg, file, MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void threeMutationsOneFileTwoLines() {
        Tracker tracker = new Tracker();
        String pkg = "aaa";
        String file = "f1.java";
        tracker.expect(pkg, file, MutationImpact.SURVIVED, 11);
        tracker.expect(pkg, file, MutationImpact.KILLED, 11);
        tracker.expect(pkg, file, MutationImpact.KILLED, 17);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoFiles() {
        Tracker tracker = new Tracker();
        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.expect("bbb", "f1.java", MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoDeepFiles() {
        Tracker tracker = new Tracker();
        tracker.expect("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.expect("aaa.ccc", "f1.java", MutationImpact.KILLED, 12);
        tracker.verify();
    }

    @Test
    void twoMutationsTwoShallowFiles() {
        Tracker tracker = new Tracker();
        tracker.expect("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.expect("bbb.ccc", "f1.java", MutationImpact.KILLED, 11);
        tracker.verify();
    }

    @Test
    void fourMutationsTwoShallowFiles() {
        Tracker tracker = new Tracker();
        tracker.expect("aaa.bbb", "f1.java", MutationImpact.SURVIVED, 11);
        tracker.expect("aaa.bbb", "f1.java", MutationImpact.KILLED, 11);
        tracker.expect("aaa.bbb", "f1.java", MutationImpact.KILLED, 13);
        tracker.expect("aaa.bbb", "f2.java", MutationImpact.NO_COVERAGE, 6);
        tracker.verify();
    }

    @Test
    void sortByFile() {
        Tracker tracker = new Tracker();

        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 5);
        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 6);
        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 7);
        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 8);

        tracker.expect("aaa", "f2.java", MutationImpact.SURVIVED, 5);
        tracker.expect("aaa", "f2.java", MutationImpact.SURVIVED, 6);
        tracker.expect("aaa", "f2.java", MutationImpact.SURVIVED, 7);
        tracker.expect("aaa", "f2.java", MutationImpact.KILLED, 8);

        tracker.expect("bbb", "f3.java", MutationImpact.SURVIVED, 5);
        tracker.expect("bbb", "f3.java", MutationImpact.SURVIVED, 6);
        tracker.expect("bbb", "f3.java", MutationImpact.KILLED, 7);
        tracker.expect("bbb", "f4.java", MutationImpact.KILLED, 8);

        tracker.expect("bbb", "f4.java", MutationImpact.SURVIVED, 5);
        tracker.expect("bbb", "f4.java", MutationImpact.KILLED, 6);
        tracker.expect("bbb", "f4.java", MutationImpact.KILLED, 7);
        tracker.expect("bbb", "f4.java", MutationImpact.KILLED, 8);

        verifyFileSort(tracker, Sorting.By.SCORE, Sorting.Direction.ASC,"f1.java", "f2.java", "f3.java", "f4.java");
        verifyFileSort(tracker, Sorting.By.SCORE, Sorting.Direction.DESC,"f4.java", "f3.java", "f2.java", "f1.java");
    }

    private static void verifyFileSort(Tracker tracker, Sorting.By sortBy, Sorting.Direction dir, String... expectedFileNames) {
        tracker.recorder.sort(new DisplayChoices(Viewing.PackageChoice.NONE, sortBy, dir));

        List<String> gotFileNames = new ArrayList<>();

        tracker.recorder.visit(new PitExecutionRecorder.FileVisitor() {
            @Override
            public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
                gotFileNames.add(file.getName());
            }

            @Override
            public void visit(String pkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
                fail("Unexpected package" + pkg);
            }
        });

        assertEquals(Arrays.asList(expectedFileNames), gotFileNames);
    }
}