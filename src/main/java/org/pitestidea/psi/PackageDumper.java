package org.pitestidea.psi;

public class PackageDumper implements IPackageCollector {
    private void print(String fmt, Object... args) {
        System.out.printf(">> " + fmt, args);
    }

    @Override
    public void acceptCodePackage(String pkg) {
        print("codePkg: %s%n", pkg);
    }

    @Override
    public void acceptCodeClass(String className, String fileName) {
        print("codeClass: %s, file=%s%n", className, fileName);
    }

    @Override
    public void acceptTestPackage(String pkg) {
        print("testPkg: %s%n", pkg);
    }

    @Override
    public void acceptTestClass(String className) {
        print("testClass: %s%n", className);
    }
}
