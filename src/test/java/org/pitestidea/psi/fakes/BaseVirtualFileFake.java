package org.pitestidea.psi.fakes;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The class hierarchy here serves to replace mocks for VirtualFiles, with additional properties:
 * <ul>
 *     <li>Way to reference fully qualified files without requiring a dense naming scheme</li>
 *     <li>Exposes whether a file is in a test directory or source directory</li>
 * </ul>
 */
public abstract class BaseVirtualFileFake extends VirtualFile {
    static public final String BASE_PKG = "just/testing/";
    private static final Map<String, BaseVirtualFileFake> pathMap = new HashMap<>();
    private static final Set<BaseVirtualFileFake> filesThatExist = new HashSet<>();

    final @Nullable VirtualPkgFake parent;
    final @NotNull String name;

    BaseVirtualFileFake(@Nullable VirtualPkgFake parent, @NotNull String name) {
        this.parent = parent;
        this.name = name;
        pathMap.put(getPath(),this);
    }

    public static VirtualFile findByPath(String path) {
        return pathMap.get(path);
    }

    public static void setExists(BaseVirtualFileFake[] files) {
        filesThatExist.clear();
        for (BaseVirtualFileFake file : files) {
            setExists(file);
        }
    }

    private static void setExists(BaseVirtualFileFake file) {
        if (file != null) {
            filesThatExist.add(file);
            setExists(file.getParent());
        }
    }

    @Override
    public final boolean exists() {
        return filesThatExist.contains(this);
    }

    public boolean isTest() {
        return getRoot().isTest();
    }

    public BaseVirtualFileFake getRoot() {
        return parent==null ? this : parent.getRoot();
    }

    public String getQualifiedPath() {
        if (parent == null || parent.getRoot() == parent) {
            return name;
        } else {
            return parent.getQualifiedPath() + '.' + name;
        }
    }

    public String getQualifiedPackageName() {
        if (parent == null) {
            return "";
        } else {
            return parent.getQualifiedPath();
        }
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getPath() {
        BaseVirtualFileFake pre = parent == null ? getRoot() : parent;
        return pre.getPath() + '/' + name;
    }

    public @NotNull String getRelativePath() {
        String pre = (parent == null || (parent instanceof RootFake))? "" : (parent.getRelativePath() + '/');
        return pre + name;
    }

    @Override
    public @Nullable VirtualPkgFake getParent() {
        return parent;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public byte @NotNull [] contentsToByteArray() {
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

    }

    @Override
    public @NotNull InputStream getInputStream() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String toString() {
        return "Fake{ " + getPath() + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof BaseVirtualFileFake that) {
            return this.getPath().equals(that.getPath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }
}
