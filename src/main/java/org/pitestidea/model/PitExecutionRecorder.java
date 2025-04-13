package org.pitestidea.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.reader.IMutationsRecorder;
import org.pitestidea.render.ICoverageRenderer;
import org.pitestidea.render.IMutationsFileHandler;
import org.pitestidea.toolwindow.Sorting;

import java.util.*;

/**
 * Records the output of a single execution of PITest and reorganizes individual lines into
 * a hierarchical directory/file structure.
 */
public class PitExecutionRecorder implements IMutationsRecorder {
    public static final String ROOT_PACKAGE_NAME = "Results";
    private final Map<VirtualFile, FileGroup> fileCache = new HashMap<>();
    private final PkgGroup rootDirectory = new PkgGroup(ROOT_PACKAGE_NAME,null);
    {
        rootDirectory.hasCodeFileChildren = true; // Force this package to be displayed
    }

    public interface PackageDiver {
        void apply(FileVisitor visitor);
        boolean hasCodeFileChildren();
        boolean isTopLevel();
    }

    private interface Directory extends IMutationScore {
        void walkInternal(FileVisitor visitor);
        void coalesce(boolean topLevel);
        void sort(Sorting.By by, Sorting.Direction dir);
    }

    private class PkgGroup extends BaseMutationsScore implements Directory, PackageDiver {
        private String name;
        private Map<String, Directory> children = new HashMap<>();
        private List<Directory> sortedChildren = null;
        private boolean hasCodeFileChildren = false;

        private PkgGroup(String name, PkgGroup parent) {
            super(parent==null ? 0 : parent.children.size());
            this.name = name;
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(name, this, this);
        }

        /**
         * Modifies the tree by merging packages containing a single package child by absorbing that child,
         * e.g. "com"->"foo"->"bar.java" becomes "com.foo"->"bar.java" if "foo" is the only child of "com".
         *
         * @param topLevel true if this is the top level line, which should not be merged
         */
        @Override
        public void coalesce(boolean topLevel) {
            children.values().forEach(c->c.coalesce(false));
            if (children.size() == 1 && !topLevel) {
                String key = children.keySet().stream().findFirst().get();
                Directory dir = children.get(key);
                if (dir instanceof PkgGroup pkgGroup) {
                    children.remove(key);
                    key = name + '.' + key;
                    // Absorb single child
                    name = key;
                    children = pkgGroup.children;
                    hasCodeFileChildren = pkgGroup.hasCodeFileChildren;
                }
            }
        }

        @Override
        public void sort(Sorting.By by, Sorting.Direction dir) {
            Comparator<Directory> fn;
            switch (by) {
                case PROJECT -> fn = Comparator.comparing(Directory::getOrder);
                case SCORE -> fn = Comparator.comparing(Directory::getScore);
                default -> throw new IllegalArgumentException("Unsupported sorting by: " + by);
            }
            if (dir== Sorting.Direction.DESC) {
                fn = fn.reversed();
            }
            sortedChildren = children.values().stream().sorted(fn).toList();
            children.values().forEach(c->c.sort(by, dir));
        }

        @Override
        public void apply(FileVisitor visitor) {
            Collection<Directory> subs = sortedChildren==null ? children.values() : sortedChildren;
            subs.forEach(g->g.walkInternal(visitor));
        }

        @Override
        public boolean hasCodeFileChildren() {
            return hasCodeFileChildren;
        }

        @Override
        public boolean isTopLevel() {
            return this==rootDirectory;
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

        private FileGroup(VirtualFile file, String pkg, PkgGroup parent) {
            super(parent.children.size());
            this.file = file;
            this.fileMutations = new FileMutations(pkg);
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(file, fileMutations, this);
        }

        @Override
        public void coalesce(boolean topLevel) {
            // Nothing to do
        }

        @Override
        public void sort(Sorting.By by, Sorting.Direction dir) {
            // Nothing to do
        }
    }

    @Override
    public void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description) {
        rootDirectory.accountFor(impact);
        PkgGroup last = rootDirectory;
        for (String segment: pkg.split("\\.")) {
            final PkgGroup parent = last;
            Directory dir = last.children.computeIfAbsent(segment, _k -> new PkgGroup(segment, parent));
            dir.accountFor(impact);
            last = (PkgGroup)dir;
        }
        last.hasCodeFileChildren = true;
        final PkgGroup parent = last;
        FileGroup dir = (FileGroup)last.children.computeIfAbsent(file.getName(), _k -> {
            FileGroup fileGroup = new FileGroup(file, pkg, parent);
            fileCache.put(file, fileGroup);
            return fileGroup;
        });
        dir.fileMutations.add(lineNumber, new Mutation(impact, description));
        dir.accountFor(impact);
    }

    @Override
    public void postProcess() {
        rootDirectory.coalesce(true);
    }

    @Override
    public void sort(Sorting.By by, Sorting.Direction dir) {
        rootDirectory.sort(by, dir);
    }

    public interface FileVisitor {
        void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score);
        void visit(String pkg, PackageDiver diver, IMutationScore score);
    }

    public void visit(FileVisitor visitor) {
        rootDirectory.walkInternal(visitor);
    }

    public void visit(Project project, IMutationsFileHandler visitor, VirtualFile file) {
        FileGroup fileGroup = fileCache.get(file);
        if (fileGroup!=null) {
            visitor.fileOpened(project, file, fileGroup.fileMutations, fileGroup);
        }
    }
}
