package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.*;
import java.util.stream.Collectors;


public class VocabSelectEnhancedInputFieldRenderer implements InputFieldRenderer {

    private Collection<ControlledVocabularyValue> all = new ArrayList<>();
    private int numberOfResults = 100;
    private static final int RESULTS_INCREMENT_STEP = 100;

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_ENHANCED_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_ENHANCED_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns value provided in constructor
     */
    @Override
    public boolean renderInTwoColumns() {
        return false;
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

    public Integer getNumberOfResults() {
        return numberOfResults;
    }

    public void onSelection(DatasetField datasetField, String autoCompleteId) {
        queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public void onDeselection(DatasetField datasetField, String autoCompleteId) {
        queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public List<ControlledVocabularyValue> complete(DatasetField datasetField, String autoCompleteId) {
        return queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public List<ControlledVocabularyValue> loadMore(DatasetField datasetField, String autoCompleteId) {
        numberOfResults += RESULTS_INCREMENT_STEP;
        return queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    private List<ControlledVocabularyValue> queryControlledVocabularyValues(DatasetField datasetField, String autoCompleteId) {
        if (all == null || all.isEmpty()) {
            all = datasetField.getDatasetFieldType().getControlledVocabularyValues();
        }
        String query = SuggestionAutocompleteHelper.processSuggestionQuery(autoCompleteId).orElse("");
        return all.stream()
                .filter(item -> !datasetField.getControlledVocabularyValues().contains(item))
                .filter(item -> item.getStrValue().toLowerCase().contains(query.toLowerCase()))
                .limit(numberOfResults)
                .collect(Collectors.toList());
    }
}
