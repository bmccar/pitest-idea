package org.pitestidea.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PathsList;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.constants.PluginVersions;
import org.pitestidea.model.SysDiffs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Enhances the user classpath with bundled libraries, unless they have already been provided in the project classpath.
 * This relies on 'conditional' directories being present in the sandbox, which is done using alternative
 * Gradle configurations placed in subdirectories prefixed with conditional operators.
 */
class ClassPathConfigurator {
    private static final Logger LOGGER = Logger.getInstance(ClassPathConfigurator.class);

    // Sandbox-based library folder used by Intellij
    static final String PLUGIN_LIB = "lib";

    @VisibleForTesting
    static boolean versionLte(SemVer ver1, SemVer ver2) {
        if (ver1 == null || ver2 == null) {
            LOGGER.warn(String.format("Unable to parse versions %s to %s", ver1, ver2));
        } else {
            return ver2.equals(ver1) || ver2.isGreaterThan(ver1);
        }
        return false;
    }

    /**
     * Invokes the supplied function with the bounds of the version string if there is one,
     * else if none, then with two equal arguments. Also discounts a ".jar" suffix if present.
     *
     * @param path to read
     * @param fn   to call with two arguments bounding the version string if present, else both equal to the end
     * @return fn value
     */
    private static String versionBounds(String path, BiFunction<Integer, Integer, String> fn) {
        int to = path.length(); // path="/c-1.2.jar", to=6
        if (path.endsWith(".jar")) {
            to -= 4;
        }
        for (int i = to - 1; i >= 0; i--) {
            char c = path.charAt(i);
            if (!(Character.isDigit(c) || c == '.')) {
                if (c == '-') {
                    return fn.apply(i + 1, to);
                }
                break;
            }
        }
        return fn.apply(to, to);
    }


    /**
     * Returns "c" from "a/b/c-1.2.3.jar". Works with either unix or win path separators.
     *
     * @param path to read
     * @return name, without the version, of a multi-segment pat
     */
    @VisibleForTesting
    static @NotNull String lastSegmentNameOf(String path) {
        return versionBounds(path, (from, to) -> {
            if (from < to) {
                to = from - 1;  // Account for the necessary '-' of the version string
            }
            int ix = path.lastIndexOf(SysDiffs.fs(), from);
            if (ix < 0 && SysDiffs.fs() != '/') {
                ix = path.lastIndexOf('/', from);
            }
            from = ix < 0 ? 0 : ix + 1;
            return path.substring(from, to);
        });
    }

    /**
     * Returns "1.2.3" from "a/b/c-1.2.3.jar".
     *
     * @param path to read
     * @return version of the last segment of a multi-segment path
     */
    @VisibleForTesting
    static @Nullable String lastSegmentVersionOf(String path) {
        return versionBounds(path, (from, to) ->
                from < to ? path.substring(from, to) : null
        );
    }

