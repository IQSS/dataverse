package io.gdcc.solrteur.mdb.tsv;

import java.util.ArrayList;
import java.util.List;

public final class ParserException extends Throwable {
    
    private final List<ParserException> subExceptions = new ArrayList<>(0);
    private int lineNumber = -1;
    
    public ParserException(String message) {
        super(message);
    }
    
    public boolean hasSubExceptions() {
        return !subExceptions.isEmpty();
    }
    
    public void addSubException(String message) {
        this.subExceptions.add(new ParserException(message));
    }
    
    public void addSubException(ParserException ex) {
        this.subExceptions.add(ex);
    }
    
    public List<ParserException> getSubExceptions() {
        return List.copyOf(subExceptions);
    }
    
    public String getLineNumber() {
        return lineNumber > 0 ? ":"+lineNumber : "";
    }
    
    public ParserException withLineNumber(int lineIndex) {
        this.lineNumber = lineIndex + 1;
        return this;
    }
}
