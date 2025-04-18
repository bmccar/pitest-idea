package org.pitestidea.model;

import org.jetbrains.annotations.NotNull;

/**
 * Pairs an ExecutionRecord with a PitExecutionRecorder, with the ability to lazy
 * load the latter (which can grow large) or drop it save space.
 */
public class CachedRun implements Comparable<CachedRun> {
    private final ExecutionRecord executionRecord;
    private PitExecutionRecorder pitExecutionRecorder = null;
    private long timestamp; // TODO move to ExecutionRecord
    private RunState runState;

    public CachedRun(PitExecutionRecorder recorder, RunState runState) {
        this.runState = runState;
        this.pitExecutionRecorder = recorder;
        this.executionRecord = recorder.getExecutionRecord();
    }

    public RunState getRunState() {
        return runState;
    }

    public void setRunState(RunState runState) {
        this.runState = runState;
    }

    public ExecutionRecord getExecutionRecord() {
        return executionRecord;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PitExecutionRecorder getRecorder() {
        return pitExecutionRecorder;
    }

    public PitExecutionRecorder ensureLoaded() {
        // TODO load if not loaded!
        return pitExecutionRecorder;
    }

    void setPitExecutionRecorder(PitExecutionRecorder pitExecutionRecorder) {
        this.pitExecutionRecorder = pitExecutionRecorder;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(@NotNull CachedRun that) {
        //System.out.println("Comparing " + this.executionRecord + " to " + that.executionRecord + " ==> " + executionRecord.compareTo(that.executionRecord));
        return executionRecord.compareTo(that.executionRecord);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CachedRun) {
            return this.executionRecord.equals(((CachedRun) obj).executionRecord);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return executionRecord.hashCode();
    }
}
