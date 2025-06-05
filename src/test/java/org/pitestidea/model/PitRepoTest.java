package org.pitestidea.model;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.intellij.openapi.module.Module;
import org.pitestidea.configuration.IdeaDiscovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PitRepoTest {

    private Module commonModule;
    private static final MockedStatic<CompilerPaths> compilerPaths = Mockito.mockStatic(CompilerPaths.class);
    private static final MockedStatic<IdeaDiscovery> discoveryMockedStatic = Mockito.mockStatic(IdeaDiscovery.class);

    @BeforeAll
    static void beforeAll() {
        discoveryMockedStatic.when(()->IdeaDiscovery.getModuleOutputDirectory(any())).thenReturn("someOutputDir");
        discoveryMockedStatic.when(()->IdeaDiscovery.getAbsoluteOutputPath(any(),any(),any())).thenReturn("someAbsoluteOutputPath");
    }

    @AfterAll
    static void afterAll() {
        compilerPaths.close();
        discoveryMockedStatic.close();
    }

    @BeforeEach
    void setUp() {
        Project project = Mockito.mock(Project.class);
        commonModule = Mockito.mock(Module.class);
        when(commonModule.getProject()).thenReturn(project);

        VirtualFile vf = Mockito.mock(VirtualFile.class);
        VirtualFile pf = Mockito.mock(VirtualFile.class);
        when(vf.getParent()).thenReturn(pf);
        when(pf.getPath()).thenReturn("someFile");

        compilerPaths.when(() -> CompilerPaths.getModuleOutputDirectory(commonModule, false)).thenReturn(vf);

        PitRepo.clear(commonModule.getProject());
    }

    private @NotNull CachedRun genRecorder(String... inputs) {
        try {
            Thread.sleep(5);  // So the timestamps are different
        } catch (InterruptedException e) {
            // Do nothing
        }
        InputBundle bundle = new InputBundle();
        Arrays.stream(inputs).forEach(s->bundle.addPath(InputBundle.Category.SOURCE_PKG, s));
        return PitRepo.register(commonModule, new ExecutionRecord(bundle));
    }

    private void verify(Module module, CachedRun... recorders) {
        List<ExecutionRecord> gotRecords = new ArrayList<>();

        PitRepo.apply(module.getProject(), (c,_h) -> gotRecords.add(c.getExecutionRecord()));

        List<ExecutionRecord> exp = Arrays.stream(recorders).map(CachedRun::getExecutionRecord).toList();

        assertEquals(exp, gotRecords);
    }

    @Test
    void twoRecords() {
        CachedRun ab = genRecorder("a", "b");
        verify(ab.getRecorder().getModule(), ab);

        CachedRun cd = genRecorder("c", "d");
        verify(cd.getRecorder().getModule(), cd, ab);
    }

    @Test
    void overwriteOne() {
        CachedRun ab1 = genRecorder("a", "b");
        verify(ab1.getRecorder().getModule(), ab1);

        CachedRun ab2 = genRecorder("a", "b");
        verify(ab2.getRecorder().getModule(), ab2);
    }

    @Test
    void sequence() {
        CachedRun ab = genRecorder("a", "b");
        verify(ab.getRecorder().getModule(), ab);

        CachedRun cd = genRecorder("c", "d");
        verify(cd.getRecorder().getModule(), cd, ab);

        ab = genRecorder("a", "b");
        verify(ab.getRecorder().getModule(), ab, cd);
    }
}