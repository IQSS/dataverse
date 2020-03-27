package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import javax.ejb.Stateless;

@Stateless
public class TextboxInputFieldRendererFactory implements InputFieldRendererFactory<TextboxInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#TEXTBOX}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.TEXTBOX;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public TextboxInputFieldRenderer createRenderer(JsonObject jsonOptions) {
        return new TextboxInputFieldRenderer();
    }

}
