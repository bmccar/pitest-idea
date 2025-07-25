package org.pitestidea.model;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.reader.IMutationsRecorder;
import org.pitestidea.render.IMutationsFileHandler;
import org.pitestidea.toolwindow.DisplayChoices;
import org.pitestidea.toolwindow.Sorting;
import org.pitestidea.toolwindow.Viewing;

import java.util.*;

/**
 * Records the single execution output of a PITest and reorganizes individual lines into
 * a hierarchical directory/file structure.
 */
public class PitExecutionRecorder implements IMutationsRecorder {
    public static final String ROOT_PACKAGE_NAME = "Aggregated Results";
    private final Module module;
    private final Map<VirtualFile, FileGroup> fileCache = new HashMap<>();
    private final Map<VirtualFile, FileGroup> lastFileCache;
    private final List<FileGroup> sortedFiles = new ArrayList<>();
    private final PkgGroup rootDirectory;
    private final Map<VirtualFile, PkgGroup> pkgCache = new HashMap<>();
    private final Map<VirtualFile, PkgGroup> lastPkgCache;
    private boolean hasMultiplePackages = false;

    public PitExecutionRecorder(Module module, PitExecutionRecorder previousRecorder) {
        rootDirectory = new PkgGroup(ROOT_PACKAGE_NAME, null, previousRecorder == null ? null : previousRecorder.rootDirectory);
        this.module = module;
        this.lastFileCache = previousRecorder == null ? Collections.emptyMap() : previousRecorder.fileCache;
        this.lastPkgCache = previousRecorder == null ? Collections.emptyMap() : previousRecorder.pkgCache;
        rootDirectory.hasCodeFileChildren = true; // Force this package to be displayed
    }

    public Module getModule() {
        return module;
    }

    /**
     * Returns a file for which at least one mutation has been seen for this run. The selected file is the
     * least obtrusive, with priority being the selected file or the first open file or the first file if non-opened.
     *
     * @return the file to open in the editor, or none if empty (though that would be unexpected)
     */
    public @Nullable VirtualFile getNaturalFileToActivate() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(module.getProject());
        List<VirtualFile> selectedFiles = Arrays.asList(fileEditorManager.getSelectedFiles());

        VirtualFile firstFile = null;
        VirtualFile firstOpenFile = null;
        for (VirtualFile nextFile : fileCache.keySet()) {
            if (firstFile == null) {
                firstFile = nextFile;
            }
            if (firstOpenFile == null && fileEditorManager.isFileOpen(nextFile)) {
                firstOpenFile = nextFile;
            }
            if (selectedFiles.contains(nextFile)) {
                return nextFile;
            }
        }

