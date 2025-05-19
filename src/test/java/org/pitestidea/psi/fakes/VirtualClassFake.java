package org.pitestidea.psi.fakes;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class VirtualClassFake extends BaseVirtualFileFake {
    VirtualClassFake(VirtualPkgFake parent, String name) {
        super(parent, name);
    }

    @Override
    public @NotNull FileType getFileType() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @Override
    public String getQualifiedPath() {
        String qp = super.getQualifiedPath();
        int lastDot = qp.lastIndexOf('.');
        if (lastDot >= 0) {
            return qp.substring(0, lastDot);
        }
        return qp;
    }

    static class Terminal extends VirtualClassFake {
        public Terminal(VirtualPkgFake parent) {
            super(parent, "Should never see this");
        }
    }
}
