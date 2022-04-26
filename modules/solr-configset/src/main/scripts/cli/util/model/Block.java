package cli.util.model;

import java.util.List;
import java.util.Optional;

public final class Block {
    public static final String TRIGGER = Constants.TRIGGER_INDICATOR + "metadataBlock";
    
    private Block() {}
    
    Optional<List<Field>> fields = Optional.empty();
}
