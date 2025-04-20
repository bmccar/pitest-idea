package org.pitestidea.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.FileOpenCloseListener;
import org.pitestidea.toolwindow.PitToolWindowFactory;

/**
 * Pairs an ExecutionRecord with a PitExecutionRecorder, with the ability to lazy
 * load the latter (which can grow large) or drop it save space.
 */
public class CachedRun implements Comparable<CachedRun> {
    private final PitRepo.ProjectRunRecords runRecords;
    private final ExecutionRecord executionRecord;
    private PitExecutionRecorder pitExecutionRecorder = null;
    private long timestamp; // TODO move to ExecutionRecord
    private RunState runState;
    private boolean includesPackages = false;

    public CachedRun(PitRepo.ProjectRunRecords runRecords, PitExecutionRecorder recorder, RunState runState) {
        this.runRecords = runRecords;
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

    public boolean isIncludesPackages() {
        return includesPackages;
    }

    public void setIncludesPackages() {
        this.includesPackages = true;
    }

    public PitExecutionRecorder getRecorder() {
        return pitExecutionRecorder;
    }

    public Project getProject() {
        return pitExecutionRecorder.getModule().getProject(); // TODO may be null
    }

    public PitExecutionRecorder ensureLoaded() {
        // TODO load if not loaded!
        return pitExecutionRecorder;
    }

    void setPitExecutionRecorder(PitExecutionRecorder pitExecutionRecorder) {
        this.pitExecutionRecorder = pitExecutionRecorder;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Makes this CachedRun the currently-selected item in its history list, and performs all necessary
     * UI updates to reflect this new selection.
     */
    public void activate() {
        Project project = getProject();
        PitExecutionRecorder recorder = getRecorder();
        setAsCurrent();
        CoverageGutterRenderer.removeGutterIcons(project);
        FileOpenCloseListener.replayOpenFiles(project);
        PitToolWindowFactory.show(project, recorder, includesPackages);
    }


    @Override
    public int compareTo(@NotNull CachedRun that) {
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

    public void setAsCurrent() {
        runRecords.setAsCurrent(this);
    }

    public boolean isCurrent() {
        return runRecords.isCurrent(this);
    }

    public boolean isAlone() {
        return runRecords.getSize()==1;
    }
}
