package org.pitestidea.render;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.model.*;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Comparator;
import java.util.List;

class AiChatter {

    private static final Logger LOGGER = Logger.getInstance(AiChatter.class);

    private record FileMethod(FileMutations fileMutations, String methodName) {
    }

    static void generateUnitTests(@NotNull AnActionEvent e, int lineNumber) {
        Project project = e.getProject();
        int progress = 1;
        if (project != null) {
            CachedRun cachedRun = PitRepo.getCurrent(project);
            if (cachedRun != null) {
                progress++;
                PitExecutionRecorder recorder = cachedRun.getRecorder();
                FileMethod fileMethod = getSelectedFileMethod(e, lineNumber, recorder);
                if (fileMethod != null) {
                    progress++;
                    FileMutations fileMutations = fileMethod.fileMutations;
                    if (fileMutations != null) {
                        progress++;
                        String methodName = fileMethod.methodName;
                        List<Mutation> mutations = fileMutations.getLineMutations(methodName);
                        StringBuilder sb = new StringBuilder();
                        explain(sb, fileMethod, mutations);
                        format(sb, mutations);
                        progress++;
                        PromptPopup.showCustomPopup(project, sb.toString());
                        progress = -1;
                    }
                }
            }
            if (progress > 0) {
                LOGGER.warn("AiChatter.generateUtils stalled progress at " + progress);
            }
        }
    }

    private static final String KILL_MSG = "change the `SURVIVED` mutations to `KILLED`";
    private static final String COVER_MSG = "cover the `NO_COVERAGE` mutations";
    private static final String ALL_MSG = COVER_MSG + " and " + KILL_MSG;

    private static void explain(StringBuilder sb, FileMethod fileMethod, List<Mutation> mutations) {
        int killCount = getNumberOf(mutations, MutationImpact.KILLED);
        int coverCount = getNumberOf(mutations, MutationImpact.NO_COVERAGE);
        final String msg;
        if (killCount > 0) {
            if (coverCount > 0) {
                msg = ALL_MSG;
            } else {
                msg = KILL_MSG;
            }
        } else if (coverCount > 0) {
            msg = COVER_MSG;
        } else {
            msg = "improve any remaining edge cases";
        }

        sb.append("Create JUnit tests for `");
        sb.append(fileMethod.fileMutations.getFile().getName());
        sb.append('.');
        sb.append(fileMethod.methodName);
        sb.append("` to ");
        sb.append(msg);
    }

    private static int getNumberOf(List<Mutation> mutations, MutationImpact impact) {
        return (int)mutations.stream().filter(m->m.mutationImpact().equals(impact)).count();
    }

    private static void format(StringBuilder sb, List<Mutation> mutations) {
        mutations = mutations.stream().sorted(Comparator.comparing(Mutation::lineNumber)).toList();
        if (!mutations.isEmpty()) {
            sb.append(". The current mutation results reported by PITest for this method are:\n\n");
            for (Mutation mutation : mutations) {
                sb.append("* Line ");
                sb.append(mutation.lineNumber());
                sb.append(": ");
                sb.append(mutation.mutationImpact());
                sb.append(" \"");
                sb.append(mutation.description());
                sb.append("\"\n");
            }
        }
    }

    /*
    private static boolean isAIAssistantAvailable() {
        return PluginManagerCore.isPluginInstalled(PluginId.getId("com.intellij.ml.llm"));
    }
     */

    private static FileMethod getSelectedFileMethod(@NotNull AnActionEvent e, int  line, PitExecutionRecorder recorder) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            editor.getContentComponent().requestFocusInWindow();

            // Get PSI information
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                psiFile.getVirtualFile().refresh(false, false);
                // Note that getting the offset/line from the CaretModel are unreliable given the diverse possible
                // set of focus states in the editor.
                int offset = editor.getDocument().getLineStartOffset(line);
                PsiMethod method = getPsiMethod(psiFile, offset);

                if (method == null) {
                    LOGGER.warn("No PsiMethod found in " + psiFile.getName() + " at offset " + offset + " for line " + line);
                } else {
                    VirtualFile file = psiFile.getVirtualFile();
                    FileMutations fileMutations = recorder.getFileMutations(file);
                    return new FileMethod(fileMutations, method.getName());
                }
            }
        }
        return null;
    }

    private static @Nullable PsiMethod getPsiMethod(PsiFile psiFile, int offset) {
        PsiElement element = psiFile.findElementAt(offset);

        if (element != null) {
            // Print the element hierarchy for debugging
            PsiElement parent = element.getParent();
            int depth = 0;
            while (parent != null && depth < 10) {
                if (parent instanceof PsiMethod) {
                    break;
                }
                parent = parent.getParent();
                depth++;
            }
        }

        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }

}
