package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.CsvSource;
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
        private final PitExecutionRecorder recorder = new PitExecutionRecorder(null, null);
        private final Map<String, VirtualFile> fileMap = new HashMap<>();

        // Track Top-level packages so we can know if a top-level aggregated results row should be expected
        private final Set<String> expectedTopLevelPackages = new HashSet<>();

        // Expected elements are removed one-by-one on each callback from the recorder
        private final Set<String> expectedPackages = new HashSet<>();
        private final List<ExpectedFileLine> expectedFileLines = new ArrayList<>();
        private final VirtualFile rootFile;

        // Within a test, packages must have a unique name because of this simplistic map
        private final Map<String, VirtualFile> packageMap = new HashMap<>();

        Tracker() {
            String nm = PitExecutionRecorder.ROOT_PACKAGE_NAME;
            expectedPackages.add(nm);
            rootFile = ensurePkg(null, PitExecutionRecorder.ROOT_PACKAGE_NAME);
        }

        private VirtualFile ensurePkg(VirtualFile parent, String childPkg) {
            return packageMap.computeIfAbsent(childPkg, _k -> {
                VirtualFile vf = Mockito.mock(VirtualFile.class);
                when(vf.getName()).thenReturn(childPkg);
                if (parent != null) {
                    when(vf.getParent()).thenReturn(parent);
                }
                return vf;
            });
        }

        void expect(String pkg, String file, MutationImpact impact, int lineNumber) {
            String description = "";
            VirtualFile vf = fileMap.computeIfAbsent(file, _k -> Mockito.mock(VirtualFile.class));
            when(vf.getName()).thenReturn(file);
            ExpectedFileLine track = new ExpectedFileLine(pkg, vf, impact, lineNumber, description);
            expectedFileLines.add(track);
            String[] ps = pkg.split("\\.");
            expectedTopLevelPackages.add(ps[0]);
            expectedPackages.addAll(Arrays.asList(ps));
            VirtualFile parent = rootFile;
            for (int i = 0; i < ps.length; i++) {
                parent = ensurePkg(parent, ps[i]);
            }
            when(vf.getParent()).thenReturn(parent);
            recorder.record(pkg, vf, impact, lineNumber, description);
        }

        void verify() {
            recorder.visit(this);

            // Expect that the collective calls to visit() below would have removed all of these entries
            assertTrue(expectedFileLines.isEmpty(), "Missed files");
            int expSize = expectedTopLevelPackages.size() == 1 ? 1 : 0;  // Account for ROOT_PACKAGE_NAME
            assertEquals(expSize, expectedPackages.size(), "Expected packages: " + expectedPackages);
        }

        @Override
        public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
            fileMutations.visit(lineImpact -> lineImpact.getMutations(LineImpact.LineImpactPoint.CURRENT).forEach(mutation -> {
                int lineNumber = lineImpact.getLineNumber();
                ExpectedFileLine match = new ExpectedFileLine(fileMutations.getPkg(), file, mutation.mutationImpact(), lineNumber, "");
                ExpectedFileLine expectedFileLine = expectedFileLines.stream().filter(t -> t.equals(match)).findFirst().orElse(null);
                assertNotNull(expectedFileLine, "Unexpected mutation on file " + file.getName() + " line " + lineNumber + " " + mutation.mutationImpact());
                assertEquals(expectedFileLine.pkg, fileMutations.getPkg());
                expectedFileLines.remove(expectedFileLine);
            }));
        }

        @Override
        public void visit(String pkg, String qualifiedPkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
            if (qualifiedPkg != null) {
                assertFalse(qualifiedPkg.startsWith(PitExecutionRecorder.ROOT_PACKAGE_NAME), "Qualified name should not start with \"" + PitExecutionRecorder.ROOT_PACKAGE_NAME + '"');
                String lastSegment = qualifiedPkg;
                int lastDot = lastSegment.lastIndexOf('.');
                if (lastDot > 0) {
                    lastSegment = lastSegment.substring(lastDot + 1);
                }
                assertEquals(pkg, lastSegment, "Unexpected qualified package name: " + pkg + ", " + qualifiedPkg);
            }
            if (pkg.equals(PitExecutionRecorder.ROOT_PACKAGE_NAME)) {
                assertTrue(expectedTopLevelPackages.size() > 1, "Unexpected package: " + pkg);
            }
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
        tracker.expect("ccc.ddd", "f1.java", MutationImpact.KILLED, 11);
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
    void sortWithOneFile() {
        Tracker tracker = new Tracker();

        tracker.expect("aaa", "f1.java", MutationImpact.SURVIVED, 5);

        verifyFileSort(tracker, Sorting.By.SCORE, Sorting.Direction.ASC, "f1.java");
        verifyFileSort(tracker, Sorting.By.PROJECT, Sorting.Direction.ASC, "f1.java");
    }

    @Test
    void sortByFiles() {
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

        verifyFileSort(tracker, Sorting.By.SCORE, Sorting.Direction.ASC, "f1.java", "f2.java", "f3.java", "f4.java");
        verifyFileSort(tracker, Sorting.By.SCORE, Sorting.Direction.DESC, "f4.java", "f3.java", "f2.java", "f1.java");
    }

    void sortBy(Sorting.By sortBy) {
        Tracker tracker = new Tracker();

        // NAME and SCORE sort in the same order
        tracker.expect("aaa", "a/f1.java", MutationImpact.SURVIVED, 5);

        tracker.expect("aaa", "a/f3.java", MutationImpact.SURVIVED, 5);
        tracker.expect("aaa", "a/f3.java", MutationImpact.KILLED, 5);
        tracker.expect("aaa", "a/f3.java", MutationImpact.KILLED, 5);

        tracker.expect("aaa", "b/f2.java", MutationImpact.SURVIVED, 5);
        tracker.expect("aaa", "b/f2.java", MutationImpact.KILLED, 5);

        tracker.expect("aaa", "b/f4.java", MutationImpact.KILLED, 5);

        verifyFileSort(tracker, sortBy, Sorting.Direction.ASC, "a/f1.java", "b/f2.java", "a/f3.java", "b/f4.java");
        verifyFileSort(tracker, sortBy, Sorting.Direction.DESC, "b/f4.java", "a/f3.java", "b/f2.java", "a/f1.java");
    }

    //@ParameterizedTest
    //@CsvSource({"NAME","SCORE"})
    @CsvSource({"SCORE"})
    void sortFilesInDifferentPackages(Sorting.By sortBy) {
        sortBy(sortBy);
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
            public void visit(String pkg, String qualifiedPkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
                assertTrue(expectedFileNames.length > 1, "Unexpected package: " + pkg);
            }
        });

        assertEquals(Arrays.asList(expectedFileNames), gotFileNames);
    }
}