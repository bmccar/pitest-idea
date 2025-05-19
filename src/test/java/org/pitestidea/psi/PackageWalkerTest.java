package org.pitestidea.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.pitestidea.model.InputBundle;
import org.pitestidea.psi.fakes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PackageWalkerTest {
    private static final Project project = Mockito.mock(Project.class);

    private static final PsiManager psiManager = Mockito.mock(PsiManager.class);

    private static final ProjectRootManager projectRootManager = Mockito.mock(ProjectRootManager.class);
    private static final MockedStatic<ProjectRootManager> projectRootManagerStatic = Mockito.mockStatic(ProjectRootManager.class);

    private static final ProjectFileIndex projectFileIndex = Mockito.mock(ProjectFileIndex.class);
    private static final MockedStatic<ProjectFileIndex> projectFileIndexStatic = Mockito.mockStatic(ProjectFileIndex.class);

    private static final MockedStatic<PsiManager> psiManagerStatic = Mockito.mockStatic(PsiManager.class);

    private static final JavaDirectoryService javaDirectoryService = Mockito.mock(JavaDirectoryService.class);
    private static final MockedStatic<JavaDirectoryService> javaDirectoryServiceStatic = Mockito.mockStatic(JavaDirectoryService.class);

    private static final VirtualFileManager virtualFileManager = Mockito.mock(VirtualFileManager.class);
    private static final MockedStatic<VirtualFileManager> virtualFileManagerStatic = Mockito.mockStatic(VirtualFileManager.class);


    @BeforeAll
    public static void setUpProject() {
        projectRootManagerStatic.when(() -> ProjectRootManager.getInstance(any())).thenReturn(projectRootManager);
        when(projectRootManager.getContentSourceRoots()).thenAnswer((Answer<VirtualFile[]>) invocation -> RootFake.getContentSourceRoots());
        projectFileIndexStatic.when(() -> ProjectFileIndex.getInstance(any())).thenReturn(projectFileIndex);

        when(projectFileIndex.getSourceRootForFile(any())).thenAnswer((Answer<VirtualFile>) invocation -> {
            BaseVirtualFileFake vf = invocation.getArgument(0);
            return vf.getRoot();
        });

        when(projectFileIndex.isInSourceContent(any())).thenAnswer((Answer<Boolean>) invocation -> {
            BaseVirtualFileFake vf = invocation.getArgument(0);
            return !vf.isTest();
        });

        when(projectFileIndex.isInTestSourceContent(any())).thenAnswer((Answer<Boolean>) invocation -> {
            BaseVirtualFileFake vf = invocation.getArgument(0);
            return vf.isTest();
        });

        virtualFileManagerStatic.when(VirtualFileManager::getInstance).thenReturn(virtualFileManager);
        virtualFileManagerStatic.when(()->VirtualFileManager.constructUrl(eq(LocalFileSystem.PROTOCOL),anyString())).thenAnswer((Answer<String>) invocation -> {
            String path = invocation.getArgument(1);
            VirtualFile file = BaseVirtualFileFake.findByPath(path);
            if (file == null) {
                return null;
            }
            return file.getPath();  // Not a real url, but it works for these tests

        });
        when(virtualFileManager.findFileByUrl(any())).thenAnswer((Answer<VirtualFile>) invocation -> {
            String url = invocation.getArgument(0);
            System.out.printf("findFileByUrl(%s)%n", url);
            return BaseVirtualFileFake.findByPath(url);
        });

        psiManagerStatic.when(() -> PsiManager.getInstance(any())).thenReturn(psiManager);

        javaDirectoryServiceStatic.when(JavaDirectoryService::getInstance).thenReturn(javaDirectoryService);
    }

    record Given(List<BaseVirtualFileFake> files) {
        record InputSet(List<BaseVirtualFileFake> files) {

            InputSet(List<BaseVirtualFileFake> files) {
                this.files = files;
                for (BaseVirtualFileFake vf : files) {
                    PsiClass psiClass = Mockito.mock(PsiClass.class);
                    when(psiClass.getName()).thenReturn(vf.getName());
                    PsiJavaFile psiJavaFile = Mockito.mock(PsiJavaFile.class);
                    when(psiJavaFile.getName()).thenReturn(vf.getName());
                    when(psiJavaFile.getPackageName()).thenReturn(vf.getQualifiedPackageName());
                    when(psiJavaFile.getClasses()).thenReturn(new PsiClass[]{psiClass});
                    when(psiManager.findFile(vf)).thenReturn(psiJavaFile);
                }
            }

            /**
             * Defines the files that are expected to result from transforming the 'input' in the context
             * of the 'given' files. Also verifies that these and only these files are the ones generated.
             *
             * @param src the expected source package files
             * @param tst the expected test package files
             */
            void expect(FileSet src, FileSet tst) {
                final List<VirtualFile> vfs = files.stream().map(f->(VirtualFile)f).toList();
                InputBundle bundle = new InputBundle();
                PackageWalker.read(project, vfs, bundle);
                bundle.asPath().get(InputBundle.Category::isSource).forEach(src::verify);
                bundle.asPath().get(InputBundle.Category::isTest).forEach(tst::verify);
                src.verifyEmpty();
                tst.verifyEmpty();
            }
        }

        /**
         * Defines the files that would be user-supplied inputs for a test.
         *
         * @param files as if input by a user
         * @return an object on which {@link InputSet#expect(FileSet, FileSet)} can be called
         */
        InputSet input(BaseVirtualFileFake... files) {
            return new InputSet(Arrays.asList(files));
        }
    }

    /**
     * Defines the files, along with their ancestors, that 'exist' for a test.
     *
     * @param files that will be set to exist
     * @return an object on which {@link Given#input(BaseVirtualFileFake...)} can be called
     */
    Given given(BaseVirtualFileFake...files) {
        BaseVirtualFileFake.setExists(files);
        return new Given(Arrays.asList(files));
    }

    record FileSet(String loc, List<BaseVirtualFileFake> files) {
        void verify(String pathToMatch) {
            System.out.printf("verify(%s)%n", pathToMatch);
            for (int i = 0; i < files.size(); i++) {
                BaseVirtualFileFake next = files.get(i);
                System.out.printf(" %d) name=%s, nextPath=%s, path=%s%n", i, next.getName(), next.getRelativePath(), pathToMatch);
                if (next.getRelativePath().equals(pathToMatch)) {
                    files.remove(i);
                    return;
                }
            }
            fail("File not expected: " + pathToMatch);
        }
        void verifyEmpty() {
            assertTrue(files.isEmpty(), "Missed " + loc + " files: " + files.stream().map(BaseVirtualFileFake::getPath).toList());
        }
    }

    FileSet src(BaseVirtualFileFake...files) {
        return new FileSet("src", new ArrayList<>(Arrays.asList(files)));
    }

    FileSet tst(BaseVirtualFileFake...files) {
        return new FileSet("test", new ArrayList<>(Arrays.asList(files)));
    }

    static final VirtualSrcPkgFake P = new VirtualSrcPkgFake(3);
    static final VirtualTestPkgFake T = new VirtualTestPkgFake(3);

    @Test
    void testFakes() {
        VirtualClassFake fake = P.p1.p2.j;
        assertFalse(fake.isTest());
        assertEquals(BaseVirtualFileFake.BASE_PKG + "src/main/java/F/p1/p2/j.java", fake.getPath());
        assertEquals("j.java", fake.getName());
        assertEquals("F.p1.p2", fake.getQualifiedPackageName());
        assertEquals("F.p1.p2.j", fake.getQualifiedPath());
    }

    @Test
    void singleSrcFileIncludesItsTest() {
        given(P.p1.p2.j, T.p1.p2.jtest).input(P.p1.p2.j, T.p1.p2.jtest).expect(src(P.p1.p2.j), tst(T.p1.p2.jtest));
    }

    @Test
    void altScFileIncludesItsTest() {
        given(P.p1.p2.j, T.p1.p2.testj).input(P.p1.p2.j, T.p1.p2.testj).expect(src(P.p1.p2.j), tst(T.p1.p2.testj));
    }

    @Test
    void singleSrcFileWithoutItsTest() {
        given(P.p1.p2.j, T.p1.p2.jtest).input(P.p1.p2.j).expect(src(P.p1.p2.j), tst(T.p1.p2.jtest));
    }

    @Test
    void singleTestFileWithoutItsSrc() {
        given(T.p1.p2.jtest,P.p1.p2.j).input(T.p1.p2.jtest).expect(src(P.p1.p2.j), tst(T.p1.p2.jtest));
    }

    @Test
    void singleTestFileWithOtherSrc() {
        given(T.p1.p2.jtest,P.p1.j).input(T.p1.p2.jtest,P.p1.j).expect(src(P.p1.j), tst(T.p1.p2.jtest));
    }

    @Test
    void singleSrcPkgIncludesItsTest() {
        given(P.p1.p2, T.p1.p2).input(P.p1.p2, T.p1.p2).expect(src(P.p1.p2), tst(T.p1.p2));
    }

    @Test
    void singlePkgWithoutItsTest() {
        given(P.p1.p2, T.p1.p2).input(P.p1.p2).expect(src(P.p1.p2), tst(T.p1.p2));
    }

    @Test
    void srcFileAndItsPackage() {
        given(P.p1.p2.j, T.p1.p2).input(P.p1.p2.j, P.p1.p2).expect(src(P.p1.p2), tst(T.p1.p2));
    }

    @Test
    void srcFileAndItsAncestor() {
        given(P.p1.p2.j).input(P.p1).expect(src(P.p1), tst());
    }

    @Test
    void oneCommonAncestor() {
        BaseVirtualFileFake[] files = {P.p1.p2.j, P.p1, P.p1.p2, T.p1.p2};
        given(files).input(files).expect(src(P.p1), tst(T.p1));
    }
}