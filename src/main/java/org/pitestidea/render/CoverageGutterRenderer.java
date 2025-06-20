package org.pitestidea.render;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.*;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.FileDocumentManager;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.IMutationScore;
import org.pitestidea.model.LineImpact;
import org.pitestidea.model.Mutation;

import javax.swing.*;
import java.util.List;

/**
 * Visual display of mutation results.
 */
public class CoverageGutterRenderer implements IMutationsFileHandler {

    private static final Key<Boolean> HIGHLIGHTER_KEY = new Key<>("PitHighlighter");
    private static final TextAttributes ICON_TEXT_ATTRIBUTES = new TextAttributes();
    private static final int ICON_LAYER = HighlighterLayer.WARNING + 20;
    private static final CoverageGutterRenderer INSTANCE = new CoverageGutterRenderer();

    private CoverageGutterRenderer() {
    }

    public static CoverageGutterRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void fileOpened(Project project, VirtualFile file, FileMutations fileMutations, IMutationScore score) {
        Application app = ApplicationManager.getApplication();
        app.executeOnPooledThread(() -> app.runReadAction(() -> fileMutations.visit(lineImpact -> addGutterIcon(project, file, lineImpact))));
    }

    @Override
    public void fileClosed(Project project, VirtualFile file) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        removeDocumentIcons(document, project);
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

    private static void addGutterIcon(Project project, VirtualFile file, LineImpact lineImpact) {
        int lineNumber = lineImpact.getLineNumber();
        //List<Mutation> records = lineImpact.getMutations(LineImpact.LineImpactPoint.CURRENT);
        if (lineNumber > 0) {
            // Adjust for IJ editor positioning -- sometimes lineNumber is zero, apparently when PIT can't locate it
            lineNumber -= 1;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(file);
        if (psiFile != null) {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document != null) {
                final int startOffset = document.getLineStartOffset(lineNumber);
                final int endOffset = document.getLineEndOffset(lineNumber);

                if (startOffset != -1 && endOffset != -1) {
                    MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);

                    String pfx1 = null;
                    String pfx2 = null;
                    final boolean diff = lineImpact.getMutations(LineImpact.LineImpactPoint.PREVIOUS) != null;
                    if (diff) {
                        pfx1 = "From <i>most recent</i> run";
                        pfx2 = "From <i>previous</i> run";
                    }

                    addLineIcon(markupModel, lineNumber, lineImpact, LineImpact.LineImpactPoint.CURRENT, pfx1);
                    if (diff) {
                        addLineIcon(markupModel, lineNumber, lineImpact, LineImpact.LineImpactPoint.PREVIOUS, pfx2);
                    }
                }
            }
        }
    }

    private static void addLineIcon(MarkupModel markupModel, int adjustedLineNumber, LineImpact lineImpact, LineImpact.LineImpactPoint point, String header) {
        List<Mutation> records = lineImpact.getMutations(point);
        String iconFile = locateIconFile(records);
        if (iconFile != null) {
            Icon icon = IconLoader.getIcon(iconFile, CoverageGutterRenderer.class);
            RangeHighlighter highlighter = markupModel.addLineHighlighter(adjustedLineNumber, ICON_LAYER, ICON_TEXT_ATTRIBUTES);
            highlighter.putUserData(HIGHLIGHTER_KEY, Boolean.TRUE);
            String tooltip = createTooltipFrom(header, records);
            highlighter.setGutterIconRenderer(new MutationGutterIconographer(icon, tooltip));
        }
    }

    private static @NotNull String createTooltipFrom(String header, List<Mutation> records) {
        StringBuilder sb = new StringBuilder();
        if (header != null) {
            sb.append(header);
            sb.append("&#58;<br>");
        }
        for (Mutation record : records) {
            String description = record.description();
            String anchor = PitLinkAnchors.linkFor(description);
            if (anchor != null) {
                description = "<a href=\"https://pitest.org/quickstart/mutators/#" + anchor + "\">" + description + "</a>";
            }
            sb.append(String.format("%s&#58; %s%n", record.mutationImpact(), description));
        }
        return sb.toString();
    }

    public static void removeGutterIcons(Project project) {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            Document doc2 = editor.getDocument();
            removeDocumentIcons(doc2, project);
        }
    }

    private static void removeDocumentIcons(Document currentDoc, Project project) {
        if (currentDoc != null) {
            MarkupModel markupModel = DocumentMarkupModel.forDocument(currentDoc, project, true);
            for (@NotNull RangeHighlighter next : markupModel.getAllHighlighters()) {
                if (next.getUserData(HIGHLIGHTER_KEY) != null) {
                    markupModel.removeHighlighter(next);
                }
            }
        }
    }
}
