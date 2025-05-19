package org.pitestidea.configuration;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nullable;
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

        // Get the content roots of the module
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();

        if (contentRoots.length > 0) {
            // Return the absolute file system path of the first content root
            return contentRoots[0].getPath();
        }
        return null; // No content root available
    }

    public static @Nullable PsiJavaFile getCurrentJavaFile(Project project) {
        @Nullable Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            Document currentDoc = editor.getDocument();
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            PsiFile psiFile = documentManager.getPsiFile(currentDoc);
            return (PsiJavaFile) psiFile;
        }
        return null;
    }

    public static VirtualFile getCurrentFile() {
        Project project = getActiveProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        return fileEditorManager.getSelectedFiles().length > 0
                ? fileEditorManager.getSelectedFiles()[0]
                : null;
    }

    public static String getAbsoluteOutputPath(Module module, String... subs) {
        @Nullable VirtualFile vf = CompilerPaths.getModuleOutputDirectory(module, false);
        if (vf == null) {
            throw new RuntimeException("No output directory for module " + module.getName());
        }
        vf = vf.getParent();
        StringBuilder sb = new StringBuilder(vf.getPath());
        for (String sub : subs) {
            sb.append(FileSystems.getDefault().getSeparator());
            sb.append(sub);
        }
        return sb.toString();
    }

    public static File getAbsoluteOutputDir(Module module, String... subs) {
        @Nullable VirtualFile vf = CompilerPaths.getModuleOutputDirectory(module, false);
        if (vf == null) {
            return null;
        }
        vf = vf.getParent();
        File file = new File(vf.getPath());
        for (String sub : subs) {
            file = new File(file, sub);
        }
        return file;
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
