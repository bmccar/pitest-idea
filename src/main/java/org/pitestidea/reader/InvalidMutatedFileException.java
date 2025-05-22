package org.pitestidea.reader;

public class InvalidMutatedFileException extends Exception {
    public InvalidMutatedFileException(String message) {
        super(message);
    }
    public InvalidMutatedFileException(Exception e) {
        super(e);
    }
}
