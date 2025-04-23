package org.pitestidea.model;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.render.CoverageGutterRenderer;
import org.pitestidea.render.FileOpenCloseListener;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Pairs an ExecutionRecord with a PitExecutionRecorder, with the ability to lazy
 * load the latter (which can grow large) or drop it save space.
 */
public class CachedRun implements Comparable<CachedRun> {
    public static String MUTATIONS_FILE = "mutations.xml";

    private final PitRepo.ProjectRunRecords runRecords;
    private final ExecutionRecord executionRecord;
    private PitExecutionRecorder recorder = null;
    private RunState runState;
    private boolean includesPackages = false;

    public CachedRun(PitRepo.ProjectRunRecords runRecords, PitExecutionRecorder recorder, RunState runState) {
        this.runRecords = runRecords;
        this.runState = runState;
        this.recorder = recorder;
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

    public boolean isIncludesPackages() {
        return includesPackages;
    }

    public void setIncludesPackages() {
        this.includesPackages = true;
    }

    public PitExecutionRecorder getRecorder() {
        return recorder;
    }

    public Project getProject() {
        return recorder.getModule().getProject(); // TODO may be null
    }

    public PitExecutionRecorder ensureLoaded() {
        // TODO load if not loaded!
        return recorder;
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
        PitToolWindowFactory.show(project, this, includesPackages);
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

    private OSProcessHandler processHandler;

    public void setProcessHandler(OSProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public String getReportDir() {
        Module module = recorder.getModule();
        return PitRepo.getReportBaseDirectory(module) + '/' + executionRecord.getReportDirectoryName();
    }

    public boolean cancel() {
        if (this.runState == RunState.RUNNING && processHandler != null) {
            processHandler.destroyProcess();
            this.runState = RunState.CANCELLED;
            return true;
        }
        return false;
    }

    /**
     * Deletes all files for this run and any references to this object. This should be called based on a user
     * request to permanently delete a report.
     */
    public void deleteFilesForThisRun() {
        prepareForRun();
        recorder = null;
        runRecords.remove(this);
    }

    /**
     * Deletes all files that PIT generated for this run. For safety, extra checks are made to ensure that the right
     * thing is being deleted.
     */
    public void prepareForRun() {
        String reportDirectory = getReportDir();
        File dir = new File(reportDirectory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && Arrays.stream(files).anyMatch(f->f.getName().equals(MUTATIONS_FILE))) {
                deleteFilesInDir(dir);
            }
        }
    }

    private void deleteFilesInDir(File dir) {
        // First, extra safety/sanity checks just to be sure we're only deleting within the expected directory
        if (dir.exists() && dir.isDirectory() && dir.getAbsolutePath().contains(PitRepo.PIT_IDEA_REPORTS_DIR)) {
            File[] files = dir.listFiles();
            boolean all = true;
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFilesInDir(file);
                    } else {
                        if (!file.delete()) {
                            all = false;
                        }
                    }
                }
            }
            if (all) {
                if (!dir.delete()) {
                    throw new RuntimeException("Could not delete directory " + dir);
                }
            }
        }
    }
}
