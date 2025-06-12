package org.pitestidea.model;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;

public class SysDiffs {
    static char fs() {
        return File.separatorChar;
    }
    @VisibleForTesting
    static String fss() {
        return File.separator;
    }
}
