package org.pitestidea.reader;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.pitestidea.model.MutationImpact;
import org.pitestidea.toolwindow.DisplayChoices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MutationsFileReaderTest {

    private static final MockedStatic<ProjectRootManager> projectRootManagerStatic = Mockito.mockStatic(ProjectRootManager.class);

    @AfterAll
    static void afterAll() {
        projectRootManagerStatic.close();
    }

    private static class Xerciser implements IMutationsRecorder {
        private final StringBuilder sb = new StringBuilder();
        private final String mutatedClass;
        private final Set<String> expecting = new HashSet<>();
        private final VirtualFile file = Mockito.mock(VirtualFile.class);

        private Xerciser(String mutatedClass) {
            this.mutatedClass = mutatedClass;
            when(file.findFileByRelativePath(anyString())).thenReturn(file);
            ProjectRootManager projectRootManager = Mockito.mock(ProjectRootManager.class);
            when(projectRootManager.getContentSourceRoots()).thenAnswer((Answer<VirtualFile[]>) _i -> new VirtualFile[]{file});
            projectRootManagerStatic.when(() -> ProjectRootManager.getInstance(any())).thenReturn(projectRootManager);
            sb.append("<mutations>\n");
        }

        private String makeKey(int lineNumber, MutationImpact impact) {
            return lineNumber + "->" + impact;
        }

        private String getFileName() {
            return mutatedClass.replace('.',File.separatorChar) + ".java";
        }

        private String getDescription(int lineNumber, MutationImpact impact) {
            return mutatedClass + ":" + lineNumber + "-" + impact;
        }

        private String getPkg() {
            int ix = mutatedClass.lastIndexOf('.');
            if (ix < 0) {
                return "";
            } else {
                return mutatedClass.substring(0, ix);
            }
        }

        private void line(int lineNumber, MutationImpact impact) {
            expecting.add(makeKey(lineNumber, impact));
            sb.append("<mutation status='");
            sb.append(impact);
            sb.append("'><sourceFile>");
            sb.append(getFileName());
            sb.append("</sourceFile><mutatedClass>");
            sb.append(mutatedClass);
            sb.append("</mutatedClass><lineNumber>");
            sb.append(lineNumber);
            sb.append("</lineNumber><description>");
            sb.append(getDescription(lineNumber, impact));
            sb.append("</description></mutation>\n");
        }

        void verify() throws IOException, InvalidMutatedFileException {
            sb.append("</mutations>\n");
            File file = File.createTempFile("temp-", ".xml");
            Files.writeString(file.toPath(), sb.toString());
            Project project = Mockito.mock(Project.class);
            MutationsFileReader.read(project, file, this);
        }

        @Override
        public void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description) {
            assertEquals(getPkg(), pkg, "Unexpected package: " + pkg);
            assertEquals(this.file, file);
            assertTrue(expecting.remove(makeKey(lineNumber, impact)), "Unexpected mutation: " + description);
            assertEquals(getDescription(lineNumber,impact),description);
        }

        @Override
        public void postProcess() {

        }

        @Override
        public void sort(DisplayChoices choices) {

        }
    }

    @Test
    void readTopClassOneLine() throws IOException, InvalidMutatedFileException {
        Xerciser xr = new Xerciser("x");
        xr.line(1, MutationImpact.KILLED);
        xr.verify();
    }

    @Test
    void readClassOneLine() throws IOException, InvalidMutatedFileException {
        Xerciser xr = new Xerciser("x.y");
        xr.line(1, MutationImpact.TIMED_OUT);
        xr.verify();
    }

    @Test
    void readClassMultiLine() throws IOException, InvalidMutatedFileException {
        Xerciser xr = new Xerciser("x.y.z");
        xr.line(1, MutationImpact.NO_COVERAGE);
        xr.line(2, MutationImpact.TIMED_OUT);
        xr.line(2, MutationImpact.SURVIVED);
        xr.verify();
    }

    @Test
    void readZero() throws IOException, InvalidMutatedFileException {
        Xerciser xr = new Xerciser("x.y.z");
        xr.line(0, MutationImpact.KILLED);
        xr.line(0, MutationImpact.TIMED_OUT);
        xr.verify();
    }
}