package org.pitestidea.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.pitestidea.model.InputBundle.Category;

import static org.junit.jupiter.api.Assertions.*;

class InputBundleTest {

    private static final int MAX = InputBundle.MAX_REPORT_NAME_LENGTH;
    private static final String FS = File.separator;

    @Test
    void emptyBundleFails() {
        InputBundle bundle = new InputBundle();
        assertThrows(IllegalStateException.class, bundle::generateReportName,
                "Expected exception for empty bundle");
    }

    private static String biggerThanMax() {
        StringBuilder sb = new StringBuilder("x");
        while (sb.length() <= InputBundleTest.MAX) {
            sb.append('*');
        }
        return sb.toString();
    }

    private void verifyReportDirectoryName(String fn, String exp) {
        final int max_pfx = InputBundle.MAX_PREFIX_LENGTH;
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_FILE, fn);
        String got = bundle.generateReportName();
        got = got.substring(0, max_pfx);
        assertEquals(exp, got);
    }

    void verifyReportDirectoryName(String fn) {
        final int max_pfx = InputBundle.MAX_PREFIX_LENGTH;
        final String exp = fn.substring(0, max_pfx);
        verifyReportDirectoryName(fn, exp);
        verifyReportDirectoryName("x" + FS + fn, exp);
        verifyReportDirectoryName(path("a","x",fn), exp);
    }

    @Test
    void reportDirectoryName() {
        verifyReportDirectoryName("abc.x");
        verifyReportDirectoryName("abc");
    }

    private static String path(String...segments) {
        return String.join(FS, Arrays.asList(segments));
    }

    @Test
    void oneFileName() {
        String name = "Foo.x";
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_FILE, name).addPath(Category.TEST_FILE, "Junk.y");
        assertEquals("Foo", bundle.generateReportName());
        bundle = new InputBundle().addPath(Category.SOURCE_FILE, path("a",name));
        assertEquals("Foo", bundle.generateReportName());
    }

    @Test
    void noSourcesName() {
        String name = "FooTest.x";
        InputBundle bundle = new InputBundle().addPath(Category.TEST_FILE, name);
        assertEquals(1,bundle.asPath().get(Category::isFile).size());
        assertEquals(0,bundle.asPath().get(Category::isPkg).size());
        assertEquals("FooTest", bundle.generateReportName());
        bundle = new InputBundle().addPath(Category.TEST_FILE, path("a",name));
        assertEquals("FooTest", bundle.generateReportName());
    }

    @Test
    void oneDirectoryName() {
        String name = "Foo";
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_PKG, name);
        assertEquals(0,bundle.asPath().get(Category::isFile).size());
        assertEquals(1,bundle.asPath().get(Category::isPkg).size());
        assertEquals("Foo", bundle.generateReportName());
        bundle = new InputBundle().addPath(Category.SOURCE_PKG, path("a",name));
        assertEquals("Foo", bundle.generateReportName());
    }

    @Test
    void twoFileNames() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "Foo.x")
                .addPath(Category.SOURCE_FILE, "Bar.y");
        assertEquals("Bar,Foo", bundle.generateReportName());
    }

    @Test
    void truncateLongFileName() {
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_PKG, biggerThanMax() + ".z");
        String exp = biggerThanMax().substring(0, MAX) + "...";
        assertEquals(exp, bundle.generateReportName());
    }

    @Test
    void truncateLongMultiFile() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_PKG, "aa")
                .addPath(Category.SOURCE_PKG, "bb");
        assertEquals("a...", bundle.generateReportName(1));
        assertEquals("aa,...", bundle.generateReportName(2));
        assertEquals("aa,...", bundle.generateReportName(3));
        assertEquals("aa,b...", bundle.generateReportName(4));
        assertEquals("aa,bb", bundle.generateReportName(5));
    }

    @Test
    void reportNameOrder() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_PKG, "a/x")
                .addPath(Category.SOURCE_FILE, "c")
                .addPath(Category.SOURCE_PKG, "b/y");
        assertEquals("c,x,y", bundle.generateReportName());
    }

    @Test
    void rejectLeadingSlash() {
        String bad = "/a/b/c.x";
        assertThrows(IllegalArgumentException.class,
                ()->new InputBundle().addPath(Category.SOURCE_FILE, bad));
        assertThrows(IllegalArgumentException.class,
                ()->new InputBundle().setPaths(Category.SOURCE_FILE, List.of(bad)));
    }

    @Test
    void reformat() {
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_FILE, "a/b/c.x");
        assertEquals(List.of("a/b/c.x"),bundle.asPath().get(c->true));
        assertEquals(List.of("a.b.c"),bundle.asQn().get(c->true));
        assertEquals(List.of("c"),bundle.asSimple().get(c->true));
    }

    @Test
    @DisplayName("Should fail when list contains null elements")
    void failWhenListContainsNullElements() {
        InputBundle bundle = new InputBundle();
        List<String> pathsWithNull = Arrays.asList("valid/path", null, "another/path");

        assertThrows(NullPointerException.class, () ->
                bundle.setPaths(Category.SOURCE_FILE, pathsWithNull));
    }

    @Test
    @DisplayName("Should fail with zero or negative maxLength")
    void failWithZeroOrNegativeMaxLength() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "Test.java");

        assertThrows(Exception.class, () ->
                bundle.generateReportName(0));

        assertThrows(Exception.class, () ->
                bundle.generateReportName(-1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "\\"})
    void failWithWrongFileSeparators(String separator) {
        try (MockedStatic<SysDiffs> sysDiffsMockedStatic = org.mockito.Mockito.mockStatic(SysDiffs.class)) {
            sysDiffsMockedStatic.when(SysDiffs::fs).thenReturn(separator.charAt(0));
            sysDiffsMockedStatic.when(SysDiffs::fss).thenReturn(separator);

            InputBundle bundle = new InputBundle();

            String path = "com" + separator + "example" + separator + "Test.java";
            bundle.addPath(Category.SOURCE_FILE, path);

            String reportName = bundle.generateReportName();
            assertEquals("Test", reportName);
            assertEquals(bundle.asQn().get(Category::isSource), List.of("com.example.Test"));
            assertEquals(bundle.asPath().get(Category::isSource), List.of(path));
        }
    }

    @Test
    void shouldFailWithEmptyFileSeparatorEdgeCase() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, ".java"); // No actual name, just extension

        assertThrows(IllegalStateException.class, bundle::generateReportName);
    }

    @Test
    @DisplayName("Should fail with hash code collision in directory name generation")
    void failWithHashCodeCollision() {
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
    void failEmptyFileType() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "file."); // Ends with a dot but no extension

        String result = bundle.generateReportName();

        assertEquals("file", result);
    }

    @Test
    void shortFileName() {
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_FILE, "a"); // Single character, no extension

        String result = bundle.generateReportName();
        assertEquals("a", result);

        String dirName = bundle.generateReportDirectoryName();
        assertTrue(dirName.contains("a"));
    }
}