        if (firstOpenFile != null) {
            return firstOpenFile;
        } else {
            return firstFile;
        }
    }

    private static Comparator<Directory> sortCmp(DisplayChoices choices) {
        Comparator<Directory> fn;
        switch (choices.sortBy()) {
            case PROJECT -> fn = Comparator.comparing(Directory::getName);
            //case NAME -> fn = Comparator.comparing(d->SysDiffs.lastSegmentOf(d.getName()));
            case SCORE -> fn = Comparator.comparing(Directory::getScore);
            default -> throw new IllegalArgumentException("Unsupported sorting by: " + choices.sortBy());
        }
        if (choices.sortDirection() == Sorting.Direction.DESC) {
            fn = fn.reversed();
        }
        return fn;
    }

    public interface PackageDiver {
        void apply(FileVisitor visitor);

        boolean hasCodeFileChildren();

        boolean isTopLevel();
    }

    private interface Directory extends IMutationScore {
        void walkInternal(FileVisitor visitor);

        void coalesce(boolean topLevel);

        void sort(DisplayChoices choices);
    }

    private class PkgGroup extends BaseMutationsScore implements Directory, PackageDiver {
        private String name;
        private final PkgGroup parent;
        private Map<String, Directory> children = new HashMap<>();
        private List<Directory> sortedChildren = null;
        private boolean hasCodeFileChildren = false;

        private PkgGroup(String name, PkgGroup parent, PkgGroup lastGroup) {
            super(parent == null ? 0 : parent.children.size(), lastGroup);
            this.parent = parent;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getQualifiedName() {
            if (this == rootDirectory) {
                return null;
            } else if (parent == rootDirectory) {
                return name;
            } else {
                return parent.getQualifiedName() + '.' + name;
            }
        }

        @Override
        public void walkInternal(FileVisitor visitor) {
            visitor.visit(name, getQualifiedName(), this, this);
        }

        /**
         * Modifies the tree by merging packages containing a single package child by absorbing that child,
         * e.g. "com"->"foo"->"bar.java" becomes "com.foo"->"bar.java" if "foo" is the only child of "com".
         *
         * @param topLevel true if this is the top level line, which should not be merged
         */
        @Override
        public void coalesce(boolean topLevel) {
            children.values().forEach(c -> c.coalesce(false));
            if (children.size() == 1 && !topLevel) {
                String key = children.keySet().stream().findFirst().get();
                Directory dir = children.get(key);
                if (dir instanceof PkgGroup pkgGroup) {
                    children.remove(key);
                    key = name + '.' + key;
                    // Absorb a single child
                    name = key;
                    children = pkgGroup.children;
                    hasCodeFileChildren = pkgGroup.hasCodeFileChildren;
                }
            }
        }

        @Override
        public void sort(DisplayChoices choices) {
            Comparator<Directory> fn = sortCmp(choices);
            sortedChildren = children.values().stream().sorted(fn).toList();
            children.values().forEach(c -> c.sort(choices));
        }

        @Override
        public void apply(FileVisitor visitor) {
            Collection<Directory> subs = sortedChildren == null ? children.values() : sortedChildren;
            subs.forEach(g -> g.walkInternal(visitor));
        }

        @Override
        public boolean hasCodeFileChildren() {
            return hasCodeFileChildren;
        }

        @Override
        public boolean isTopLevel() {
            return this == rootDirectory;
        }

        @Override
        public String toString() {
            return "Pkg:" + name + '/' + children.size();
        }
    }

    @VisibleForTesting
    class FileGroup extends BaseMutationsScore implements Directory {
        private final VirtualFile file;
        private final FileMutations fileMutations;

        private FileGroup(VirtualFile file, String pkg, PkgGroup parent, FileGroup lastFileGroup) {
            super(parent.children.size(), lastFileGroup);
            this.file = file;
            this.fileMutations = new FileMutations(pkg, lastFileGroup == null ? null : lastFileGroup.fileMutations);
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
        public void sort(DisplayChoices _choices) {
            // Nothing to do
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getQualifiedName() {
            return getName();
        }
    }

    @Override
    public void record(String pkg, VirtualFile file, MutationImpact impact, int lineNumber, String description) {
        String[] segments = pkg.split("\\.");
        PkgGroup parentGroup = recordPkg(segments, segments.length - 1, file.getParent(), impact, lineNumber, description);
        parentGroup.hasCodeFileChildren = true;
        FileGroup dir = (FileGroup) parentGroup.children.computeIfAbsent(file.getName(), _k -> {
            FileGroup newFileGroup = new FileGroup(file, pkg, parentGroup, lastFileCache.get(file));
            fileCache.put(file, newFileGroup);
            sortedFiles.add(newFileGroup);
            return newFileGroup;
        });
        dir.fileMutations.add(lineNumber, new Mutation(impact, description));
        dir.accountFor(impact);
    }

    private PkgGroup recordPkg(String[] segments, int i, VirtualFile pkgFile, MutationImpact impact, int lineNumber, String description) {
        final PkgGroup pkgGroup;
        if (i < 0) {
            pkgGroup = rootDirectory;
        } else {
            // Tests need not provide a parent, but in normal execution there should always be one
            VirtualFile parentFile = pkgFile == null ? null : pkgFile.getParent();
            PkgGroup parentGroup = recordPkg(segments, i - 1, parentFile, impact, lineNumber, description);
            String segment = segments[i];
            pkgGroup = pkgCache.computeIfAbsent(pkgFile, _k -> {
                PkgGroup pg = new PkgGroup(segment, parentGroup, lastPkgCache.get(pkgFile));
                parentGroup.children.put(segment, pg);
                return pg;
            });
        }
        pkgCache.put(pkgFile, pkgGroup);
        if (pkgGroup.children.size() > 1) {
            hasMultiplePackages = true;
        }
        pkgGroup.accountFor(impact);
        return pkgGroup;
    }

    @Override
    public void postProcess() {
        rootDirectory.coalesce(true);
    }

    public boolean hasMultiplePackages() {
        return hasMultiplePackages;
    }

    private DisplayChoices displayChoices;

    @Override
    public void sort(DisplayChoices choices) {
        this.displayChoices = choices;
        rootDirectory.sort(choices);
        sortedFiles.sort(sortCmp(choices));
    }

    public interface FileVisitor {
        void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score);

        void visit(String pkg, String qualifiedPkg, PackageDiver diver, IMutationScore score);
    }

    private static final PackageDiver EMPTY_PACKAGE_DIVER = new PackageDiver() {
        @Override
        public void apply(FileVisitor visitor) {

        }

        @Override
        public boolean hasCodeFileChildren() {
            return false;
        }

        @Override
        public boolean isTopLevel() {
            return true;
        }
    };

    public void visit(FileVisitor visitor) {
        if (displayChoices != null && displayChoices.packageChoice() == Viewing.PackageChoice.NONE) {
            if (sortedFiles.size() > 1) {
                visitor.visit(rootDirectory.name, null, EMPTY_PACKAGE_DIVER, rootDirectory);
            }
            sortedFiles.forEach(g -> g.walkInternal(visitor));
        } else {
            Map<String, Directory> subs = rootDirectory.children;
            Directory toWalk = subs.size() == 1 ? subs.values().stream().toList().get(0) : rootDirectory;
            toWalk.walkInternal(visitor);
        }
    }

    public void visit(Project project, IMutationsFileHandler visitor, VirtualFile file) {
        FileGroup fileGroup = fileCache.get(file);
        if (fileGroup != null) {
            visitor.fileOpened(project, file, fileGroup.fileMutations, fileGroup);
        }
    }
}
