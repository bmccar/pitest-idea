package org.pitestidea.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.pitestidea.model.InputBundle.Category;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRecordTest {

    @Test
    void missingMetaFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.toFile();
        Files.write(tempDir.resolve("junk"), List.of("<a>b</a>"));
        assertThrows(ExecutionRecord.InvalidFile.class, () -> new ExecutionRecord(file));
    }

    @Test
    void readCorrupted(@TempDir Path tempDir) throws IOException {
        File file = tempDir.toFile();
        Files.write(tempDir.resolve(ExecutionRecord.META_FILE_NAME), List.of("<a>b</a>"));
        assertThrows(ExecutionRecord.InvalidFile.class, () -> new ExecutionRecord(file));
    }

    @Test
    void writeRead(@TempDir Path tempDir) {
        File file = tempDir.toFile();
        InputBundle bundle = new InputBundle()
                .addPath(Category.SOURCE_PKG, "a")
                .addPath(Category.SOURCE_PKG, "b")
                .addPath(Category.SOURCE_PKG, "c");
        ExecutionRecord record = new ExecutionRecord(bundle);
        record.writeToDirectory(file);
        ExecutionRecord read = new ExecutionRecord(file);
        assertEquals(record, read);
        assertEquals(record.getStartedAt(), read.getStartedAt());
        assertEquals(record.getFormattedDuration(), read.getFormattedDuration());
    }

    @Test
    void eqTest() {
        InputBundle bundle1 = new InputBundle()
                .addPath(Category.SOURCE_FILE, "a.x")
                .addPath(Category.TEST_PKG, "b")
                .addPath(Category.SOURCE_FILE, "c.y");
        InputBundle bundle2 = new InputBundle()
                .addPath(Category.SOURCE_FILE, "c.y")
                .addPath(Category.SOURCE_FILE, "a.x")
                .addPath(Category.TEST_PKG, "b");

        assertEquals(new ExecutionRecord(bundle1), new ExecutionRecord(bundle2));
        bundle1.addPath(Category.SOURCE_FILE, "d.z");
        assertNotEquals(new ExecutionRecord(bundle1), new ExecutionRecord(bundle2));
    }
}