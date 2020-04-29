package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;

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

        SuggestionInputRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, SuggestionInputRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));


        if (StringUtils.isNotBlank(rendererOptions.getSuggestionSourceClass())) {

            return createRendererWithSuggestionHandler(rendererOptions);
        }

        return new SuggestionInputFieldRenderer();
    }

    // -------------------- PRIVATE --------------------

    private SuggestionInputFieldRenderer createRendererWithSuggestionHandler(SuggestionInputRendererOptions options) {
        SuggestionHandler suggestionHandler = Option.of(this.suggestionHandlers.get(options.getSuggestionSourceClass()))
                .getOrElseThrow(() -> new InputRendererInvalidConfigException("Suggestion handler with name: " +
                                                                                      options.getSuggestionSourceClass() + " doesn't exist."));


        return new SuggestionInputFieldRenderer(suggestionHandler, options.getSuggestionFilteredBy(), options.getSuggestionSourceField());
    }

    // -------------------- INNER CLASSES --------------------
    public static class SuggestionInputRendererOptions {

        private List<String> suggestionFilteredBy;
        private String suggestionSourceClass;
        private String suggestionSourceField;

        // -------------------- GETTERS --------------------

        public List<String> getSuggestionFilteredBy() {
            return suggestionFilteredBy;
        }

        public String getSuggestionSourceClass() {
            return suggestionSourceClass;
        }

        public String getSuggestionSourceField() {
            return suggestionSourceField;
        }
    }
}
