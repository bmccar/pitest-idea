package org.pitestidea.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A bundle of input files for a PIT run.
 */
public class InputBundle {
    @VisibleForTesting
    static final int MAX_REPORT_NAME_LENGTH = 24;
    static final int MAX_PREFIX_LENGTH = 3;

    public enum Category {
        SOURCE_FILE(false, false, "source_file"),
        TEST_FILE(false, true, "test_file"),
        SOURCE_PKG(true, false, "source_pkg"),
        TEST_PKG(true, true, "test_pkg");

        private final boolean isPkg;
        private final boolean isTest;
        private final String serializableName;

        Category(boolean isPkg, boolean isTest, String serializableName) {
            this.isPkg = isPkg;
            this.isTest = isTest;
            this.serializableName = serializableName;
        }

        public String getSerializableName() {
            return serializableName;
        }

        public boolean isFile() {
            return !isPkg;
        }

        public boolean isPkg() {
            return isPkg;
        }

        public boolean isSource() {
            return !isTest;
        }

        public boolean isTest() {
            return isTest;
        }

        public boolean is(boolean isTest, boolean isPkg) {
            return this.isTest == isTest && this.isPkg == isPkg;
        }
    }

    private final Map<Category, Set<String>> map = new HashMap<>();

    {
        Arrays.stream(Category.values()).forEach(c -> map.put(c, new TreeSet<>()));
    }

    public abstract class Format {
        abstract Function<String, String> getTransformer();


        public List<String> get(Function<Category, Boolean> fn) {
            return transformPaths(fn, getTransformer());
        }

        /**
         * Applies two transformers to each element of the list returned by {@link #get(Function)}. Sorting
         * is done only after the first transform.
         *
         * @param fn to first apply
         * @param transformer to apply second
         * @return list after applying both transforms
         * @param <T> any type
         */
        public <T> List<T> transform(Function<Category, Boolean> fn, Function<String, T> transformer) {
            List<String> ss = get(fn);
            // 2nd transform applied independently because we're not sure that it's a Comparable and get()
            // does a sort that requires Comparable.
            return ss.stream().map(transformer).toList();
        }
    }

    class AsQn extends Format {
        @Override
        public Function<String, String> getTransformer() {
            return s -> {
                int lastDot = s.lastIndexOf('.');
                if (lastDot >= 0) {
                    s = s.substring(0, lastDot);
                }
                return s.replace(File.separatorChar, '.');
            };
        }
    }

    public Format asQn() {
        return new AsQn();
    }

