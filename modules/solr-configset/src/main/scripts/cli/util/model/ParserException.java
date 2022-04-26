package cli.util.model;

import java.util.ArrayList;
import java.util.List;

public final class ParserException extends Throwable {
    
    final List<ParserException> subExceptions = new ArrayList<>(0);
    
    public ParserException(String message) {
        super(message);
    }
    
    public boolean hasSubExceptions() {
        return subExceptions.size() > 0;
    }
    
    public void addSubException(String message) {
        this.subExceptions.add(new ParserException(message));
    }
    
    public List<ParserException> getSubExceptions() {
        return List.copyOf(subExceptions);
    }
}
