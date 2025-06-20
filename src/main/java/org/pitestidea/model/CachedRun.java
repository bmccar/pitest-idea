package org.pitestidea.model;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.reader.InvalidMutatedFileException;
import org.pitestidea.reader.MutationsFileReader;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * Pairs an ExecutionRecord with a PitExecutionRecorder, with the ability to lazily
 * load the latter (which can grow large) or drop it to save space. An instance of this
 * class is created for every PIT output directory in the project.
 */
public class CachedRun implements Comparable<CachedRun> {
    private static final String MUTATIONS_FILE = "mutations.xml";

    // Back ptr to the owner of this object
    private final PitRepo.ProjectRunRecords runRecords;

    // Holds inputs and timings for this run
    private final ExecutionRecord executionRecord;

    // Loaded from mutations.xml -- may be unloaded as well to reduce space usage
    private PitExecutionRecorder recorder;

    // State of the run that produced this object
    private RunState runState = RunState.COMPLETED;

    // Optional callback for when runState changes
    private BiConsumer<RunState, RunState> runStateChangedListener;

    // Directory may or may not exist -- store the String path rather than File to avoid race-deletion headaches
    private final @NotNull String reportDirectory;

    // For later display of classpath from console
    private ClassPaths classPaths = null;

    public CachedRun(PitRepo.ProjectRunRecords runRecords, ExecutionRecord record, PitExecutionRecorder recorder, @NotNull String reportDirectory) {
        this.runRecords = runRecords;
        this.recorder = recorder;
        this.executionRecord = record;
        this.reportDirectory = reportDirectory;
    }

    public synchronized RunState getRunState() {
        return runState;
    }

    public synchronized void setRunState(RunState newRunState) {
        if (this.runState != newRunState) {
            RunState oldState = this.runState;
            this.runState = newRunState;
            if (runStateChangedListener != null) {
                runStateChangedListener.accept(oldState, newRunState);
            }
        }
    }

    public BiConsumer<RunState, RunState> getRunStateChangedListener() {
        return runStateChangedListener;
    }

    public void setRunStateChangedListener(BiConsumer<RunState, RunState> runStateChangedListener) {
        this.runStateChangedListener = runStateChangedListener;
    }

    public ExecutionRecord getExecutionRecord() {
        return executionRecord;
    }

    public void setClassPath(ClassPaths classPaths) {
        this.classPaths = classPaths;
    }

    public ClassPaths getClassPaths() {
        return classPaths;
    }

    public PitExecutionRecorder getRecorder() {
        return recorder;
    }

    public @Nullable Project getProject() {
        return recorder.getModule().getProject();
    }

    public synchronized PitExecutionRecorder ensureLoaded() {
        // TODO -- Placeholder for later work on lazy loading
        return recorder;
    }

    /**
     * Makes this CachedRun the currently selected item in its history list and performs all necessary
     * UI updates to reflect this new selection.
     */
    public synchronized void activate() {
        Project project = getProject();
        setAsCurrent();
        PitToolWindowFactory.show(project, this);
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

    /**
     * Makes this CachedRun the current one, taking the place of an existing current if there is one.
     */
    public void setAsCurrent() {
        runRecords.setAsCurrent(this);
    }

    /**
     * At most one CachedRun is 'current' with respect to a project. This is for display purposes.
     *
     * @return true iff this CachedRun is the current one inside its project
     */
    public boolean isCurrent() {
        return runRecords.isCurrent(this);
    }

    private OSProcessHandler processHandler;

    public void setProcessHandler(OSProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public String getReportDir() {
        return reportDirectory;
    }

    public File getReportFileDir() {
        return new File(reportDirectory);
    }

    public File getMutationsFile() {
        return new File(getReportFileDir(), MUTATIONS_FILE);
    }

    public boolean cancel() {
        if (this.runState == RunState.RUNNING && processHandler != null) {
            processHandler.destroyProcess();
            setRunState(RunState.CANCELLED);
            return true;
        }
        return false;
    }

    public void reload() throws InvalidMutatedFileException {
        MutationsFileReader.read(getProject(), getMutationsFile(), recorder);
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
     * thing is being deleted. This does *not* delete any PIT files not generated within this plugin, e.g., those created
     * by command-line PIT runs.
     */
    public void prepareForRun() {

        File dir = getReportFileDir();
        if (dir.exists() && dir.isDirectory()) {
            deleteFilesInDir(dir);
        }
    }

    // TODO find all cmd line
    private static @NotNull Future<File[]> collectValidReportDirectories(File dir) {
        final Application app = ApplicationManager.getApplication();
        return app.executeOnPooledThread(() -> app.runReadAction((Computable<File[]>) () -> {
            File[] files = dir.listFiles();
            if (files != null && Arrays.stream(files).anyMatch(f -> f.getName().equals(MUTATIONS_FILE))) {
                return files;
            }
            return null;
        }));
    }

    private void deleteFilesInDir(File dir) {
        // First, extra safety/sanity checks just to be sure we're only doing deletes within the expected directory
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
