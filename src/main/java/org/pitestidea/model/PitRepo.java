package org.pitestidea.model;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class PitRepo {
    public static final String PIT_IDEA_REPORTS_DIR = "pit-idea-reports";

    public static class ProjectRunRecords {
        private final LinkedList<CachedRun> runHistory = new LinkedList<>();
        private CachedRun current;
        void setAsCurrent(CachedRun run) {
            this.current = run;
        }
        boolean isCurrent(CachedRun run) {
            return this.current == run;
        }
        int getSize() {
            return runHistory.size();
        }
        void remove(CachedRun run) {
            runHistory.remove(run);
            if (run == current) {
                current = null;
            }
        }
    }
    private static final Map<String, ProjectRunRecords> projectMap = new HashMap<>();

    public static void clear(@NotNull Project project) {
        System.out.println("Clearing PitRepo for " + project.getName());
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        if (runRecords != null) {
            runRecords.runHistory.clear();
            runRecords.current = null;
        }
    }

    // TODO should call after bulk registers completed
    public static void ensureSorted(@NotNull Project project) {
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        if (runRecords != null) {
            Collections.sort(runRecords.runHistory);
        }
    }

    public static CachedRun register(Module module, List<String> inputs, RunState state) {
        PitExecutionRecorder recorder = new PitExecutionRecorder(module,new ExecutionRecord(inputs));
        Project project = recorder.getModule().getProject();
        ProjectRunRecords runRecords = projectMap.computeIfAbsent(project.getName(), _x -> new ProjectRunRecords());
        CachedRun cachedRun = new CachedRun(runRecords, recorder, RunState.COMPLETED);
        if (cachedRun.equals(runRecords.current)) {
            // While swapping in a new CachedRun, ensure the replacement is current if the original was
            runRecords.setAsCurrent(cachedRun);
        }
        runRecords.runHistory.remove(cachedRun);
        runRecords.runHistory.addFirst(cachedRun);
        return cachedRun;
    }

    public interface IHistory {
        void visit(CachedRun run, boolean isCurrent);
    }

    public static void apply(Project project, IHistory history) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        runs.runHistory.forEach(r->history.visit(r,r==runs.current));
    }

    public static void deleteHistory(Project project) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        runs.runHistory.forEach(CachedRun::deleteFilesForThisRun);
        runs.runHistory.clear();
        runs.current = null;
    }

    public static PitExecutionRecorder get(Project project) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        return runs==null ? null : runs.current.ensureLoaded();
    }

    public static String getReportBaseDirectory(Module module) {

        String dir;

        @Nullable VirtualFile moduleOutputDir = CompilerPaths.getModuleOutputDirectory(module, false);
        if (moduleOutputDir != null) {
            dir = moduleOutputDir.getParent().getCanonicalPath();
        } else {
            dir = CompilerPaths.getOutputPaths(new Module[]{module})[0];
            int ix = dir.lastIndexOf(File.separator);
            if (ix > 0) {
                dir = dir.substring(0, ix);
            }
        }
        return dir + '/' + PIT_IDEA_REPORTS_DIR;
    }
}
