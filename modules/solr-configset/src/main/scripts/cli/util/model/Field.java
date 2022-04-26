package cli.util.model;

import java.util.List;
import java.util.Optional;

public class Field {
    public static final String TRIGGER = Constants.TRIGGER_INDICATOR + "datasetField";
    
    private Field() {}
    
    Optional<List<ControlledVocabulary>> controlledVocabularyValues = Optional.empty();
}
