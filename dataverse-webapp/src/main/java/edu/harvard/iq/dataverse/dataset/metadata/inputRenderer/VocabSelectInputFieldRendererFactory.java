package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import javax.ejb.Stateless;

@Stateless
public class VocabSelectInputFieldRendererFactory implements InputFieldRendererFactory<VocabSelectInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public VocabSelectInputFieldRenderer createRenderer(JsonObject jsonOptions) {
        return new VocabSelectInputFieldRenderer();
    }

}
