package org.pitestidea.psi;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
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
        //System.out.format("READ %s, name=%s, type=%s, isDir=%b%n", file,file.getName(),file.getFileType(),file.isDirectory());

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
            collector.acceptCodePackage(pkg);
            collector.acceptTestPackage(pkg);

        } else if (file.getFileType() == JavaFileType.INSTANCE) {
            PsiJavaFile psiFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(file);
            if (psiFile != null) {
                PsiClass @NotNull [] classes = psiFile.getClasses();
                if (classes.length > 0) {
                    PsiClass psiClass = classes[0];
                    String qn = psiFile.getPackageName() + '.' + psiClass.getName();
                    collector.acceptCodeClass(qn, psiFile.getName());

                    // TODO robustify
                    collector.acceptTestClass(qn + "Test");
                }
            }
        }

        /*
            // Get sub-packages, e.g. "foozle.doozle"
            PsiPackage[] subPackages = psiPackage.getSubPackages();
            //System.out.println("Sub-packages in " + psiPackage.getQualifiedName() + ":");
            for (PsiPackage subPackage : subPackages) {
                String pkg = subPackage.getQualifiedName();
                collector.acceptCodePackage(pkg);
                collector.acceptTestPackage(pkg);
                //subPackage.getContainingFile().getVirtualFile();
                //System.out.println("- " + subPackage.getQualifiedName());
            }
        // Get files in the package, e.g. "Foo.java", "FooTest.java"
        PsiDirectory[] directories = psiPackage.getDirectories();
        //System.out.println("Files in " + psiPackage.getQualifiedName() + ":");
        for (PsiDirectory dir : directories) {
            for (PsiFile psiFile : dir.getFiles()) {
                psifile.get
                collector.accept(psiPackage, psiFile);
                //System.out.println("- " + psiFile.getName());
            }
        }
         */
    }
}
