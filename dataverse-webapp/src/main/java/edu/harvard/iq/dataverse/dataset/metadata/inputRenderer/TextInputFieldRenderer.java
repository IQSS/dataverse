package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextInputFieldRenderer implements InputFieldRenderer {

    private FieldButtonActionHandler actionButtonHandler;
    private List<MetadataOperationSource> enableActionForOperations;
    private String actionButtonTextKey;

    private SuggestionHandler suggestionHandler;
    private List<String> suggestionFilteredBy;
    private String suggestionSourceField;


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

    /**
     * Constructs renderer with support for suggestions.
     */
    public TextInputFieldRenderer(SuggestionHandler suggestionHandler, List<String> suggestionFilteredBy, String suggestionSourceField) {
        this.suggestionHandler = suggestionHandler;
        this.suggestionFilteredBy = suggestionFilteredBy;
        this.suggestionSourceField = suggestionSourceField;
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

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
     */
    @Override
    public boolean isHidden() {
        return false;
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

    public void createSuggestions(DatasetField datasetField, List<DatasetFieldsByType> allBlockFields) {

        Map<String, String> suggestionFilteredFields = datasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with id: " + datasetField.getId() + " didn't have any parent required to generate suggestions"))
                .getDatasetFieldsChildren().stream()
                .filter(dsf -> suggestionFilteredBy.contains(dsf.getDatasetFieldType().getName()))
                .map(dsf -> Tuple.of(dsf.getDatasetFieldType().getName(), dsf.getFieldValue()))
                .collect(Collectors.toMap(Tuple2::_1, tuple -> tuple._2().getOrElse(StringUtils.EMPTY)));
    }


}