    private static void walkDir(Map<String, String> pathMap, Path path, List<String> paths, List<String> addedPaths, boolean add) throws IOException {
        try (Stream<Path> files = Files.list(path)) {
            files.forEach(f -> {
                try {
                    walk(pathMap, f, paths, addedPaths, add);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Processes a path, which may mean adding it to paths and addedPaths or further processing its
     * subdirectories if it is a conditional directory.
     *
     * @param pathMap    to read
     * @param fullPath   to process
     * @param paths      original user classPath
     * @param addedPaths added paths
     * @param add        whether to add at this level
     * @throws IOException on error
     */
    private static void walk(Map<String, String> pathMap, Path fullPath, List<String> paths, List<String> addedPaths, boolean add) throws IOException {
        String fullPathStr = fullPath.toString();
        int ix = fullPathStr.lastIndexOf(SysDiffs.fs());
        String segment = ix < 0 ? fullPathStr : fullPathStr.substring(ix + 1);
        if (segment.startsWith("ifn-")) {
            String nm = segment.substring(4);
            if (!pathMap.containsKey(nm)) {
                walkDir(pathMap, fullPath, paths, addedPaths, true);
            }
        } else if (segment.startsWith("ifc-")) {
            String nm = segment.substring(4);
            String nmPath = pathMap.get(nm);
            if (nmPath != null) {
                try (Stream<Path> alternatives = Files.list(fullPath)) {
                    String matchVersion = chooseFromIfcVersion(nmPath, alternatives.map(Path::toString).toList());
                    walkDir(pathMap, fullPath.resolve(matchVersion), paths, addedPaths, true);
                }
            }
        } else if (fullPath.toFile().isDirectory()) {
            walkDir(pathMap, fullPath, paths, addedPaths, add);
        } else if (segment.endsWith(".jar")) {
            if (add) {
                String root = lastSegmentNameOf(segment);
                if (!pathMap.containsKey(root)) {
                    String toAdd = fullPath.toString();
                    paths.add(toAdd);
                    addedPaths.add(toAdd);
                }
            }
        }
    }

    /**
     * Choose which among the supplied versions best matches matchVersion.
     *
     * @param matchVersion a full jar path that includes a version, e.g. "a/b-3.2.1.jar"
     * @param versions     directories whose final segment is a version
     * @return the matching version from versions, e.g., a string "3.2.1"
     */
    @VisibleForTesting
    static @NotNull String chooseFromIfcVersion(String matchVersion, List<String> versions) {
        SemVer version = SemVer.parseFromText(lastSegmentVersionOf(matchVersion));
        if (version != null) {
            SemVer bestMatch = null;
            SemVer least = null;
            for (String nextVersionToMatch : versions) {
                SemVer nextVersion = SemVer.parseFromText(SysDiffs.lastSegmentOf(nextVersionToMatch));
                if (least == null || versionLte(nextVersion, least)) {
                    least = nextVersion;
                }
                if (versionLte(nextVersion, version)) {
                    if (bestMatch == null || versionLte(bestMatch, nextVersion)) {
                        bestMatch = nextVersion;
                    }
                }
            }
            if (bestMatch == null) {
                if (least == null) {
                    throw new IllegalStateException("Unable to choose from versions " + versions);
                }
                return least.toString();
            } else {
                return bestMatch.toString();
            }
        }
        throw new IllegalStateException("Unable to choose from versions " + versions);
    }

    /**
     * Returns the path to the sandbox lib directory where bundled libraries are stored.
     *
     * @return absolute lib directory path
     */
    @VisibleForTesting
    static Path libDir() {
        return PathManager.getPluginsDir().resolve(PluginVersions.PLUGIN_NAME).resolve(PLUGIN_LIB);
    }

    private static boolean pathLte(String p1, String p2) {
        if (p1 == null) p1 = "1";
        if (p2 == null) p2 = "1";
        SemVer p1Ver = SemVer.parseFromText(p1);
        SemVer p2Ver = SemVer.parseFromText(p2);
        if (p1Ver != null && p2Ver != null) {
            return versionLte(p1Ver, p2Ver);
        } else {
            return p1.compareTo(p2) <= 0;
        }
    }

    /**
     * Updates the classpath with bundled libraries unless they have already been provided in the project classpath.
     * Each version of PIT may require different bundling, and only some versions are bundled, so PIT may fail
     * if the "best-guess" here doesn't work with it.
     *
     * <p>Process the following directives that exist in bundled directory paths:
     * <ul>
     *     <li>ifn-NAME: Only included the matching artifact is not present,
     *         e.g. "ifn-junit-jupiter-engine" would only be included if no junit-jupiter-engine was already in the classpath</li>
     *     <li>ifc-NAME/VERSION: NAME must be present in the classpath,
     *         and among all such directories with that prefix, chooses the VERSION that best matches the classpath version,
     *         where matching is the highest version that is not greater than that in the classpath
     *     </li>
     * </ul>
     *
     * @param classPath to read and update
     * @return list of paths added by this routine, which should be a subset of those added to classPath
     */
    static List<String> updateClassPathBundles(PathsList classPath) {
        List<String> paths = classPath.getPathList();
        List<String> addedPaths = new ArrayList<>();

        // Map segment name back to their full path, preferring higher versions if there are name conflicts
        Map<String, String> pathMap = new HashMap<>();
        for (String path : paths) {
            if (path.endsWith(".jar")) {
                String root = lastSegmentNameOf(path);
                String existing = pathMap.get(root);
                if (pathLte(existing, root)) {
                    pathMap.put(root, path);
                }
            }
        }

        try {
            walkDir(pathMap, libDir(), paths, addedPaths, false);
            classPath.clear();
            classPath.addAll(paths);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return addedPaths;
    }
}
