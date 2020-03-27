package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

/**
 * Base interface for every {@link InputFieldRenderer} factory.
 * 
 * @author madryk
 *
 * @param <T> - type of {@link InputFieldRenderer} produced by this factory
 */
public interface InputFieldRendererFactory<T extends InputFieldRenderer> {

    /**
     * Returns {@link InputRendererType} of
     * renderers that are produced by this factory
     * <p>
     * Note that value must match {@link InputFieldRenderer#getType()}
     * of renderers produced by this factory.
     */
    InputRendererType isFactoryForType();
    
    /**
     * Creates {@link InputFieldRenderer}
     * @param rendererOptions - options specific to renderer of type T
     * @return created input renderer 
     */
    T createRenderer(JsonObject rendererOptions);
}
