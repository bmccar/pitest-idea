package org.pitestidea.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.reader.IMutationsRecorder;

import java.util.HashMap;
import java.util.Map;

/**
 * Records the output of a single execution of PITest and reorganizes individual lines into
 * a hierarchical directory/file structure.
 */
public class PitExecutionRecorder implements IMutationsRecorder {
    public static final String ROOT_PACKAGE_NAME = "ROOT";
    private final PkgGroup rootDirectory = new PkgGroup(ROOT_PACKAGE_NAME);

    public interface PackageDiver {
        void apply(FileVisitor visitor);
    }

    private interface Directory {
        void walkInternal(FileVisitor visitor);
    }

    private static class PkgGroup implements Directory, PackageDiver {
        private final String name;
        private final Map<String, Directory> children = new HashMap<>();

        private PkgGroup(String name) {
            this.name = name;
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(name, this);
        }

        @Override
        public void apply(FileVisitor visitor) {
            children.values().forEach(g->g.walkInternal(visitor));;
        }

        @Override
        public String toString() {
            return "Pkg:"+name+'/'+children.size();
        }
    }

    private static class FileGroup implements Directory {
        private final VirtualFile file;
        private final FileMutations fileMutations;

        private FileGroup(VirtualFile file, String pkg) {
            this.file = file;
            this.fileMutations = new FileMutations(pkg);
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(file, fileMutations);
        }
    }

    @Override
    public void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description) {
        PkgGroup last = rootDirectory;
        for (String segment: pkg.split("\\.")) {
            Directory dir = last.children.computeIfAbsent(segment, _k -> new PkgGroup(segment));
            last = (PkgGroup)dir;
        }
        FileGroup dir = (FileGroup)last.children.computeIfAbsent(file.getName(), _k -> new FileGroup(file, pkg));
        dir.fileMutations.add(lineNumber, new Mutation(impact, description));
    }

    public interface FileVisitor {
        void visit(VirtualFile file, FileMutations fileMutations);
        void visit(String pkg, PackageDiver diver);
    }

    public void visit(FileVisitor visitor) {
        rootDirectory.walkInternal(visitor);
    }
}
