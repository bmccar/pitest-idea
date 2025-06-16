package org.pitestidea.model;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;

public class SysDiffs {
    public static char fs() {
        return File.separatorChar;
    }
    @VisibleForTesting
    public static String fss() {
        return File.separator;
    }

    /**
     * Returns "c" from "a/b/c".
     *
     * @param path to read
     * @return last segment of a multi-segment path
     */
    public static String lastSegmentOf(String path) {
        int ix = path.lastIndexOf(fs());
        if (ix >= 0) {
            return path.substring(ix + 1);
        }
        return path;
    }
}
