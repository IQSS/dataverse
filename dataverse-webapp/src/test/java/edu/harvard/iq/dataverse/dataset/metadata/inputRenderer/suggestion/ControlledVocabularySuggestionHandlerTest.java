package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Collections.singletonList;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControlledVocabularySuggestionHandlerTest {

    @InjectMocks
    private ControlledVocabularySuggestionHandler controlledVocabularySuggestionHandler;

    @Mock
    private ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    // -------------------- TESTS --------------------

    @Test
    void generateSuggestions() {
        // given
        ControlledVocabularyValue vocabularyValue = new ControlledVocabularyValue();
        vocabularyValue.setStrValue("suggestion1");
        vocabularyValue.setIdentifier("123");

        when(controlledVocabularyValueServiceBean.findByDatasetFieldTypeNameAndValueLike(
                anyString(), anyString(), eq(10))
        )
                .thenReturn(singletonList(vocabularyValue));

        // when
        List<Suggestion> suggestions = controlledVocabularySuggestionHandler
                .generateSuggestions(
                ImmutableMap.of(ControlledVocabularySuggestionHandler.CONTROLLED_VOCABULARY_NAME_COLUMN,
                        "testVocabulary"), "query");

        // then
        assertThat(suggestions)
                .extracting(Suggestion::getValue)
                .containsExactly("suggestion1");

        assertThat(suggestions)
                .extracting(Suggestion::getDetails)
                .containsExactly("123");

        verify(controlledVocabularyValueServiceBean).findByDatasetFieldTypeNameAndValueLike(
                "testVocabulary", "query", 10);
    }
}
