package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

import javax.ejb.Stateless;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Stateless
public class HiddenVocabInputFieldRendererFactory implements InputFieldRendererFactory<HiddenVocabInputFieldRenderer> {
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HIDDEN_VOCABULARY}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.HIDDEN_VOCABULARY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allowed options for renderer should match {@link HiddenVocabInputFieldRendererOptions}
     */
    @Override
    public HiddenVocabInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        
        HiddenVocabInputFieldRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, HiddenVocabInputFieldRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));
        
        List<ControlledVocabularyValue> defaultValues = fieldType.getControlledVocabularyValues().stream()
            .filter(vocabValue -> rendererOptions.getDefaultValues().contains(vocabValue.getStrValue()))
            .collect(toList());
        
        return new HiddenVocabInputFieldRenderer(defaultValues);
    }
    
    // -------------------- INNER CLASSES --------------------

    /**
     * Class representing allowed options for {@link HiddenVocabInputFieldRenderer}
     */
    public static class HiddenVocabInputFieldRendererOptions {
        private Set<String> defaultValues = new HashSet<>();

        // -------------------- GETTERS --------------------
        
        public Set<String> getDefaultValues() {
            return defaultValues;
        }

        // -------------------- SETTERS --------------------
        
        public void setDefaultValues(Set<String> defaultValues) {
            this.defaultValues = defaultValues;
        }
    }
}
