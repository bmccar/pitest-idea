package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the outcome of PITest for a given file.
 */
public class FileMutations /*extends BaseMutationsScore*/ {
    private final VirtualFile file;
    private final String pkg;
    private final Map<Integer, List<Mutation>> lineMutations = new HashMap<>();
    private final FileMutations lastFileMutations;

    public FileMutations(String pkg, VirtualFile file, FileMutations lastFileMutations) {
        this.pkg = pkg;
        this.file = file;
        this.lastFileMutations = lastFileMutations;
    }

    public String getPkg() {
        return pkg;
    }

    public String getFileName() {
        return file.getName();
    }

    public VirtualFile getFile() {
        return file;
    }

    public void add(int lineNumber, Mutation mutation) {
        List<Mutation> mutations = lineMutations.computeIfAbsent(lineNumber, _x -> new ArrayList<>());
        mutations.add(mutation);
    }

    public interface LineVisitor {
        void visit(LineImpact lineImpact);
    }

    public void visit(LineVisitor visitor) {
        lineMutations.forEach((lineNumber, lines) -> {
            List<Mutation> lastMutations = lastFileMutations == null ? null : lastFileMutations.lineMutations.get(lineNumber);
            LineImpact lineImpact = new LineImpact(lineNumber, lines, lastMutations);
            visitor.visit(lineImpact);
        });
    }

    public List<Mutation> getLineMutations(String methodName) {
        List<Mutation> methodMutations = new ArrayList<>();
        for (List<Mutation> mutations : lineMutations.values()) {
            for (Mutation mutation: mutations) {
                if (mutation.method().equals(methodName)) {
                    methodMutations.add(mutation);
                }
            }
        }
        return methodMutations;
    }
}
