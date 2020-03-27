package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import javax.ejb.Stateless;

@Stateless
public class HtmlMarkupInputFieldRendererFactory implements InputFieldRendererFactory<HtmlMarkupInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HTML_MARKUP}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.HTML_MARKUP;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public HtmlMarkupInputFieldRenderer createRenderer(JsonObject rendererOptions) {
        return new HtmlMarkupInputFieldRenderer();
    }

}
