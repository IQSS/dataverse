package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

/**
 * Interface that contains instructions on how
 * to render dataset fields while editing metadata on ui.
 * 
 * @author madryk
 */
public interface InputFieldRenderer {
    
    /**
     * Returns {@link InputRendererType} associated
     * with this renderer.
     * <p>
     * It is important that each
     * implementation of {@link InputFieldRenderer} returns
     * different {@link InputRendererType} in whole application.
     */
    InputRendererType getType();
    
    /**
     * Returns true if input field should render in a
     * way that enables other input field to render
     * on the second column.
     * Otherwise input field should render in
     * full width.
     */
    boolean renderInTwoColumns();
}
