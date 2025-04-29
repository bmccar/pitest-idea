package org.pitestidea.psi;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PackageWalker {
    public static void read(Project project, List<VirtualFile> files, IPackageCollector collector) {
        for (VirtualFile file : files) {
            read(project, file, collector);
        }
    }

    private static void read(Project project, VirtualFile file, IPackageCollector collector) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        final boolean isInSrc = fileIndex.isInSourceContent(file);
        final boolean isInTest = fileIndex.isInTestSourceContent(file);

        if (file.isDirectory()) {
            PsiDirectory directory = PsiManager.getInstance(project).findDirectory(file);
            if (directory == null) {
                return;
            }

            PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
            if (psiPackage == null) {
                return;
            }

            String pkg = psiPackage.getQualifiedName();
            if (isInSrc && !isInTest) {
                collector.acceptCodePackage(pkg);
                collector.acceptTestPackage(pkg);
            } else if (isInTest) {
                collector.acceptTestPackage(pkg);
            }

        } else if (file.getFileType() == JavaFileType.INSTANCE) {
            PsiJavaFile psiFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(file);
            if (psiFile != null) {
                PsiClass @NotNull [] classes = psiFile.getClasses();
                if (classes.length > 0) {
                    PsiClass psiClass = classes[0];
                    String qn = psiFile.getPackageName() + '.' + psiClass.getName();
                    if (isInTest) {
                        collector.acceptTestClass(qn);
                    } else if (isInSrc){
                        collector.acceptCodeClass(qn, psiFile.getName());
                        collector.acceptTestClass(qn + "Test");
                    }
                }
            }
        }
    }
}
