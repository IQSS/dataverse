package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import javax.ejb.Stateless;

@Stateless
public class VocabSelectEnhancedInputFieldRendererFactory implements InputFieldRendererFactory<VocabSelectEnhancedInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_ENHANCED_SELECT}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.VOCABULARY_ENHANCED_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public VocabSelectEnhancedInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        return new VocabSelectEnhancedInputFieldRenderer();
    }
}
