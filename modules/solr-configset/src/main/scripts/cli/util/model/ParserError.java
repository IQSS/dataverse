package cli.util.model;

public final class ParserError {
    String message;
    Integer lineNumber;
    
    public ParserError(String message, Integer lineNumber) {
        this.message = message;
        this.lineNumber = lineNumber;
    }
    
    public ParserError(ParserException exception, Integer lineNumber) {
        this.message = exception.getMessage();
        this.lineNumber = lineNumber;
    }
}
