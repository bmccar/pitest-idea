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
    private static class ProjectRunRecords {
        private final LinkedList<CachedRun> runHistory = new LinkedList<>();
        private CachedRun current;
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

    public static void register(CachedRun cachedRun) {
        PitExecutionRecorder recorder = cachedRun.getRecorder();
        Project project = recorder.getModule().getProject();
        ProjectRunRecords runRecords = projectMap.computeIfAbsent(project.getName(), _x -> new ProjectRunRecords());
        runRecords.runHistory.remove(cachedRun);
        runRecords.runHistory.addFirst(cachedRun);
        runRecords.current = cachedRun;
    }

    public interface IHistory {
        void visit(CachedRun run);
    }

    public static void apply(Project project, IHistory history) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        runs.runHistory.forEach(history::visit);
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
        return dir + "/pit-idea-reports";
    }
}
