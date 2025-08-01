package org.pitestidea.reader;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.pitestidea.model.InputBundle;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class PackageWalker {
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("java");  // TODO kt
    private static final List<Function<String, String>> SRC_NAME_TRANSFORMERS = List.of(
            src -> src + "Test",
            src -> "test" + src
    );
    private static final List<Function<String, String>> TEST_NAME_TRANSFORMERS = List.of(
            src -> src.length() > 4 && src.endsWith("Test") ? src.substring(0, src.length() - 4) : null,
            src -> src.length() > 4 && src.startsWith("test") ? src.substring(4) : null
    );

    /**
     * Adjusts the list of files selected by the user and sends them to the collector:
     * <ul>
     *     <li> Finds siblings, which are matching files across the source and test directories
     *     <li> Removes redundant files, which are children of other files in the list
     * </ul>
     * <p>
     * Note that source siblings of items in the test directory are only included if there are no
     * source elements in the original list.
     *
     * @param project     owning the files
     * @param files       supplied by the user
     * @param inputBundle to send results to
     */
    public static void read(Project project, List<VirtualFile> files, InputBundle inputBundle) {
        Set<VirtualFile> srcPkgFiles = new HashSet<>();
        Set<VirtualFile> testPkgFiles = new HashSet<>();
        Set<VirtualFile> srcFiles = new HashSet<>();
        Set<VirtualFile> testFiles = new HashSet<>();
        groupByLocAndType(project, files, srcPkgFiles, testPkgFiles, srcFiles, testFiles);
        addFileSiblings(project, srcFiles, testFiles, SRC_NAME_TRANSFORMERS);
        addPkgSiblings(project, srcPkgFiles, testPkgFiles);
        if (srcFiles.isEmpty() && srcPkgFiles.isEmpty()) {
            addFileSiblings(project, testFiles, srcFiles, TEST_NAME_TRANSFORMERS);
            addPkgSiblings(project, testPkgFiles, srcPkgFiles);
        }
        simplify(srcPkgFiles);
        simplify(testPkgFiles);
        simplify(srcFiles, srcPkgFiles);
        simplify(testFiles, testPkgFiles);

        inputBundle.setPaths(InputBundle.Category.SOURCE_FILE, names(project, srcFiles));
        inputBundle.setPaths(InputBundle.Category.SOURCE_PKG, names(project, srcPkgFiles));
        inputBundle.setPaths(InputBundle.Category.TEST_FILE, names(project, testFiles));
        inputBundle.setPaths(InputBundle.Category.TEST_PKG, names(project, testPkgFiles));
    }

    private static List<String> names(Project project, Set<VirtualFile> files) {
        return files.stream().map(f -> relativize(project, f)).toList();
    }

    private static void addPkgSiblings(Project project, Set<VirtualFile> forFiles, Set<VirtualFile> fileCollection) {
        for (VirtualFile forFile : forFiles) {
            VirtualFile sibling = getMatching(project, forFile, p -> p);
            if (sibling != null) {
                fileCollection.add(sibling);
            }
        }
    }

    private static void addFileSiblings(Project project, Set<VirtualFile> forFiles, Set<VirtualFile> fileCollection, List<Function<String, String>> matchingNameTransformers) {
        for (VirtualFile forFile : forFiles) {
            VirtualFile sibling = getContentFileSibling(project, forFile, matchingNameTransformers);
            if (sibling != null) {
                fileCollection.add(sibling);
            }
        }
    }

    private static String relativize(Project project, VirtualFile file) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        VirtualFile sourceRoot = fileIndex.getSourceRootForFile(file);

        if (sourceRoot == null) {
            return null; // Not under a source root
        }

        String relativePath = file.getPath().substring(sourceRoot.getPath().length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

    private static void groupByLocAndType(Project project,
                                          List<VirtualFile> files,
                                          Set<VirtualFile> srcPkgFiles,
                                          Set<VirtualFile> testPkgFiles,
                                          Set<VirtualFile> srcFiles,
                                          Set<VirtualFile> testFiles) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        for (VirtualFile file : files) {
            final boolean isInTest = fileIndex.isInTestSourceContent(file);
            final boolean isInSrc = fileIndex.isInSourceContent(file) && !isInTest;
            if (file.isDirectory()) {
                VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
                if (Arrays.asList(sourceRoots).contains(file)) {
                    // PIT accepts only files and packages
                    for (VirtualFile child : file.getChildren()) {
                        groupByLocAndType(project, List.of(child), srcPkgFiles, testPkgFiles, srcFiles, testFiles);
                    }
                } else if (isInSrc) {
                    srcPkgFiles.add(file);
                } else if (isInTest) {
                    testPkgFiles.add(file);
                }

            } else if (SUPPORTED_EXTENSIONS.contains(file.getExtension())) {
                if (isInSrc) {
                    srcFiles.add(file);
                } else if (isInTest) {
                    testFiles.add(file);
                }
            }
        }
    }

    /**
     * Removes any files that are children of other files in the given set.
     *
     * @param files to update
     */
    private static void simplify(Set<VirtualFile> files) {
        List<VirtualFile> pending = files.stream().filter(f -> isChildOfAny(f, files)).toList();
        pending.forEach(files::remove);
    }

    /**
     * Removes any files that are children of another set.
     *
     * @param childSet  to update
     * @param parentSet to check for ancestors
     */
    private static void simplify(Set<VirtualFile> childSet, Set<VirtualFile> parentSet) {
        List<VirtualFile> pending = childSet.stream().filter(f -> isChildOfAny(f, parentSet)).toList();
        pending.forEach(childSet::remove);
    }

    private static boolean isChildOfAny(VirtualFile file, Set<VirtualFile> files) {
        VirtualFile parent = file.getParent();
        do {
            if (files.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        } while (parent != null);
        return false;
    }

    /**
     * Returns the sibling of the given file that matches the given name transformers:
     * <ul>
     *     <li> A sibling of a source file X is its counterpart in the test directory
     *     <li> A sibling of a test file X is its counterpart in the source directory.
     * </ul>
     *
     * @param project                  containing the file
     * @param virtualFile              to find sibling of
     * @param matchingNameTransformers possible ways of transforming the file name to find a sibling of
     * @return sibling if any, or null if no sibling could be found
     */
    public static VirtualFile getContentFileSibling(Project project, VirtualFile virtualFile, List<Function<String, String>> matchingNameTransformers) {
        String name = virtualFile.getName();
        String sfx = "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            sfx = name.substring(lastDot + 1);  // TODO can languages alt between src & test?
            name = name.substring(0, lastDot);
        }
        final String finalName = name;
        final String finalSfx = sfx;

        if (SUPPORTED_EXTENSIONS.contains(sfx)) {
            return getMatching(project, virtualFile.getParent(), p -> {
                for (Function<String, String> transformer : matchingNameTransformers) {
                    String transformedName = transformer.apply(finalName);
                    if (transformedName != null) {
                        VirtualFile match = p.findChild(transformedName + '.' + finalSfx);
                        if (match != null && match.exists()) {
                            return match;
                        }
                    }
                }
                return null;
            });
        }
        return null;
    }

    /**
     * Finds a file in a source other than that containing virtualFile which (a) has the same relative path and
     * (b) passes the supplied fn that returns a non-null result.
     *
     * @param project     to search
     * @param virtualFile to match
     * @param fn          to determine the final result
     * @return an existing valid file or null if none of the criteria above match
     */
    private static VirtualFile getMatching(Project project, VirtualFile virtualFile, Function<VirtualFile, VirtualFile> fn) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        VirtualFile rootFile = fileIndex.getSourceRootForFile(virtualFile);

        if (rootFile != null) {
            String relativePath = virtualFile.getPath().substring(rootFile.getPath().length());
            Predicate<VirtualFile> typeFilter =
                    fileIndex.isInTestSourceContent(virtualFile) ?
                            fileIndex::isInSourceContent :
                            fileIndex::isInTestSourceContent;

            VirtualFile[] altRoots = Arrays.stream(ProjectRootManager.getInstance(project).getContentSourceRoots())
                    .filter(typeFilter)
                    .toArray(VirtualFile[]::new);

            for (VirtualFile altRoot : altRoots) {
                if (!altRoot.equals(rootFile)) {
                    String altPackagePath = altRoot.getPath() + relativePath;
                    String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, altPackagePath);
                    VirtualFile altFile = VirtualFileManager.getInstance().findFileByUrl(url);

                    if (altFile != null && altFile.exists() && altFile.isDirectory() == virtualFile.isDirectory()) {
                        VirtualFile match = fn.apply(altFile);
                        if (match != null) {
                            return match;
                        }
                    }
                }
            }
        }
        return null;
    }
}
