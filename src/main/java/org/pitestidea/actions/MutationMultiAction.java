package org.pitestidea.actions;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pitestidea.psi.PackageWalker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MutationMultiAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        Object @Nullable [] selectedNodes = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
        if (selectedNodes != null) {
            List<VirtualFile> virtualFiles = Arrays.stream(selectedNodes).map(n -> {
                if (n instanceof ProjectViewNode) {
                    VirtualFile file = ((ProjectViewNode<?>) n).getVirtualFile();
                    //System.out.format("READ %s, name=%s, type=%s%n", file,file.getName(),file.getFileType());
                    return ((ProjectViewNode<?>) n).getVirtualFile();
                }
                return null;
            }).filter(Objects::nonNull).toList();

            PITestRunProfile runProfile = new PITestRunProfile(project);
            PackageWalker.read(project, virtualFiles, runProfile);

            ExecutionUtils.execute(project, runProfile);

        }

        /*
        ProjectViewNode<?> selectedNode = (ProjectViewNode<?>) selectedNodes[0];
        if (selectedNode == null || selectedNode.getVirtualFile() == null) {
            return;
        }
        //selectedNode.
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(selectedNode.getVirtualFile());
        if (directory == null) {
            return;
        }

        PackageWalker walker = new PackageWalker();
        PackageDumper dumper = new PackageDumper();
        PackageWalker.read(project)
*/
    }

    //@Override
    public void actionPerformed1(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        // Get selected directory or package from the context
        //ProjectViewNode<?> selectedNode = event.getData(ProjectViewNode.DATA_KEY);
        //ProjectViewNode<?> selectedNode = event.getData(CommonDataKeys.NAVIGATABLE);
        Object @Nullable [] selectedNodes = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
        //ProjectViewNode<?> selectedNode = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
        ProjectViewNode<?> selectedNode = (ProjectViewNode<?>) selectedNodes[0];
        if (selectedNode == null || selectedNode.getVirtualFile() == null) {
            return;
        }
        //selectedNode.
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(selectedNode.getVirtualFile());
        if (directory == null) {
            return;
        }

        PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
        //PsiPackage selectedPackage = directory.getPackage();
        PsiPackage selectedPackage = JavaDirectoryService.getInstance().getPackage(directory);
        if (selectedPackage == null) {
            return;
        }

        // Update the ToolWindow with package contents
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("Package Contents");
        if (toolWindow == null) {
            return;
        }

        // Get sub-packages, e.g. "foozle.doozle"
        PsiPackage[] subPackages = psiPackage.getSubPackages();
        System.out.println("Sub-packages in " + psiPackage.getQualifiedName() + ":");
        for (PsiPackage subPackage : subPackages) {
            System.out.println("- " + subPackage.getQualifiedName());
        }

        // Get classes, e.g. "foozle.Foo", "foozle.FooTest"
        PsiClass[] classes = psiPackage.getClasses();
        System.out.println("Classes in " + psiPackage.getQualifiedName() + ":");
        for (PsiClass psiClass : classes) {
            System.out.println("- " + psiClass.getQualifiedName());
        }

        // Get files in the package, e.g. "Foo.java", "FooTest.java"
        PsiDirectory[] directories = psiPackage.getDirectories();
        System.out.println("Files in " + psiPackage.getQualifiedName() + ":");
        for (PsiDirectory dir : directories) {
            for (PsiFile psiFile : dir.getFiles()) {
                System.out.println("- " + psiFile.getName());
            }
        }

        /*
        PackageContentsToolWindowFactory.PackageContentsPanel panel =
                (PackageContentsToolWindowFactory.PackageContentsPanel) toolWindow.getContentManager()
                        .getContent(0).getComponent();

        String[] contents = selectedPackage.getDirectories().stream()
                .map(dir -> dir.getVirtualFile().getName())
                .toArray(String[]::new);

        panel.populateTree(selectedPackage.getQualifiedName(), contents);

         */

        // Ensure the tool window is visible
        toolWindow.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        /*
        // Enable only when clicking on a package node
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        ProjectViewNode<?> selectedNode = e.getData(ProjectViewNode.DATA_KEY);
        boolean isPackage = selectedNode != null && PsiManager.getInstance(project).findDirectory(selectedNode.getVirtualFile())
                .getPackage() != null;

        e.getPresentation().setEnabledAndVisible(isPackage);
         */
    }
}
