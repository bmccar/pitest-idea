package org.pitestidea.configuration;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.model.CachedRun;

import java.awt.*;
import java.io.File;
import java.nio.file.FileSystems;

/**
 * Various utilities for accessing elements of the opened project and files in Intellij.
 */
public class IdeaDiscovery {
    private static final Logger LOGGER = Logger.getInstance(IdeaDiscovery.class);

    public static Project getActiveProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        if (openProjects.length == 1) {
            // This is not just for efficiency, but at times the single window is not active,
            // maybe only during debugging?
            activeProject = openProjects[0];
        } else {
            for (Project project : openProjects) {
                Window window = WindowManager.getInstance().getFrame(project);
                if (window != null && window.isActive()) {
                    activeProject = project;
                    break;
                }
            }
        }
        return activeProject;
    }

    public static @Nullable String getAbsolutePathOfModule(Module module) {
        if (module == null) {
            return null; // Safety check
        }

        String path = GradleUtils.findModulePathGradle(module);
        if (path == null) {
            VirtualFile file = ProjectUtil.guessModuleDir(module);
            if (file != null) {
                path = file.getPath();
            }
        }
        return path;
    }

    public static VirtualFile getCurrentFile() {
        Project project = getActiveProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        return fileEditorManager.getSelectedFiles().length > 0
                ? fileEditorManager.getSelectedFiles()[0]
                : null;
    }

    public static String getAndSetClassPathOuts(Module module, PathsList list) {
        String outDir = getModuleOutputDirectory(module);
        String mutableCodePath = outDir + File.separatorChar + "classes";
        File testClasses = new File(outDir + File.separatorChar + "test-classes");
        if (testClasses.exists()) {  // true for Maven, false for Gradle
            list.add(outDir + File.separatorChar + "classes");
            list.add(testClasses.getPath());
        } else {
            list.add(outDir + File.separatorChar + "classes/java/main");
            list.add(outDir + File.separatorChar + "classes/java/test");
            mutableCodePath += "/java/main";
        }
        return mutableCodePath;
    }

    /**
     * Returns the output directory immediately below the module directory.
     *
     * @param module to find output directory for
     * @return output directory
     */
    @VisibleForTesting
    public static @Nullable String getModuleOutputDirectory(@NotNull Module module) {
        @Nullable VirtualFile vf = CompilerPaths.getModuleOutputDirectory(module, false);
        if (vf == null) {
            // Ensure it's a Gradle ".main" module because the ".test" module does not have the output directory
            Module alt = GradleUtils.ensureMainModule(module);
            if (alt != module) {
                return getModuleOutputDirectory(alt);
            }
            return null;
        }
        String path = vf.getPath();
        // From the CompilerPaths call, a Gradle project returns several levels while Maven projects just one,
        // so ensure we're returning only the immediate child of the base
        String base = getAbsolutePathOfModule(module);
        if (base != null) {
            int from = path.indexOf(File.separatorChar, base.length() + 2);
            if (from > 0) {
                return path.substring(0, from);
            }
        }
        return null;
    }

    public static @Nullable String getAbsoluteOutputPath(Module module, String... subs) {
        String path = getModuleOutputDirectory(module);
        if (path == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(path);
        for (String sub : subs) {
            sb.append(FileSystems.getDefault().getSeparator());
            sb.append(sub);
        }
        return sb.toString();
    }

    public static @Nullable File getAbsoluteOutputDir(Module module, String... subs) {
        String path = getModuleOutputDirectory(module);
        if (path == null) {
            return null;
        }
        if (subs != null && subs.length > 0) {
            StringBuilder sb = new StringBuilder(path);
            for (String sub : subs) {
                sb.append(FileSystems.getDefault().getSeparator());
                sb.append(sub);
            }
            path = sb.toString();
        }
        return new File(path);
    }

    public static <T> T onLocationOf(Project project, VirtualFile selectedFile, T code, T test) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        boolean isSrc = fileIndex.isInSourceContent(selectedFile);
        boolean isTest = fileIndex.isInTestSourceContent(selectedFile);
        if (isSrc && !isTest) {
            return code;
        } else if (isTest) {
            return test;
        } else {
            return null;
        }
    }

    public static void openBrowserTo(String url) {
        if (url != null) {
            com.intellij.ide.BrowserUtil.browse(url);
        }
    }

    public static String getUrl(CachedRun run) {
        return "file://" + run.getReportDir() + "/index.html";
    }

    public static String getPackageUrl(CachedRun run, String packageName) {
        return "file://" + run.getReportDir() + File.separatorChar + packageName + File.separatorChar + "index.html";
    }

    public static String getUrl(CachedRun run, VirtualFile file) {
        String sfx = IdeaDiscovery.getPackageOf(run.getProject(), file);
        if (sfx != null) {
            if (file.isDirectory()) {
                sfx += "/index.html";
            } else {
                sfx += File.separatorChar + file.getName() + ".html";
            }
            return "file://" + run.getReportDir() + File.separatorChar + sfx;
        }
        return null;
    }

    public static String getPackageOf(Project project, VirtualFile file) {
        String filePath = file.getPath();

        VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();

        for (VirtualFile sourceRoot : sourceRoots) {
            String sourcePath = sourceRoot.getPath();
            if (filePath.startsWith(sourcePath)) {
                String relativePath = filePath.substring(sourcePath.length() + 1);
                relativePath = relativePath.substring(0, relativePath.lastIndexOf(File.separatorChar));

                return relativePath.replace(File.separatorChar, '.');
            }
        }

        return null;
    }

    public static VirtualFile findVirtualFileByRQN(Project project, String relPath) {
        if (!project.isOpen()) {
            String msg = String.format("Project %s not in expected state open=%b, initialized=%b",
                    project.getName(),
                    project.isOpen(),
                    project.isInitialized());
            LOGGER.warn(msg);
        }
        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        // Iterate through all source roots of the project
        for (VirtualFile sourceRoot : rootManager.getContentSourceRoots()) {
            VirtualFile file = sourceRoot.findFileByRelativePath(relPath);
            if (file != null) {
                return file;
            }
        }
        return null;
    }
}
