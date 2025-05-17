package org.pitestidea.psi.fakes;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

abstract public class VirtualPkgFake extends BaseVirtualFileFake {

    private final List<VirtualFile> files = new ArrayList<>();

    protected VirtualPkgFake(VirtualPkgFake parent, String name) {
        super(parent, name);
    }

    protected void setChildren(List<VirtualFile> files) {
        this.files.clear();
        this.files.addAll(files);
    }

    @Override
    public final boolean isDirectory() {
        return true;
    }

    @Override
    public final VirtualFile[] getChildren() {
        return files.toArray(new VirtualFile[0]);
    }
}
