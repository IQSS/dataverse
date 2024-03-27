package io.gdcc.spi.export;

import java.io.IOException;

public class ExportException extends IOException {
    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
