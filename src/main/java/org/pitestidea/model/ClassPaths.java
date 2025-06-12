package org.pitestidea.model;

import java.util.List;

/**
 * Records class paths for display in the console popup.
 */
public class ClassPaths {
    private final List<String> classPaths;
    private final List<String> addedClassPaths;

    /**
     * Creates an instance that will show all classPaths when formatted, and those that are
     * also in addedClassPaths will be highlighted.
     *
     * @param classPaths to read
     * @param addedClassPaths to indicate which among classPaths should be highlighted.
     */
    public ClassPaths(List<String> classPaths, List<String> addedClassPaths) {
        this.classPaths = classPaths;
        this.addedClassPaths = addedClassPaths;
    }

    public String formatHtml() {
        StringBuilder sb = new StringBuilder();
        for (String classPath : classPaths) {
            sb.append("<div");
            if (addedClassPaths.contains(classPath)) {
                sb.append(" style='color:#80ff80'");
            }
            sb.append('>');
            sb.append(classPath);
            sb.append("</div>");
        }
        return sb.toString();
    }
}
