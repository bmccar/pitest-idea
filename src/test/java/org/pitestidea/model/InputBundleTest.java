package org.pitestidea.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.pitestidea.model.InputBundle.Category;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.*;

class InputBundleTest {

    private static final int MAX = InputBundle.MAX_REPORT_NAME_LENGTH;
    private static final String FS = File.separator;

    @Test
    @DisplayName("Should throw IllegalStateException when bundle is empty")
    void emptyBundleFails() {
        InputBundle bundle = new InputBundle();
        assertThrows(IllegalStateException.class, bundle::generateReportName,
                "Expected exception for empty bundle");
    }

    private static String biggerThanMax() {
        return "x".repeat(MAX + 1);
    }

    @ParameterizedTest
    @CsvSource({
            "abc.x, abc",
            "abc, abc"
    })
    @DisplayName("Should generate correct report directory name prefix")
    void reportDirectoryName(String fileName, String expectedPrefix) {
        final int max_pfx = InputBundle.MAX_PREFIX_LENGTH;
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_FILE, fileName);
        String got = bundle.generateReportName();
        got = got.substring(0, Math.min(got.length(), max_pfx));
        assertEquals(expectedPrefix, got);

        bundle = new InputBundle().addPath(Category.SOURCE_FILE, "x" + FS + fileName);
        got = bundle.generateReportName();
        got = got.substring(0, Math.min(got.length(), max_pfx));
        assertEquals(expectedPrefix, got);

        bundle = new InputBundle().addPath(Category.SOURCE_FILE, path("a", "x", fileName));
        got = bundle.generateReportName();
        got = got.substring(0, Math.min(got.length(), max_pfx));
        assertEquals(expectedPrefix, got);
    }

    private static String path(String... segments) {
        return String.join(FS, segments);
    }

    @ParameterizedTest
    @CsvSource({
            "SOURCE_FILE, Foo.x, Foo",
            "TEST_FILE, FooTest.x, FooTest",
            "SOURCE_PKG, Foo, Foo",
            "SOURCE_FILE, a/Foo.x, Foo",
            "TEST_FILE, a/FooTest.x, FooTest",
            "SOURCE_PKG, a/Foo, Foo",
            "SOURCE_FILE, a.b/c, c",
    })
    @DisplayName("Should generate correct report name for single path")
    void singlePathNameGeneration(Category category, String path, String expectedName) {
        InputBundle bundle = new InputBundle().addPath(category, path);
        assertEquals(expectedName, bundle.generateReportName());
    }

    @Test
    @DisplayName("Should combine names for multiple source files")
    void twoFileNames() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "Foo.x")
                .addPath(Category.SOURCE_FILE, "Bar.y");
        assertEquals("Bar,Foo", bundle.generateReportName());
    }

    @Test
    @DisplayName("Should truncate a long file name")
    void truncateLongFileName() {
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_PKG, biggerThanMax() + ".z");
        String expected = biggerThanMax().substring(0, MAX) + "...";
        assertEquals(expected, bundle.generateReportName());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 'a...'",
            "2, 'aa,...'",
            "3, 'aa,...'",
            "4, 'aa,b...'",
            "5, 'aa,bb'"
    })
    @DisplayName("Should truncate long multi-file name at specified length")
    void truncateLongMultiFile(int maxLength, String expected) {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_PKG, "aa")
                .addPath(Category.SOURCE_PKG, "bb");
        assertEquals(expected, bundle.generateReportName(maxLength));
    }

    @Test
    @DisplayName("Should correctly order report names")
    void reportNameOrder() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_PKG, "a/x")
                .addPath(Category.SOURCE_FILE, "c")
                .addPath(Category.SOURCE_PKG, "b/y");
        assertEquals("c,x,y", bundle.generateReportName());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for paths starting with a slash")
    void rejectLeadingSlash() {
        String bad = "/a/b/c.x";
        assertThrows(IllegalArgumentException.class,
                () -> new InputBundle().addPath(Category.SOURCE_FILE, bad));
        assertThrows(IllegalArgumentException.class,
                () -> new InputBundle().setPaths(Category.SOURCE_FILE, List.of(bad)));
    }

    @Test
    @DisplayName("Should reformat paths to qualified names and simple names")
    void reformat() {
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_FILE, "a/b/c.x");
        assertEquals(List.of("a/b/c.x"), bundle.asPath().get(c -> true));
        assertEquals(List.of("a.b.c"), bundle.asQn().get(c -> true));
        assertEquals(List.of("c"), bundle.asSimple().get(c -> true));
    }

    @Test
    @DisplayName("Should throw NullPointerException when list contains null elements")
    void failWhenListContainsNullElements() {
        InputBundle bundle = new InputBundle();
        List<String> pathsWithNull = Arrays.asList("valid/path", null, "another/path");

        assertThrows(NullPointerException.class, () ->
                bundle.setPaths(Category.SOURCE_FILE, pathsWithNull));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Should throw IllegalArgumentException for zero or negative maxLength")
    void failWithZeroOrNegativeMaxLength(int maxLength) {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "Test.java");

        assertThrows(IllegalArgumentException.class, () ->
                bundle.generateReportName(maxLength));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "\\"})
    @DisplayName("Should handle different file separators")
    void handleDifferentFileSeparators(String separator) {
        try (MockedStatic<SysDiffs> sysDiffsMockedStatic = org.mockito.Mockito.mockStatic(SysDiffs.class)) {
            sysDiffsMockedStatic.when(SysDiffs::fs).thenReturn(separator.charAt(0));
            sysDiffsMockedStatic.when(SysDiffs::fss).thenReturn(separator);

            InputBundle bundle = new InputBundle();

            String path = "com" + separator + "example" + separator + "Test.java";
            bundle.addPath(Category.SOURCE_FILE, path);

            String reportName = bundle.generateReportName();
            assertEquals("Test", reportName);
            assertEquals(List.of("com.example.Test"), bundle.asQn().get(Category::isSource));
            assertEquals(List.of(path), bundle.asPath().get(Category::isSource));
        }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when file name is only an extension")
    void failWhenFileNameIsOnlyAnExtension() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, ".java"); // No actual name, just extension

        assertThrows(IllegalStateException.class, bundle::generateReportName);
    }

    @Test
    @DisplayName("Should generate unique directory names for different inputs")
    void shouldGenerateUniqueDirectoryNamesForDifferentInputs() {
        // Create two different bundles that might have the same hash code
        InputBundle bundle1 = new InputBundle()
                .addPath(Category.SOURCE_FILE, "Aa.java");

        InputBundle bundle2 = new InputBundle()
                .addPath(Category.SOURCE_FILE, "BB.java");

        String dir1 = bundle1.generateReportDirectoryName();
        String dir2 = bundle2.generateReportDirectoryName();

        assertNotEquals(dir1, dir2);
    }

    @Test
    @DisplayName("Should handle file name ending with a dot")
    void handleFileNameEndingWithDot() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "file."); // Ends with a dot but no extension

        String result = bundle.generateReportName();

        assertEquals("file", result);
    }

    @Test
    @DisplayName("Should handle single character file name")
    void shortFileName() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "a"); // Single character, no extension

        String result = bundle.generateReportName();
        assertEquals("a", result);

        String dirName = bundle.generateReportDirectoryName();
        assertTrue(dirName.contains("a"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    @DisplayName("Should throw for empty or blank paths")
    void shouldThrowForEmptyOrBlankPaths(String invalidPath) {
        InputBundle bundle = new InputBundle();
        assertThatIllegalArgumentException().isThrownBy(() ->
                bundle.addPath(Category.SOURCE_FILE, invalidPath));
    }
}