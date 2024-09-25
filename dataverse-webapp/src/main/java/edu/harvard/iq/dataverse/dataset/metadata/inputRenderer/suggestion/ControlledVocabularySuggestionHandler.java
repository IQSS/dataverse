package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * This suggestion handler provides suggestions based on controlled vocabulary values.
 */
@Stateless
public class ControlledVocabularySuggestionHandler implements SuggestionHandler {

    public static final String CONTROLLED_VOCABULARY_NAME_COLUMN = "controlledVocabularyName";
    private ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ControlledVocabularySuggestionHandler() {
    }

    @Inject
    public ControlledVocabularySuggestionHandler(ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean) {
        this.controlledVocabularyValueServiceBean = controlledVocabularyValueServiceBean;
    }

    // -------------------- LOGIC --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns class name.
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }


    /**
     * This is not dependent on siblings input values. All values will be taken
     * to build matching suggestions list.
     */
    @Override
    public boolean isDependentOnSiblings() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation filers out base on controlled vocabulary (input) name,
     * a.k.a. dictionary name
     */
    @Override
    public List<String> getAllowedFilters() {
        return Lists.newArrayList(CONTROLLED_VOCABULARY_NAME_COLUMN);
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldValue) {
        return controlledVocabularyValueServiceBean
                .findByDatasetFieldTypeNameAndValueLike(filters.get(CONTROLLED_VOCABULARY_NAME_COLUMN), suggestionSourceFieldValue, 10)
                .stream().map(vocabulary -> new Suggestion(vocabulary.getStrValue(), vocabulary.getIdentifier()))
                .collect(toList());
    }
}
