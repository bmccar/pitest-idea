package org.pitestidea.configuration;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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

    public static String getAbsolutePathOfModule(Module module) {
        if (module == null) {
            return null; // Safety check
        }

        Project project = module.getProject();
        String path = GradleUtils.findModulePathGradle(project, module);
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

     * @param module to find output directory for
     * @return output directory
     */
    @VisibleForTesting
    public static String getModuleOutputDirectory(@NotNull Module module) {
        @Nullable VirtualFile vf = CompilerPaths.getModuleOutputDirectory(module, false);
        if (vf == null) {
            final String firstName = module.getName();
            // Gradle projects have separate modules for module X: "X.main" and "X.test". If the user request
            // was initiated from a test file, we get the "X.test" module which doesn't have the source file.
            // Adjust for that here by switching from X.test to X.main.
            final String gradleSfx = ".test";
            if (firstName.endsWith(gradleSfx) ) {
                String subName = firstName.substring(0, firstName.length() - gradleSfx.length()) + ".main";
                ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
                module =  moduleManager.findModuleByName(subName);
                if (module != null) {
                    return getModuleOutputDirectory(module);
                }
            }
            throw new RuntimeException("No output directory for module " + firstName);
        }
        String path = vf.getPath();
        // From the CompilerPaths call, a Gradle project returns several levels while Maven projects just one,
        // so ensure we're returning only the immediate child of the base
        String base = getAbsolutePathOfModule(module);
        return path.substring(0, path.indexOf(File.separatorChar, base.length()+2));
    }

    public static String getAbsoluteOutputPath(Module module, String... subs) {
        String path = getModuleOutputDirectory(module);
        StringBuilder sb = new StringBuilder(path);
        for (String sub : subs) {
            sb.append(FileSystems.getDefault().getSeparator());
            sb.append(sub);
        }
        return sb.toString();
    }

    public static File getAbsoluteOutputDir(Module module, String... subs) {
        return new File(getAbsoluteOutputPath(module,subs));
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

        // Iterate through all source roots of the project
        for (VirtualFile sourceRoot : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            // Find the file in this source root
            VirtualFile file = sourceRoot.findFileByRelativePath(relPath);
            if (file != null) {
                return file; // Return the file if found
            }
        }
        return null;
    }
}
