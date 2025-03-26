package org.pitestidea.model;

import org.pitestidea.reader.ICoverageRecorder;

import java.util.Map;

/**
 * Records the output of a single execution of PITest.
 */
public class PitExecutionRecorder implements ICoverageRecorder {
    private final Map<String,FileMutations> fileMutationsMap = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void record(String filePath, CoverageImpact impact, int lineNumber, String description) {
        FileMutations fileMutations = fileMutationsMap.computeIfAbsent(filePath, _k -> new FileMutations());
        fileMutations.add(lineNumber, new Mutation(impact, description));
    }

    public interface FileVisitor {
        void visit(String filePath, FileMutations fileMutations);
    }

    public void visit(FileVisitor visitor) {
        fileMutationsMap.forEach(visitor::visit);
    }
}
