package org.pitestidea.model;

public class CachedRun {
    private final ExecutionRecord executionRecord;
    private PitExecutionRecorder pitExecutionRecorder = null;

    CachedRun(ExecutionRecord executionRecord) {
        this.executionRecord = executionRecord;
    }

    public ExecutionRecord getExecutionRecord() {
        return executionRecord;
    }

    public PitExecutionRecorder ensureLoaded() {
        // TODO load if not loaded!
        return pitExecutionRecorder;
    }

    void setPitExecutionRecorder(PitExecutionRecorder pitExecutionRecorder) {
        this.pitExecutionRecorder = pitExecutionRecorder;
    }
}
