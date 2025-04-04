package org.pitestidea.render;

import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.model.Mutation;
import org.pitestidea.model.PitExecutionRecorder;

import javax.swing.*;
import java.util.List;

/**
 * Visual display of mutation results.
 */
public class CoverageGutterRenderer implements ICoverageRenderer {

    private static final Key<Boolean> HIGHLIGHTER_KEY = new Key<>("PitHighlighter");
    private static final TextAttributes ICON_TEXT_ATTRIBUTES = new TextAttributes();
    private static int ICON_LAYER = HighlighterLayer.WARNING + 20;

    @Override
    public void render(Project project, PitExecutionRecorder recorder) {
        removeGutterIcons();
        recorder.visit(new PitExecutionRecorder.FileVisitor() {
            @Override
            public void visit(VirtualFile file, FileMutations fileMutations, IMutationScore score) {
                fileMutations.visit((lineNumber, lineImpact, mutations) -> {
                    addGutterIcon(project, file, lineNumber, mutations);
                });
            }

            @Override
            public void visit(String pkg, PitExecutionRecorder.PackageDiver diver, IMutationScore score) {
                // No place to put icons for packages, but keep walking the tree
                diver.apply(this);
            }
        });
    }

    private static String locateIconFile(List<Mutation> records) {
        int count = records.size();
        String sfx = count <= 4 ? String.valueOf(count) : "*";
        int survived = 0;
        int killed = 0;
        int no_coverage = 0;
        int timed_out = 0;
        for (Mutation record : records) {
            switch (record.mutationImpact()) {
                case KILLED -> killed++;
                case SURVIVED -> survived++;
                case NO_COVERAGE -> no_coverage++;
                case TIMED_OUT -> timed_out++;
            }
        }
        String root;
        if (survived > 0) {
            root = "survived";
        } else if (timed_out > 0) {
            root = "timed_out";
        } else if (killed > 0) {
            root = "killed";
        } else if (no_coverage > 0) {
            root = "no_coverage";
        } else {
            return null;
        }
        return "/icons/" + root + '_' + sfx + ".svg";
    }

    private static void addGutterIcon(Project project, VirtualFile file, int lineNumber, List<Mutation> records) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(file);
        String iconFile = locateIconFile(records);
        if (psiFile != null && iconFile != null) {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document != null) {
                int startOffset = document.getLineStartOffset(lineNumber);
                int endOffset = document.getLineEndOffset(lineNumber);
                if (startOffset != -1 && endOffset != -1) {
                    Icon icon = IconLoader.getIcon(iconFile, CoverageGutterRenderer.class);
                    MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);
                    TextAttributes x = new TextAttributes();
                    RangeHighlighter highlighter = markupModel.addLineHighlighter(lineNumber, ICON_LAYER, ICON_TEXT_ATTRIBUTES);
                    highlighter.putUserData(HIGHLIGHTER_KEY,Boolean.TRUE);
                    StringBuilder sb = new StringBuilder();
                    for (Mutation record : records) {
                        sb.append(String.format("%s: %s%n", record.mutationImpact(), record.description()));
                    }
                    String tooltip = sb.toString();
                    highlighter.setGutterIconRenderer(new MutationGutterIconographer(icon,tooltip));
                }
            }
        }
    }

    public static void removeGutterIcons() {
        Project project = IdeaDiscovery.getActiveProject();
        Document currentDoc = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);
        MarkupModel markupModel = DocumentMarkupModel.forDocument(currentDoc, project, true);
        for (@NotNull RangeHighlighter next: markupModel.getAllHighlighters()) {
            if (next.getUserData(HIGHLIGHTER_KEY) != null) {
                // TODO remove empty lane if not needed
                markupModel.removeHighlighter(next);
            }
        }
    }
}
