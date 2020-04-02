package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.ArrayList;
import java.util.List;

public class HiddenVocabInputFieldRenderer implements InputFieldRenderer {

    private List<ControlledVocabularyValue> defaultVocabValues = new ArrayList<>();
    
    // -------------------- CONSTRUCTORS --------------------
    
    public HiddenVocabInputFieldRenderer(List<ControlledVocabularyValue> defaultVocabValues) {
        this.defaultVocabValues = defaultVocabValues;
    }
    
    // -------------------- GETTERS --------------------
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HIDDEN_VOCABULARY}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.HIDDEN_VOCABULARY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
     */
    @Override
    public boolean renderInTwoColumns() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code true}
     */
    @Override
    public boolean isHidden() {
        return true;
    }

    /**
     * Returns {@link ControlledVocabularyValue}s that should
     * be assigned to dataset field by default
     */
    public List<ControlledVocabularyValue> getDefaultVocabValues() {
        return defaultVocabValues;
    }
    
}
