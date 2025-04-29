package org.pitestidea.reader;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.MutationImpact;
import org.pitestidea.toolwindow.DisplayChoices;
import org.pitestidea.toolwindow.Sorting;

public interface IMutationsRecorder {
    /**
     * Record a single mutation in a file.
     *
     * @param pkg of file
     * @param file reference
     * @param impact mutation
     * @param lineNumber where occurred
     * @param description of mutation
     */
    void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description);

    /**
     * Called after all calls to {@link #record(String, VirtualFile, MutationImpact, int, String)} have completed.
     */
    void postProcess();

    /**
     * Changes the sorting order.
     *
     * @param choices how results should be returned
     */
    void sort(DisplayChoices choices);
}
