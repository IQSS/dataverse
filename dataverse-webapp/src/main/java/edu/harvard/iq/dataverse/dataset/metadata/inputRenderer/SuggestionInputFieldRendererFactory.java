package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.api.client.util.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.collections4.IteratorUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class SuggestionInputFieldRendererFactory implements InputFieldRendererFactory<SuggestionInputFieldRenderer>{

    private final Map<String, SuggestionHandler> suggestionHandlers = new HashMap<>();
    private final Instance<SuggestionHandler> suggestionHandlerInstance;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public SuggestionInputFieldRendererFactory(Instance<SuggestionHandler> suggestionHandlerInstance) {
        this.suggestionHandlerInstance = suggestionHandlerInstance;
    }

    @PostConstruct
    public void postConstruct() {

        IteratorUtils.toList(suggestionHandlerInstance.iterator())
                .forEach(factory -> suggestionHandlers.put(factory.getName(), factory));
    }

    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.SUGGESTION_TEXT;
    }

    @Override
    public SuggestionInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {

        SuggestionInputRendererOptions rendererOptions = parseRendererOptions(jsonOptions);

        SuggestionHandler suggestionHandler = obtainSuggestionHandlerByName(rendererOptions.getSuggestionSourceClass());

        Map<String, String> datasetFieldTypeToSuggestionFilterMapping = parseFilteredBy(
                rendererOptions.getSuggestionFilteredBy(), suggestionHandler);

        return new SuggestionInputFieldRenderer(
                suggestionHandler,
                datasetFieldTypeToSuggestionFilterMapping,
                rendererOptions.getSuggestionDisplayType() == null ? SuggestionDisplayType.SIMPLE : rendererOptions.getSuggestionDisplayType(),
                fieldType.getName(),
                fieldType.getMetadataBlock().getName());
    }

    // -------------------- PRIVATE --------------------

    private SuggestionInputRendererOptions parseRendererOptions(JsonObject jsonOptions) {
        return Try.of(() -> new Gson().fromJson(jsonOptions, SuggestionInputRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));
    }

    private SuggestionHandler obtainSuggestionHandlerByName(String suggestionSourceClass) {
        return Option.of(this.suggestionHandlers.get(suggestionSourceClass))
                .getOrElseThrow(() -> new InputRendererInvalidConfigException(
                        "Suggestion handler with name: " + suggestionSourceClass + " doesn't exist."));
    }

    private Map<String, String> parseFilteredBy(List<String> suggestionFilteredBy, SuggestionHandler suggestionHandler) {
        Map<String, String> datasetFieldTypeToSuggestionFilterMapping = Maps.newHashMap();
        if (suggestionFilteredBy == null) {
            return datasetFieldTypeToSuggestionFilterMapping;
        }

        for (String filter: suggestionFilteredBy) {
            String[] dftToSuggestionFilter = filter.split(":");
            if (dftToSuggestionFilter.length != 2) {
                throw new InputRendererInvalidConfigException("Invalid value for suggestionFilteredBy: " + filter);
            }
            if (!suggestionHandler.getAllowedFilters().contains(dftToSuggestionFilter[1])) {
                throw new InputRendererInvalidConfigException("Suggestion handler: " + suggestionHandler.getName() + " does not support filtering by: : " + dftToSuggestionFilter[1]);
            }

            datasetFieldTypeToSuggestionFilterMapping.put(
                    dftToSuggestionFilter[0], dftToSuggestionFilter[1]);
        }
        return datasetFieldTypeToSuggestionFilterMapping;
    }

    // -------------------- INNER CLASSES --------------------
    public static class SuggestionInputRendererOptions {

        private String suggestionSourceClass;
        private List<String> suggestionFilteredBy;
        private SuggestionDisplayType suggestionDisplayType;

        // -------------------- GETTERS --------------------

        /**
         * Defines which class will be used for retrieving suggestions.
         * Value must match {@link SuggestionHandler#getName()}.
         */
        public String getSuggestionSourceClass() {
            return suggestionSourceClass;
        }

        /**
         * Defines additional filters for retrieved suggestions.
         * Each value in list must be in form:
         * <code>datasetFieldTypeName:suggestionFilterName</code>.
         * <p>
         * Allowed values for <code>suggestionFilterName</code> are
         * specific to picked {@link SuggestionHandler} and they
         * are defined in {@link SuggestionHandler#getAllowedFilters()}
         * <p>
         * Suggestions mechanism will take value that is currently
         * entered in field <code>datasetFieldTypeName</code>
         * and pass it to {@link SuggestionHandler#generateSuggestions(Map, String)}
         * filters map in key: <code>suggestionFilterName</code>
         */
        public List<String> getSuggestionFilteredBy() {
            return suggestionFilteredBy;
        }

        /**
         * Defines style in which suggestions will be displayed for user.
         */
        public SuggestionDisplayType getSuggestionDisplayType() {
            return suggestionDisplayType;
        }
    }

    public enum SuggestionDisplayType {
        SIMPLE,
        TWO_COLUMNS
    }
}
