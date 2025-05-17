package org.pitestidea.psi.fakes;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class RootFake extends VirtualPkgFake {
    public static final RootFake javaSrc = new RootFake("main", "java", false);
    public static final RootFake javaTest = new RootFake("test", "java", true);

    private final String loc;
    private final String lang;
    private final boolean isTest;

    public RootFake(String loc, String lang, boolean isTest) {
        super(null, lang);
        this.loc = loc;
        this.lang = lang;
        this.isTest = isTest;
    }

    public BaseVirtualFileFake getRoot() {
        return this;
    }

    public static VirtualFile[] getContentSourceRoots() {
        return new VirtualFile[]{javaSrc, javaTest};
    }

    @Override
    public @NonNls @NotNull String getPath() {
        return BaseVirtualFileFake.BASE_PKG + "src/" + loc + '/' + lang;
    }

    @Override
    public boolean isTest() {
        return isTest;
    }

    public boolean isRootTest() {
        return isTest;
    }
}
