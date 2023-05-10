package io.gdcc.spi.export;

/**
 *
 * @author Leonid Andreev
 */
public class ExportException extends Exception {
    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