    class AsSimple extends Format {
        @Override
        public Function<String, String> getTransformer() {
            return s -> {
                {
                    int lastDot = s.lastIndexOf('.');
                    if (lastDot >= 0) {
                        s = s.substring(0, lastDot);
                    }
                }
                {
                    int lastSlash = s.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        s = s.substring(lastSlash + 1);
                    }
                }
                return s;
            };
        }
    }

    public Format asSimple() {
        return new AsSimple();
    }

    class AsPath extends Format {
        @Override
        public Function<String, String> getTransformer() {
            return s -> s;
        }
    }

    public Format asPath() {
        return new AsPath();
    }

    private List<String> getPaths(Function<Category, Boolean> fn) {
        return Stream.of(Category.values()).filter(fn::apply)
                .map(map::get)
                .flatMap(Set::stream)
                .sorted()
                .toList();
    }

    private <T> List<T> transformPaths(Function<Category, Boolean> fn, Function<String, T> transformer) {
        return Stream.of(Category.values()).filter(fn::apply)
                .map(map::get)
                .flatMap(Set::stream)
                .map(transformer)
                .sorted()
                .toList();
    }

    public boolean isEmpty(Function<Category, Boolean> fn) {
        return Stream.of(Category.values()).filter(fn::apply)
                .map(map::get)
                .allMatch(Set::isEmpty);
    }

    public @NotNull InputBundle addPath(Category category, @NotNull String element) {
        if (element.startsWith("/")) {
            throw new IllegalArgumentException("Path must not start with a slash: " + element);
        }
        map.get(category).add(element);
        return this;
    }

    public void setPaths(@NotNull Category category, @NotNull List<String> elements) {
        if (elements.stream().anyMatch(s -> s.startsWith("/"))) {
            throw new IllegalArgumentException("No path must start with a slash: " + elements);
        }
        Set<String> list = map.get(category);
        list.clear();
        list.addAll(elements);
    }

    /**
     * Can only initiate a PIT run if there are both some source entries and some test entries.
     *
     * @return true iff this record can be run
     */
    public boolean isRunnable() {
        return !isEmpty(Category::isSource) && !isEmpty(Category::isTest);
    }

    public void appendHtmlListOfInputs(StringBuilder sb) {
        sb.append("<br>Source inputs for this run:");
        Format format = asQn();
        appendHtmlListOfInputs(sb, format.get(Category::isSource));
        sb.append("Test inputs:");
        appendHtmlListOfInputs(sb, format.get(Category::isTest));
    }

    private static void appendHtmlListOfInputs(StringBuilder sb, List<String> fileNames) {
        sb.append("<br><ul>");
        for (String inputFile : fileNames) {
            sb.append("<li>");
            sb.append(inputFile);
            sb.append("</li>");
        }
        sb.append("</ul>");
    }

    public String generateReportName() {
        return generateReportName(MAX_REPORT_NAME_LENGTH);
    }

    /**
     * Generates a readable name as a comma-separated list of source file names. The result is no longer
     * than maxLength, but a suffix of '...' or ',...' is appended if necessary to keep the name in range.
     *
     * @param maxLength to not exceed
     * @return readable report name
     */
    @VisibleForTesting
    String generateReportName(int maxLength) {
        // Prefer source files if any
        String name = generateShortName(asSimple().transform(Category::isSource, InputBundle::nameFrom), maxLength);
        if (name.isEmpty()) {
            // Else if no sources, use test files rather than an empty string
            name = generateShortName(asSimple().transform(Category::isTest, InputBundle::nameFrom), maxLength);
        }
        return name;
    }


    private static String generateShortName(List<String> source, int maxLength) {
        StringBuilder sb = new StringBuilder();
        for (String file : source) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            int charsLeft = maxLength - sb.length();
            if (charsLeft <= 0) {
                sb.append("...");
            } else {
                int len = file.length();
                if (len > charsLeft) {
                    sb.append(file, 0, charsLeft);
                    sb.append("...");
                } else {
                    sb.append(file);
                    continue;
                }
            }
            break;
        }
        return sb.toString();
    }

    private static String nameFrom(String relPath) {
        int from = relPath.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1;
        int to = relPath.lastIndexOf('.');
        if (to < 0) {
            to = relPath.length();
        }
        return relPath.substring(from, to);
    }

    public String generateReportDirectoryName() {
        return generateReportDirectoryName(asQn().get(Category::isSource));
    }

    private static String generateReportDirectoryName(List<String> qns) {
        String rn = String.valueOf(Math.abs(qns.hashCode()));
        if (!qns.isEmpty()) {
            String pfx = qns.get(0);
            int ix = pfx.lastIndexOf(FileSystems.getDefault().getSeparator());
            if (ix > 0) {
                // Add a prefix just to ease task of looking through files if/when necessary
                pfx = pfx.substring(ix + 1);
            }
            pfx = pfx.substring(0, Math.min(pfx.length(), MAX_PREFIX_LENGTH));
            rn = pfx + rn;
        }
        return rn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof InputBundle that) {
            return this.map.equals(that.map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    public int compareTo(@NotNull InputBundle that) {
        for (Category category : Category.values()) {
            Set<String> theseFiles = this.map.get(category);
            Set<String> thoseFiles = that.map.get(category);

            int sizeComparison = Integer.compare(theseFiles.size(), thoseFiles.size());
            if (sizeComparison != 0) {
                return sizeComparison;
            }

            Iterator<String> theseIter = theseFiles.iterator();
            Iterator<String> thoseIter = thoseFiles.iterator();
            while (theseIter.hasNext() && thoseIter.hasNext()) {
                String theseElement = theseIter.next();
                String thoseElement = thoseIter.next();
                int elementComparison = theseElement.compareTo(thoseElement);
                if (elementComparison != 0) {
                    return elementComparison;
                }
            }
        }
        return 0;
    }
}
