package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.List;

public class TextInputFieldRenderer implements InputFieldRenderer {

    private FieldButtonActionHandler actionButtonHandler;
    private List<MetadataOperationSource> enableActionForOperations;
    private String actionButtonTextKey;
    
    
    // -------------------- CONSTRUCTORS --------------------
    
    /**
     * Constructs simple renderer (without additional action button)
     */
    public TextInputFieldRenderer() {
    }
    
    /**
     * Constructs renderer with support for action button.
     */
    public TextInputFieldRenderer(FieldButtonActionHandler actionButtonHandler, String actionButtonTextKey, List<MetadataOperationSource> enableActionForOperations) {
        this.actionButtonHandler = actionButtonHandler;
        this.enableActionForOperations = enableActionForOperations;
        this.actionButtonTextKey = actionButtonTextKey;
    }
    
    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#TEXT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.TEXT;
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

    // -------------------- LOGIC --------------------

    public boolean hasActionButton() {
        return actionButtonHandler != null;
    }
    
    public boolean showActionButtonForOperation(String operation) {
        return enableActionForOperations.contains(MetadataOperationSource.valueOf(operation));
    }
    
    public String getActionButtonText() {
        return BundleUtil.getStringFromBundle(actionButtonTextKey);
    }
    
    public void executeButtonAction(DatasetField datasetField, List<DatasetFieldsByType> allBlockFields) {
        
        actionButtonHandler.handleAction(datasetField, allBlockFields);
    }
}
