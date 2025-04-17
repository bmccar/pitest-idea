package org.pitestidea.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRecordTest {

    private static int MAX = ExecutionRecord.MAX_REPORT_NAME_LENGTH;
    private static String FS = File.separator;

    private static ExecutionRecord record(String...names) {
        return new ExecutionRecord(Arrays.asList(names));
    }

    private static String extendPast(String s, int len) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() <= len) {
            sb.append('*');
        }
        return sb.toString();
    }

    private void verifyReportDirectoryName(String fn, String exp) {
        final int max_pfx = ExecutionRecord.MAX_PREFIX_LENGTH;
        ExecutionRecord record = record(fn);
        String got = record.getReportDirectoryName();
        got = got.substring(0, max_pfx);
        assertEquals(exp, got);
    }

    void verifyReportDirectoryName(String fn) {
        final int max_pfx = ExecutionRecord.MAX_PREFIX_LENGTH;
        final String exp = fn.substring(0, max_pfx);
        verifyReportDirectoryName(fn, exp);
        verifyReportDirectoryName("x"+FS + fn, exp);
        verifyReportDirectoryName(FS+"x"+FS + fn, exp);
    }

    @Test
    void reportDirectoryName() {
        verifyReportDirectoryName("abcdefg.x");
        verifyReportDirectoryName("abcdefg");
    }

    @Test
    void oneFileName() {
        ExecutionRecord record = record("Foo.x");
        assertEquals("Foo",record.getReportName());
        record = record(FS+"Foo.x");
        assertEquals("Foo",record.getReportName());
    }

    @Test
    void oneDirectoryName() {
        ExecutionRecord record = record("Foo");
        assertEquals("Foo",record.getReportName());
        record = record(FS+"Foo");
        assertEquals("Foo",record.getReportName());
    }

    @Test
    void twoFileNames() {
        ExecutionRecord record = record("Foo.x", "Bar.y");
        assertEquals("Foo,Bar",record.getReportName());
    }

    @Test
    void oneLongFileName() {
        ExecutionRecord record = record(extendPast("x", MAX)+".z");
        String exp = extendPast("x", MAX).substring(0,MAX) + "...";
        assertEquals(exp,record.getReportName());
    }

    @Test
    void longMix() {
        ExecutionRecord record = record("a/foozle", "/b/c/doozle","Bingo.x", "/c/d/e/zebra","abcdefghijklm.n");
        String exp = "foozle,doozle,Bingo,zebra,abcdefghijklm".substring(0, MAX+1) + ",...";
        assertEquals(exp,record.getReportName());
    }
}