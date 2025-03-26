package org.pitestidea.render;

import com.intellij.openapi.project.Project;
import org.pitestidea.model.PitExecutionRecorder;

public interface ICoverageRenderer {
    void render(Project project, PitExecutionRecorder recorder);
}