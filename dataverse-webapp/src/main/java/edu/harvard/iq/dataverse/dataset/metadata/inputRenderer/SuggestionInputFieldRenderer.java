package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.SuggestionInputFieldRendererFactory.SuggestionDisplayType;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuggestionInputFieldRenderer implements InputFieldRenderer {
    private static final String VALUE_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.valueHeader";
    private static final String DETAILS_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.detailsHeader";

    private SuggestionHandler suggestionHandler;
    private Map<String, String> datasetFieldTypeToSuggestionFilterMapping;
    private SuggestionDisplayType suggestionDisplayType;
    private String datasetFieldTypeName;
    private String metadataBlockName;

    // -------------------- CONSTRUCTORS --------------------

    public SuggestionInputFieldRenderer() {
    }

    /**
     * Constructs renderer with support for suggestions.
     */
    public SuggestionInputFieldRenderer(
            SuggestionHandler suggestionHandler,
            Map<String, String> datasetFieldTypeToSuggestionFilterMapping,
            SuggestionDisplayType suggestionDisplayType,
            String datasetFieldTypeName, String metadataBlockName) {

        this.suggestionHandler = suggestionHandler;
        this.datasetFieldTypeToSuggestionFilterMapping = datasetFieldTypeToSuggestionFilterMapping;
        this.suggestionDisplayType = suggestionDisplayType;
        this.datasetFieldTypeName = datasetFieldTypeName;
        this.metadataBlockName = metadataBlockName;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#SUGGESTION_TEXT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.SUGGESTION_TEXT;
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

    public SuggestionDisplayType getSuggestionDisplayType() {
        return suggestionDisplayType;
    }

    public String getValueHeaderText() {
        return BundleUtil.getStringFromNonDefaultBundle(
                String.format(VALUE_HEADER_KEY_FORMAT, datasetFieldTypeName), metadataBlockName);
    }
    public String getDetailsHeaderText() {
        return BundleUtil.getStringFromNonDefaultBundle(
                String.format(DETAILS_HEADER_KEY_FORMAT, datasetFieldTypeName), metadataBlockName);
    }

    // -------------------- LOGIC --------------------

    /**
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<Suggestion> processSuggestionQuery(DatasetField datasetField, String autoCompleteId) {
        return createSuggestions(
                datasetField,
                SuggestionAutocompleteHelper.processSuggestionQuery(autoCompleteId)
                        .orElseThrow(() -> new IllegalStateException("Autocomplete query was not found.")));
    }

    /**
     * Creates suggestions, if suggestionFilteredBy is not empty it searches through datasetField group to find the inputed values.
     */
    public List<Suggestion> createSuggestions(DatasetField datasetField, String query) {

        Map<String, String> suggestionFilteredFields = new HashMap<>();

        if (!datasetFieldTypeToSuggestionFilterMapping.isEmpty()) {
            if(suggestionHandler.isDependentOnSiblings()) {
                suggestionFilteredFields = findFilterValuesInSiblings(datasetField, datasetFieldTypeToSuggestionFilterMapping);
            } else {
                suggestionFilteredFields = getFilterValue(datasetField, datasetFieldTypeToSuggestionFilterMapping);
            }

            if (suggestionFilteredFields.isEmpty()){
                return new ArrayList<>();
            }
        }

        return suggestionHandler.generateSuggestions(suggestionFilteredFields, query);
    }

    // -------------------- PRIVATE --------------------

    /**
     * In this scenario we are using indirectly value from datasetFieldTypeToSuggestionFilterMapping
     * which comes from datasetField configuration.
     * This value is used indirectly to filter out suggestions.
     * This value is the name of other siblings datasetField in datasetField group,
     * that shares common parent. Method loops over all siblings to find out
     * the one that have corresponding name, then its value is used to filter out suggestions.
     */
    private Map<String, String> findFilterValuesInSiblings(DatasetField datasetField, Map<String, String> datasetFieldTypeToSuggestionFilterMapping) {
        Map<String, String> filteredValues = datasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + datasetField.getTypeName() + " didn't have any parent required to generate suggestions"))
                .getDatasetFieldsChildren().stream()
                .filter(dsf -> datasetFieldTypeToSuggestionFilterMapping.containsKey(dsf.getTypeName()))
                .map(dsf -> Tuple.of(
                        datasetFieldTypeToSuggestionFilterMapping.get(dsf.getTypeName()),
                        dsf.getFieldValue().getOrElse(StringUtils.EMPTY)))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        return filteredValues.containsValue(StringUtils.EMPTY) ? new HashMap<>() : filteredValues;
    }

    /**
     * In this scenario we are using directly value from datasetFieldTypeToSuggestionFilterMapping
     * which comes from datasetField configuration.
     * This value is used directly to filter out suggestions.
     */
    private Map<String, String> getFilterValue(DatasetField datasetField, Map<String, String> datasetFieldTypeToSuggestionFilterMapping) {
        return datasetFieldTypeToSuggestionFilterMapping
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(datasetField.getTypeName()))
                .findFirst()
                .map(entry -> {
                    Map<String, String> filterValues = new HashMap<>();
                    filterValues.put(entry.getValue(), entry.getKey());
                    return filterValues;
                })
                .orElseGet(HashMap::new);
    }
}
