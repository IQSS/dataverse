package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    // -------------------- GETTERS --------------------
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code true}
     */
    @Override
    public boolean renderInTwoColumns() {
        return true;
    }

}
