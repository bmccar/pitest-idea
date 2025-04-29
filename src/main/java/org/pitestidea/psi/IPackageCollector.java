package org.pitestidea.psi;

public interface IPackageCollector {
    void acceptCodePackage(String pkg);
    void acceptCodeClass(String qualifiedClassName, String fileName);

    void acceptTestPackage(String pkg);
    void acceptTestClass(String qualifiedClassName);
}
