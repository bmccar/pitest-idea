package org.pitestidea.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.pitestidea.model.InputBundle.Category;

class InputBundleTest {

    private static final int MAX = InputBundle.MAX_REPORT_NAME_LENGTH;
    private static final String FS = File.separator;

    private static String extendPast(String s) {
        StringBuilder sb = new StringBuilder(s);
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
        verifyReportDirectoryName("abcdefg.x");
        verifyReportDirectoryName("abcdefg");
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
        assertEquals("FooTest", bundle.generateReportName());
        bundle = new InputBundle().addPath(Category.TEST_FILE, path("a",name));
        assertEquals("FooTest", bundle.generateReportName());
    }

    @Test
    void oneDirectoryName() {
        String name = "Foo";
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_PKG, name);
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
    void oneLongFileName() {
        InputBundle bundle = new InputBundle().addPath(Category.SOURCE_PKG, extendPast("x") + ".z");
        String exp = extendPast("x").substring(0, MAX) + "...";
        assertEquals(exp, bundle.generateReportName());
    }

    @Test
    void reportNameTruncation() {
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
}