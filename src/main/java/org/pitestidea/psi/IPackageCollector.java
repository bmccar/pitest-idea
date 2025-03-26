package org.pitestidea.psi;

public interface IPackageCollector {
    void acceptCodePackage(String pkg);
    void acceptCodeClass(String className, String fileName);

    void acceptTestPackage(String pkg);
    void acceptTestClass(String className);
}
