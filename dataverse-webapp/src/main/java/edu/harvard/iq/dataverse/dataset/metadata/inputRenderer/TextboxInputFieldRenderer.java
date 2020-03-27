package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

public class TextboxInputFieldRenderer implements InputFieldRenderer {

    // -------------------- GETTERS --------------------
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#TEXTBOX}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.TEXTBOX;
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

}
