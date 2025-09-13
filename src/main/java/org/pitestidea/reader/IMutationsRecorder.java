package org.pitestidea.reader;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.MutationImpact;
import org.pitestidea.toolwindow.DisplayChoices;

public interface IMutationsRecorder {
    /**
     * Record a single mutation in a file.
     *
     * @param pkg         of file
     * @param file        reference
     * @param methodName  name of method containing mutation
     * @param impact      mutation
     * @param lineNumber  where occurred
     * @param description of mutation
     */
    void record(String pkg, VirtualFile file, String methodName, MutationImpact impact, int lineNumber, String description);

    /**
     * Called after all calls to {@link #record(String, VirtualFile, String, MutationImpact, int, String)} have completed.
     */
    void postProcess();

    /**
     * Changes the sorting order.
     *
     * @param choices how results should be returned
     */
    void sort(DisplayChoices choices);
}
