package org.pitestidea.toolwindow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClickTreeTest {
    @Test
    void simpleLength() {
        assertEquals(3, ClickTree.contentLength("abc"));
    }

    @Test
    void htmlBlock() {
        assertEquals(3, ClickTree.contentLength("<html>abc</html>"));
    }

    @Test
    void htmlBlockWithItalics() {
        assertEquals(3, ClickTree.contentLength("<html>a<i>b</i>c</html>"));
    }

    @Test
    void htmlBlockWithLongSymbol() {
        assertEquals(3, ClickTree.contentLength("<html>a&#8595;c</html>"));
    }

    @Test
    void htmlBlockWithShortSymbol() {
        assertEquals(3, ClickTree.contentLength("<html>a&#85;c</html>"));
    }

    @Test
    void htmlBlockWithSpaces() {
        assertEquals(4, ClickTree.contentLength("<html>a&nbsp;&nbsp;c</html>"));
    }
}