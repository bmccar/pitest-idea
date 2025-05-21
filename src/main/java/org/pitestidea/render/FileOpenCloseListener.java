package org.pitestidea.render;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.model.PitExecutionRecorder;
import org.pitestidea.model.PitRepo;
import org.pitestidea.toolwindow.MutationControlPanel;
import org.pitestidea.toolwindow.PitToolWindowFactory;

public class FileOpenCloseListener implements FileEditorManagerListener {

    /**
     * Processes all currently open files in a project in the same manner as if a fileOpened event occurred.
     *
     * @param project to check for open files
     */
    public static void replayOpenFiles(Project project) {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (project == editor.getProject()) {
                Document document = editor.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                if (file != null) {
                    fileOpenedInternal(FileEditorManager.getInstance(project), file);
                }
            }
        }
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ApplicationManager.getApplication().runReadAction(() -> fileOpenedInternal(source, file));
    }

    private static void fileOpenedInternal(FileEditorManager source, VirtualFile file) {
        Project project = source.getProject();
        PitExecutionRecorder xr = PitRepo.get(project);
        if (xr != null) {
            MutationControlPanel mutationControlPanel =
                    PitToolWindowFactory.getOrCreateControlPanel(project);
            if (mutationControlPanel.isGutterIconsEnabled()) {
                CoverageGutterRenderer renderer = CoverageGutterRenderer.getInstance();
                xr.visit(project, renderer, file);
            }
        }
    }

    @Override
    public void fileClosed(FileEditorManager source, @NotNull VirtualFile file) {
        CoverageGutterRenderer renderer = CoverageGutterRenderer.getInstance();
        renderer.fileClosed(source.getProject(), file);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        /*
        VirtualFile newFile = event.getNewFile();
        if (newFile == null) {
            System.out.println("Selection closed");
        } else {
            System.out.println("Selection Changed to: " + newFile.getName());
        }
         */
    }
}

