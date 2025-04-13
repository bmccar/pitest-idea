package org.pitestidea.configuration;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import java.awt.*;

/**
 * Various utilities for accessing elements of the opened project and files in Intellij.
 */
public class IdeaDiscovery {
    public static Project getActiveProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : openProjects) {
            Window window = WindowManager.getInstance().getFrame(project);
            if (window != null && window.isActive()) {
                activeProject = project;
                break;
            }
        }
        return activeProject;
    }

    public static Module getModuleOf(PsiJavaFile file) {
        return ModuleUtil.findModuleForPsiElement(file);
    }

    public static PsiJavaFile getCurrentJavaFile() {
        Project activeProject = getActiveProject();
        Document currentDoc = FileEditorManager.getInstance(activeProject).getSelectedTextEditor().getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(activeProject);
        PsiFile psiFile = documentManager.getPsiFile(currentDoc);
        return (PsiJavaFile) psiFile;
    }

    public static PsiClass getMainClassInFile(PsiJavaFile file) {
        PsiJavaFile psiJavaFile = getCurrentJavaFile();
        PsiClass[] classes = psiJavaFile.getClasses();
        return classes[0]; // TODO multiple classes in file
    }

    public static String getCurrentPackageName() {
        PsiJavaFile file = getCurrentJavaFile();
        return file.getPackageName();
    }

    public static String getCurrentClassName() {
        PsiJavaFile file = getCurrentJavaFile();
        PsiClass targetClass = getMainClassInFile(file);
        return targetClass.getName();
    }

    public static String getCurrentTestClassName() {
        return getCurrentClassName() + "Test"; // TODO (a) verify exists, (b) check Test file is current
    }

    public static String getProjectDirectory() {
        Project activeProject = getActiveProject();
        return activeProject.getBasePath();
    }

    public static String getReportDir() {
        return getReportDir(getActiveProject());
    }

    public static String getReportDir(Project project) {
        String projectDir = project.getBasePath();
        return projectDir + "/target/report/pitestidea";
    }

    /**
     * Resolves a VirtualFile from the given fully qualified name.
     *
     * @param project The current project
     * @param fullyQualifiedName The fully qualified name of the file (e.g., "com.example.TestClass")
     * @return The VirtualFile, or null if the file cannot be found
     */
    public static VirtualFile findVirtualFileByFQN(Project project, String fullyQualifiedName) {
        // Get the project base path
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            return null; // Project base path is unavailable.
        }

        // Convert the FQN to the relative file path (e.g., "com/example/TestClass.java")
        String relativePath = fullyQualifiedName.replace('.', '/') + ".java";

        // Iterate through all source roots of the project
        for (VirtualFile sourceRoot : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            // Find the file in this source root
            VirtualFile file = sourceRoot.findFileByRelativePath(relativePath);
            if (file != null) {
                return file; // Return the file if found
            }
        }

        // File not found in any source root
        return null;
/*
        // Construct the full file path using the project's base path
        String fullPath = Paths.get(projectBasePath, "src", relativePath).toString();
        System.out.format("qn=%s, rp=%s, fp=%s%n",fullyQualifiedName,relativePath,fullPath);

        System.out.println(">>> " + LocalFileSystem.getInstance().findFileByPath(fullPath));

        return LocalFileSystem.getInstance().findFileByPath(fullPath);
 */
    }
}
