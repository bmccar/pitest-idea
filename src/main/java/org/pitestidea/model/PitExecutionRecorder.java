package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.reader.ICoverageRecorder;

import java.util.Map;

/**
 * Records the output of a single execution of PITest.
 */
public class PitExecutionRecorder implements ICoverageRecorder {
    private final Map<VirtualFile,FileMutations> fileMutationsMap = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void record(VirtualFile file, CoverageImpact impact, int lineNumber, String description) {
        FileMutations fileMutations = fileMutationsMap.computeIfAbsent(file, _k -> new FileMutations());
        fileMutations.add(lineNumber, new Mutation(impact, description));
    }

    public interface FileVisitor {
        void visit(VirtualFile file, FileMutations fileMutations);
    }

    public void visit(FileVisitor visitor) {
        fileMutationsMap.forEach(visitor::visit);
    }
}
