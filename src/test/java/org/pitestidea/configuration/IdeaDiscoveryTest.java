package org.pitestidea.configuration;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

class IdeaDiscoveryTest {
    private static final MockedStatic<CompilerPaths> compilerPathsMockedStatic = Mockito.mockStatic(CompilerPaths.class);
    private static final MockedStatic<GradleUtils> gradleUtilsMockedStatic = Mockito.mockStatic(GradleUtils.class);
    private static final MockedStatic<ProjectUtil> projectUtilMockedStatic = Mockito.mockStatic(ProjectUtil.class);


    @AfterAll
    static void afterAll() {
        compilerPathsMockedStatic.close();
        gradleUtilsMockedStatic.close();
        projectUtilMockedStatic.close();
    }

    @Test
    public void absOutputPath() {
        VirtualFile moduleOutputDirectoryFile = Mockito.mock(VirtualFile.class);
        // What the system tells us is the output directory
        Mockito.when(moduleOutputDirectoryFile.getPath()).thenReturn("/module/a/b/c/d");
        Module module = Mockito.mock(Module.class);

        compilerPathsMockedStatic.when(() -> CompilerPaths.getModuleOutputDirectory(eq(module), Mockito.anyBoolean())).thenReturn(moduleOutputDirectoryFile);
        gradleUtilsMockedStatic.when(() -> GradleUtils.findModulePathGradle(eq(module))).thenReturn(null);
        VirtualFile moduleDirFile = Mockito.mock(VirtualFile.class);
        projectUtilMockedStatic.when(() -> ProjectUtil.guessModuleDir(eq(module))).thenReturn(moduleDirFile);
        // What is the module location, excluding the output directory?
        Mockito.when(moduleDirFile.getPath()).thenReturn("/module");
        // We need only the immediate child of that directory plus the subs
        assertEquals("/module/a/x/y.z", IdeaDiscovery.getAbsoluteOutputPath(module, "x", "y.z"));
    }

    @Test
    void getCurrentFile_withSelectedFile() {
        try (MockedStatic<ProjectManager> projectManager = Mockito.mockStatic(ProjectManager.class);
             MockedStatic<FileEditorManager> fileEditorManager = Mockito.mockStatic(FileEditorManager.class)) {

            Project mockProject = Mockito.mock(Project.class);
            ProjectManager mockProjectManager = Mockito.mock(ProjectManager.class);
            projectManager.when(ProjectManager::getInstance).thenReturn(mockProjectManager);
            Mockito.when(mockProjectManager.getOpenProjects()).thenReturn(new Project[]{mockProject});

            FileEditorManager mockFileEditorManager = Mockito.mock(FileEditorManager.class);
            fileEditorManager.when(() -> FileEditorManager.getInstance(mockProject)).thenReturn(mockFileEditorManager);

            VirtualFile mockFile = Mockito.mock(VirtualFile.class);
            Mockito.when(mockFileEditorManager.getSelectedFiles()).thenReturn(new VirtualFile[]{mockFile});

            VirtualFile currentFile = IdeaDiscovery.getCurrentFile();

            assertEquals(mockFile, currentFile);
        }
    }
}