package org.pitestidea.render;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.model.PitExecutionRecorder;

public interface ICoverageRenderer {
    void render(Project project, PitExecutionRecorder recorder);
}