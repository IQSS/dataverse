package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.GrantSuggestionConnector;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class SuggestionInputFieldRenderer implements InputFieldRenderer {

    private SuggestionHandler suggestionHandler;
    private List<String> suggestionFilteredBy;
    private String suggestionSourceField;
    private final GrantSuggestionConnector grantSuggestionConnector = new GrantSuggestionConnector();

    // -------------------- CONSTRUCTORS --------------------

    public SuggestionInputFieldRenderer() {
    }

    /**
     * Constructs renderer with support for suggestions.
     */
    public SuggestionInputFieldRenderer(SuggestionHandler suggestionHandler, List<String> suggestionFilteredBy, String suggestionSourceField) {
        this.suggestionHandler = suggestionHandler;
        this.suggestionFilteredBy = suggestionFilteredBy;
        this.suggestionSourceField = suggestionSourceField;
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

    // -------------------- LOGIC --------------------

    /**
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<String> processSuggestionQuery(DatasetField datasetField, String autoCompleteId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, String> parameterMap = ctx.getExternalContext().getRequestParameterMap();

        Optional<String> query = parameterMap.keySet().stream()
                .filter(key -> key.endsWith(autoCompleteId + "_query"))
                .map(parameterMap::get)
                .findFirst();

        return createSuggestions(datasetField, query.orElseThrow(() -> new IllegalStateException("Autocomplete query was not found.")));
    }

    /**
     * Creates suggestions, if suggestionFilteredBy is not empty it searches through datasetField group to find the inputed values.
     */
    public List<String> createSuggestions(DatasetField datasetField, String query) {

        if (!hasSuggestionsAvailable()){
            return new ArrayList<>();
        }

        Map<String, String> suggestionFilteredFields = new HashMap<>();

        if (!suggestionFilteredBy.isEmpty()) {

            suggestionFilteredFields = findFilterValues(datasetField, suggestionFilteredBy);

            if (suggestionFilteredFields.isEmpty()){
                return new ArrayList<>();
            }
        }

        return suggestionHandler.generateSuggestions(suggestionFilteredFields,
                                                     grantSuggestionConnector.dsftToGrantEntityName(suggestionSourceField),
                                                     query);
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> findFilterValues(DatasetField datasetField, List<String> filterFields) {
        Map<String, String> filteredValues = datasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + datasetField.getDatasetFieldType().getName() + " didn't have any parent required to generate suggestions"))
                .getDatasetFieldsChildren().stream()
                .filter(dsf -> filterFields.contains(dsf.getDatasetFieldType().getName()))
                .map(dsf -> Tuple.of(grantSuggestionConnector.dsftToGrantEntityName(dsf.getDatasetFieldType().getName()),
                                     dsf.getFieldValue().getOrElse(StringUtils.EMPTY)))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        return filteredValues.containsValue(StringUtils.EMPTY) ? new HashMap<>() : filteredValues;
    }

    private boolean hasSuggestionsAvailable() {
        return suggestionHandler != null;
    }
}
