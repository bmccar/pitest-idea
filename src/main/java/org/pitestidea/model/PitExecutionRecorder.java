package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.reader.IMutationsRecorder;

import java.util.HashMap;
import java.util.Map;

/**
 * Records the output of a single execution of PITest and reorganizes individual lines into
 * a hierarchical directory/file structure.
 */
public class PitExecutionRecorder implements IMutationsRecorder {
    public static final String ROOT_PACKAGE_NAME = "Results";
    private final PkgGroup rootDirectory = new PkgGroup(ROOT_PACKAGE_NAME);

    public interface PackageDiver {
        void apply(FileVisitor visitor);
    }

    private interface Directory extends IMutationScore {
        void walkInternal(FileVisitor visitor);
    }

    private static class PkgGroup extends BaseMutationsScore implements Directory, PackageDiver {
        private final String name;
        private final Map<String, Directory> children = new HashMap<>();

        private PkgGroup(String name) {
            this.name = name;
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(name, this, this);
        }

        @Override
        public void apply(FileVisitor visitor) {
            children.values().forEach(g->g.walkInternal(visitor));
        }

        @Override
        public String toString() {
            return "Pkg:"+name+'/'+children.size();
        }
    }

    @VisibleForTesting
    static class FileGroup extends BaseMutationsScore implements Directory {
        private final VirtualFile file;
        private final FileMutations fileMutations;

        private FileGroup(VirtualFile file, String pkg) {
            this.file = file;
            this.fileMutations = new FileMutations(pkg);
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(file, fileMutations, this);
        }
    }

    @Override
    public void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description) {
        rootDirectory.accountFor(impact);
        PkgGroup last = rootDirectory;
        for (String segment: pkg.split("\\.")) {
            Directory dir = last.children.computeIfAbsent(segment, _k -> new PkgGroup(segment));
            dir.accountFor(impact);
            last = (PkgGroup)dir;
        }
        FileGroup dir = (FileGroup)last.children.computeIfAbsent(file.getName(), _k -> new FileGroup(file, pkg));
        dir.fileMutations.add(lineNumber, new Mutation(impact, description));
        dir.accountFor(impact);
    }

    public interface FileVisitor {
        void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score);
        void visit(String pkg, PackageDiver diver, IMutationScore score);
    }

    public void visit(FileVisitor visitor) {
        rootDirectory.walkInternal(visitor);
    }
}
