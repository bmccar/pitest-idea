package org.pitestidea.model;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

import java.io.File;
import java.util.*;

public class PitRepo {
    public static final String PIT_STANDARD_REPORTS_DIR = "pit-reports";
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

        void remove(CachedRun run) {
            runHistory.remove(run);
            if (run == current) {
                current = null;
            }
        }
    }

    private static final Map<String, ProjectRunRecords> projectMap = new HashMap<>();

    public static void clear(@NotNull Project project) {
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        if (runRecords != null) {
            runRecords.runHistory.clear();
            runRecords.current = null;
        }
    }

    public static @Nullable CachedRun getCurrent(@NotNull Project project) {
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        if (runRecords != null && runRecords.current != null) {
            return runRecords.current;
        }
        return null;
    }

    public static void ensureSorted(@NotNull Project project) {
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        if (runRecords != null) {
            Comparator<CachedRun> comparator = Comparator.comparing(c->c.getExecutionRecord().getStartedAt());
            comparator = comparator.reversed();
            runRecords.runHistory.sort(comparator);
        }
    }

    /**
     * Creates a CachedRun with {@link #register(Module, ExecutionRecord, String)}, but first ensures
     * that the report directory exists.
     *
     * @param module where run files exist
     * @param record inputs and such
     * @return a new CachedRun
     */
    public static @NotNull CachedRun register(@NotNull Module module, @NotNull ExecutionRecord record) {
        /* TODO rm comment
        AtomicReference<VirtualFile> ref = new AtomicReference<>();
        ExecutionUtils.dumpThreads("PitRepo.register OUTER");
        ApplicationManager.getApplication().invokeAndWait(() -> {
            ExecutionUtils.dumpThreads("PitRepo.register INNER");
            try {
                WriteAction.run(()-> {
                    ref.set(ensureReportDirectoryCreated(module, record));
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
         */
        String path = IdeaDiscovery.getAbsoluteOutputPath(module,PitRepo.PIT_IDEA_REPORTS_DIR, record.getReportDirectoryName());
        return register(module, record, path);
    }

    /**
     * Creates a CachedRun that is properly linked to/from the project record managed here in PitRepo.
     *
     * @param module    where run files exist
     * @param record    inputs and such
     * @param reportDir where report data will be written to
     * @return a new CachedRun
     */
    public static @NotNull CachedRun register(@NotNull Module module, @NotNull ExecutionRecord record, @NotNull String reportDir) {
        PitExecutionRecorder recorder = new PitExecutionRecorder(module);
        Project project = recorder.getModule().getProject();
        ProjectRunRecords runRecords = projectMap.computeIfAbsent(project.getName(), _x -> new ProjectRunRecords());
        CachedRun cachedRun = new CachedRun(runRecords, record, recorder, reportDir);
        if (cachedRun.equals(runRecords.current)) {
            // While swapping in a new CachedRun, ensure the replacement is current if the original was
            runRecords.setAsCurrent(cachedRun);
        }
        CachedRun old = runRecords.runHistory.stream().filter(r -> r.getExecutionRecord().equals(record)).findFirst().orElse(null);
        if (old != null) {
            cachedRun.setRunStateChangedListener(old.getRunStateChangedListener());
            runRecords.runHistory.remove(old);
        }
        runRecords.runHistory.addFirst(cachedRun);
        return cachedRun;
    }

    /* *
     * Ensures that the report directory exists and return it. Although PIT itself would create that directory if
     * needed, it is cleaner to set it before PIT execution, and in order to create a VirtualFile for it, that
     * directory must exist. Because this method involves creating directory, it must be called in a readAction
     * in accordance with IJ framework.
     *
     * @param module where report dir will be created
     * @param record to get report name from
     * @return existing or newly-created report directory
     * @throws IOException unexpected
     *\/
    @NotNull
    private static VirtualFile ensureReportDirectoryCreated(Module module, ExecutionRecord record) throws IOException {
        @Nullable VirtualFile vf = CompilerPaths.getModuleOutputDirectory(module, false);
        if (vf == null) {
            throw new RuntimeException("No output directory for module " + module.getName());
        } else {
            vf = vf.getParent(); // Don't want the "classes" child
            if (record.getInputFiles().isEmpty()) {
                vf = vf.findChild(PitRepo.PIT_STANDARD_REPORTS_DIR);
                if (vf == null) {
                    throw new RuntimeException("No reports directory for module " + module.getName());
                }
            } else {
                VirtualFile pitIdeaDir = vf.findChild(PitRepo.PIT_IDEA_REPORTS_DIR);
                System.out.println("  PitIdeaDir-found? " + pitIdeaDir);
                if (pitIdeaDir == null) {
                    pitIdeaDir = vf.createChildDirectory(null, PitRepo.PIT_IDEA_REPORTS_DIR);
                    System.out.println("  PitIdeaDir-created " + pitIdeaDir);
                }
                String rn = record.getReportDirectoryName();
                vf = pitIdeaDir.findChild(rn);
                System.out.println("  vf-found? " + vf);
                if (vf != null) {
                    vf.refresh(false, true);  // Make sure it's really there
                    vf = pitIdeaDir.findChild(rn);
                }

                if (vf == null) {
                    vf = pitIdeaDir.createChildDirectory(null, rn);
                    System.out.println("  vf-created " + vf);
                }
            }
        }
        return vf;
    }
     */


    public interface IHistory {
        void visit(CachedRun run, boolean isCurrent);
    }

    public static void apply(Project project, IHistory history) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        if (runs != null) {
            runs.runHistory.forEach(r -> history.visit(r, r == runs.current));
        }
    }

    public static void deleteHistory(Project project) {
        ProjectRunRecords runRecords = projectMap.get(project.getName());
        List<CachedRun> cachedRunsToDelete = new ArrayList<>(runRecords.runHistory);
        cachedRunsToDelete.forEach(CachedRun::deleteFilesForThisRun);
        runRecords.runHistory.clear();
        runRecords.current = null;
    }

    public static PitExecutionRecorder get(Project project) {
        ProjectRunRecords runs = projectMap.get(project.getName());
        return (runs == null || runs.current == null) ? null : runs.current.ensureLoaded();
    }

    /**
     * Reloads all reports that can be found in the output directory, including CLI generated reports.
     *
     * @param project to update
     */
    public static void reloadReports(Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            File pitIdeaDir = IdeaDiscovery.getAbsoluteOutputDir(module, PitRepo.PIT_IDEA_REPORTS_DIR);
            if (pitIdeaDir != null && pitIdeaDir.exists()) {
                File[] reports = pitIdeaDir.listFiles();
                if (reports != null) {
                    for (File report : reports) {
                        loadReport(module, report);
                    }
                }
            }
            File standardIdeaDir = IdeaDiscovery.getAbsoluteOutputDir(module, PitRepo.PIT_STANDARD_REPORTS_DIR);
            if (standardIdeaDir != null && standardIdeaDir.exists()) {
                long startedAt = standardIdeaDir.lastModified();
                CachedRun cachedRun = PitRepo.register(module, new ExecutionRecord(startedAt), standardIdeaDir.getPath());
                cachedRun.reload();
            }
            ensureSorted(project);
        }
        MutationControlPanel mutationControlPanel = PitToolWindowFactory.getControlPanel(project);
        mutationControlPanel.reloadReports(project);
    }

    private static void loadReport(Module module, File report) {
        if (report.isDirectory()) {
            try {
                CachedRun cachedRun = PitRepo.register(module, new ExecutionRecord(report), report.getPath());
                cachedRun.reload();
            } catch (ExecutionRecord.InvalidFile _e) {
                // Report directories generated from a previous failed/cancelled PIT may exist but
                // easiest to just ignore them as the utility of loading them is low and they'll
                // get removed anyway on the next project clean.
            }
        }
    }
}
