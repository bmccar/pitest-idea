package org.pitestidea.reader;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.CoverageImpact;

public interface ICoverageRecorder {
    void record(VirtualFile file, CoverageImpact impact, int lineNumber, String description);
}
