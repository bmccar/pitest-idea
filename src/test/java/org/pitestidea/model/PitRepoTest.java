package org.pitestidea.model;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.intellij.openapi.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PitRepoTest {

    private Module commonModule;

    @BeforeEach
    void setUp() {
        Project project = Mockito.mock(Project.class);
        commonModule = Mockito.mock(Module.class);
        when(commonModule.getProject()).thenReturn(project);
        PitRepo.clear(commonModule.getProject());
    }

    private PitExecutionRecorder genRecorder(String... inputs) {
        List<String> ab = Arrays.asList(inputs);
        try {
            Thread.sleep(5);  // So the timestamps are different
        } catch (InterruptedException e) {
        }
        PitExecutionRecorder recorder =  new PitExecutionRecorder(commonModule, new ExecutionRecord(ab));
        //CachedRun cachedRun = new CachedRun(recorder, RunState.COMPLETED);
        //PitRepo.register(cachedRun);
        PitRepo.register(commonModule, Arrays.asList(inputs), RunState.COMPLETED);
        return recorder;
    }

    private void verify(Module module, PitExecutionRecorder... recorders) {
        List<ExecutionRecord> gotRecords = new ArrayList<>();
        List<CachedRun> cachedRuns = new ArrayList<>();

        PitRepo.apply(module.getProject(), (c,_h) -> gotRecords.add(c.getExecutionRecord()));

        List<ExecutionRecord> exp = Arrays.stream(recorders).map(PitExecutionRecorder::getExecutionRecord).toList();

        assertEquals(exp, gotRecords);
    }

    @Test
    void twoRecords() {
        PitExecutionRecorder ab = genRecorder("a", "b");
        verify(ab.getModule(), ab);

        PitExecutionRecorder cd = genRecorder("c", "d");
        verify(cd.getModule(), cd, ab);
    }

    @Test
    void overwriteOne() {
        PitExecutionRecorder ab1 = genRecorder("a", "b");
        verify(ab1.getModule(), ab1);

        PitExecutionRecorder ab2 = genRecorder("a", "b");
        verify(ab2.getModule(), ab2);
    }

    @Test
    void sequence() {
        PitExecutionRecorder ab = genRecorder("a", "b");
        verify(ab.getModule(), ab);

        PitExecutionRecorder cd = genRecorder("c", "d");
        verify(cd.getModule(), cd, ab);

        ab = genRecorder("a", "b");
        verify(ab.getModule(), ab, cd);
    }
}