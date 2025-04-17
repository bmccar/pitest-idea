package org.pitestidea.model;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PitRepo {
    private static class ProjectRuns {
        private final Map<String,CachedRun> projectRuns = new HashMap<>();
        private CachedRun current;
    }
    private static final Map<String, ProjectRuns> runMap = new HashMap<>();

    public static void register(PitExecutionRecorder recorder) {
        Project project = recorder.getModule().getProject();
        ProjectRuns runs = runMap.computeIfAbsent(project.getName(), _x -> new ProjectRuns());
        ExecutionRecord record = recorder.getExecutionRecord();
        CachedRun run = runs.projectRuns.computeIfAbsent(record.getReportDirectoryName(), _x->
                new CachedRun(recorder.getExecutionRecord()));
        run.setPitExecutionRecorder(recorder);
        runs.current = run;

        //String dir = getReportBaseDirectory(recorder.getModule());
    }

    public interface IHistory {
        void visit(CachedRun run);
    }

    public static void apply(Project project, IHistory history) {
        ProjectRuns runs = runMap.get(project.getName());
        runs.projectRuns.values().forEach(history::visit);
    }

    public static PitExecutionRecorder get(Project project) {
        ProjectRuns runs = runMap.get(project.getName());
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
