package org.pitestidea.reader;

import org.pitestidea.model.CoverageImpact;

public interface ICoverageRecorder {
    void record(String filePath, CoverageImpact impact, int lineNumber, String description);
}
