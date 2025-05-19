package org.pitestidea.render;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;

public interface IMutationsFileHandler {
    default void fileOpened(Project project, VirtualFile file, FileMutations fileMutations, IMutationScore score) {
    }

    default void fileSelected(Project project, VirtualFile file) {
    }

    default void fileClosed(Project project, VirtualFile file) {
    }
}