package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import javax.ejb.Stateless;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

@Stateless
public class VocabSelectInputFieldRendererFactory implements InputFieldRendererFactory<VocabSelectInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public VocabSelectInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        VocabularyInputRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, VocabularyInputRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));
        
        return new VocabSelectInputFieldRenderer(rendererOptions.isSortByLocalisedStringsOrder());
    }

    // -------------------- INNER CLASSES --------------------
    
    /**
     * Class representing allowed options for {@link VocabSelectInputFieldRenderer}
     */
    public static class VocabularyInputRendererOptions {
        private boolean sortByLocalisedStringsOrder = false;
        
        // -------------------- GETTERS --------------------

        public boolean isSortByLocalisedStringsOrder() {
			return sortByLocalisedStringsOrder;
		}
        
        
        // -------------------- SETTERS --------------------
        
		public void setBySortLocalisedStringsOrder(boolean sortByLocalisedStringsOrder) {
			this.sortByLocalisedStringsOrder = sortByLocalisedStringsOrder;
		}
    }
}
