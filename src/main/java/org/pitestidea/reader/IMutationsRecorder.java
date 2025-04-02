package org.pitestidea.reader;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.MutationImpact;

public interface IMutationsRecorder {
    void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description);
}
