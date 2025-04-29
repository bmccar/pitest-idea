package org.pitestidea.psi;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PackageWalkerTest {

    private static final String PACKAGE_NAME = "some.package.name";

    private static Project project;
    private static final PsiManager psiManager = Mockito.mock(PsiManager.class);
    private static ProjectFileIndex projectFileIndex;
    private static final MockedStatic<ProjectFileIndex> projectFileIndexStatic = Mockito.mockStatic(ProjectFileIndex.class);
    private static final MockedStatic<PsiManager> psiManagerStatic = Mockito.mockStatic(PsiManager.class);
    private static final MockedStatic<JavaDirectoryService> javaDirectoryServiceStatic = Mockito.mockStatic(JavaDirectoryService.class);
    private static final JavaDirectoryService javaDirectoryService = Mockito.mock(JavaDirectoryService.class);

    private static VirtualFile sourceFile;
    private static VirtualFile testFile;
    private static VirtualFile externalFile;
    private static VirtualFile sourceDir;
    private static VirtualFile testDir;
    private static VirtualFile externalDir;

    private static VirtualFile file(String name, boolean isDirectory, FileLoc loc) {
        VirtualFile vf = Mockito.mock(VirtualFile.class);
        when(vf.getName()).thenReturn(name);
        when(vf.isDirectory()).thenReturn(isDirectory);
        when(projectFileIndex.isInSourceContent(vf)).thenReturn(loc == FileLoc.SOURCE);
        when(projectFileIndex.isInTestSourceContent(vf)).thenReturn(loc == FileLoc.TEST);
        if (loc != FileLoc.EXTERNAL) {
            when(vf.getFileType()).thenReturn(JavaFileType.INSTANCE);
        }

        PsiClass psiClass = Mockito.mock(PsiClass.class);
        when(psiClass.getName()).thenReturn(name);
        PsiJavaFile psiJavaFile = Mockito.mock(PsiJavaFile.class);
        when(psiJavaFile.getName()).thenReturn(name);
        when(psiJavaFile.getPackageName()).thenReturn(PACKAGE_NAME);
        when(psiJavaFile.getClasses()).thenReturn(new PsiClass[]{psiClass});
        when(psiManager.findFile(vf)).thenReturn(psiJavaFile);;

        if (isDirectory) {
            PsiPackage psiPackage = Mockito.mock(PsiPackage.class);
            when(psiPackage.getQualifiedName()).thenReturn(pkg(name));
            PsiDirectory directory = Mockito.mock(PsiDirectory.class);
            when(psiManager.findDirectory(vf)).thenReturn(directory);
            when(javaDirectoryService.getPackage(directory)).thenReturn(psiPackage);
        }

        return vf;
    }

    @BeforeAll
    public static void setUpProject() {
        projectFileIndex = Mockito.mock(ProjectFileIndex.class);
        projectFileIndexStatic.when(() -> ProjectFileIndex.getInstance(any())).thenReturn(projectFileIndex);

        psiManagerStatic.when(() -> PsiManager.getInstance(any())).thenReturn(psiManager);

        javaDirectoryServiceStatic.when(JavaDirectoryService::getInstance).thenReturn(javaDirectoryService);

        project = Mockito.mock(Project.class);

        sourceFile = file("Foo.java", false, FileLoc.SOURCE);
        testFile = file("FooTest.java", false, FileLoc.TEST);
        externalFile = file("abc.xyz", false, FileLoc.EXTERNAL);
        sourceDir = file("x", true, FileLoc.SOURCE);
        testDir = file("y", true, FileLoc.TEST);
        externalDir = file("z", true, FileLoc.EXTERNAL);
    }
    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    private enum FileLoc {SOURCE, TEST, EXTERNAL}

    @SuppressWarnings("MisorderedAssertEqualsArguments")
    private void readAndExpect(VirtualFile vf, boolean expDirectory, final FileLoc expLoc, int expCount) {
        AtomicInteger count = new AtomicInteger(0);
        PackageWalker.read(project, Collections.singletonList(vf), new IPackageCollector() {
            @Override
            public void acceptCodePackage(String pkg) {
                count.incrementAndGet();
                assertTrue(expDirectory);
                assertEquals(expLoc, FileLoc.SOURCE);
                assertEquals(pkg(vf.getName()), pkg);
            }

            @Override
            public void acceptCodeClass(String qualifiedClassName, String fileName) {
                count.incrementAndGet();
                assertFalse(expDirectory);
                assertEquals(expLoc, FileLoc.SOURCE);

                System.out.printf("className=%s, fileName=%s, expName=%s%n", qualifiedClassName, fileName, vf.getName());
                assertEquals(pkg(vf.getName()), qualifiedClassName);
            }

            @Override
            public void acceptTestPackage(String pkg) {
                count.incrementAndGet();
                assertTrue(expDirectory);
                if (expLoc != FileLoc.SOURCE) {
                    assertEquals(expLoc, FileLoc.TEST);
                }
                assertEquals(pkg(vf.getName()), pkg);
            }

            @Override
            public void acceptTestClass(String qualifiedClassName) {
                count.incrementAndGet();
                assertFalse(expDirectory);
                if (expLoc == FileLoc.SOURCE) {
                    assertEquals(pkg(vf.getName()+"Test"), qualifiedClassName);
                } else {
                    assertEquals(expLoc, FileLoc.TEST);
                    assertEquals(pkg(vf.getName()), qualifiedClassName);
                }
            }
        });
        assertEquals(expCount, count.get(), "Unexpected number of visitor calls");
    }

    private static String pkg(String tail) {
        return PACKAGE_NAME + '.' + tail;
    }

    @Test
    void sourceFile() {
        readAndExpect(sourceFile, false, FileLoc.SOURCE, 2);
    }

    @Test
    void testFile() {
        readAndExpect(testFile, false, FileLoc.TEST, 1);
    }

    @Test
    void externalFile() {
        readAndExpect(externalFile, false, FileLoc.EXTERNAL, 0);
    }

    @Test
    void sourceDir() {
        readAndExpect(sourceDir, true, FileLoc.SOURCE, 2);
    }

    @Test
    void testDir() {
        readAndExpect(testDir, true, FileLoc.TEST, 1);
    }

    @Test
    void externalDir() {
        readAndExpect(externalDir, true, FileLoc.EXTERNAL, 0);
    }
}