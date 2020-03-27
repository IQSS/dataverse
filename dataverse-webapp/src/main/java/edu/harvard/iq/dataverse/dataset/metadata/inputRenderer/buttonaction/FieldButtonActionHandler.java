package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.TextInputFieldRenderer;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;

import java.util.List;

/**
 * Handler of click events of additional button rendered
 * with {@link TextInputFieldRenderer}.
 * 
 * @author madryk
 */
public interface FieldButtonActionHandler {

    /**
     * Returns unique name of action handler.
     */
    String getName();
    
    /**
     * Defines action that will happen after field button has
     * been clicked.
     * 
     * @param datasetField - dataset field on which field button was clicked
     * @param allBlockFields - other fields from the same metadata block (can be used to
     *      obtain / change values )
     */
    void handleAction(DatasetField datasetField, List<DatasetFieldsByType> allBlockFields);
}
