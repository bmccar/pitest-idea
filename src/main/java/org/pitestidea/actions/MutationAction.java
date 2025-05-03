package org.pitestidea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.configuration.IdeaDiscovery;

import java.util.ArrayList;
import java.util.List;

public class MutationAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PsiJavaFile javaFile = IdeaDiscovery.getCurrentJavaFile();
        Module module = ModuleUtilCore.findModuleForPsiElement(javaFile);

        String pkg = IdeaDiscovery.getCurrentPackageName();
        String cn = IdeaDiscovery.getCurrentClassName();
        String tn = IdeaDiscovery.getCurrentTestClassName();

        List<VirtualFile> virtualFiles = new ArrayList<>();
        virtualFiles.add(IdeaDiscovery.getCurrentFile());
        PITestRunProfile runProfile = new PITestRunProfile(project, module, virtualFiles);
        String pfx = StringUtils.isEmpty(pkg) ? "" : pkg + ".";
        String qn = pfx + cn;

        // Either the file or its test will match; sort out which is which
        final String sfx = "Test";
        final String testName;
        if (qn.endsWith(sfx)) {
            testName = qn;
            qn = qn.substring(0, qn.length() - 4);
        } else {
            testName = qn + sfx;
        }

        runProfile.acceptCodeClass(qn, null);
        runProfile.acceptTestClass(testName);

        ExecutionUtils.execute(project, module, runProfile);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || selectedFile == null) {
            e.getPresentation().setEnabledAndVisible(false);
        } else {
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            final String text;
            if (fileIndex.isInSourceContent(selectedFile)) {
                text = "Run PITest against this file using its test";
            } else if (fileIndex.isInTestSourceContent(selectedFile)) {
                text = "Run PITest for this test file against its source";
            } else {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setText(text);
            e.getPresentation().setEnabledAndVisible(true);
        }
    }
}

