package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import edu.harvard.iq.dataverse.search.ror.RorSolrDataFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.context.FacesContext;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RorSuggestionHandlerTest {

    @InjectMocks
    private RorSuggestionHandler rorSuggestionHandler;

    @Mock
    private RorSolrDataFinder rorSolrDataFinder;

    // -------------------- TESTS --------------------

    @ParameterizedTest(name = "[{index}] Should generate single suggestion suggestion ({1}, {2})")
    @MethodSource("provideRorArgumentsWithExpectedSuggestion")
    void generateSuggestions(RorDto rorSolr,
            String expectedSuggestionValue, String expectedSuggestionDetails) {
        // given
        when(rorSolrDataFinder.findRorData("query", 5)).thenReturn(Lists.newArrayList(rorSolr));

        // when
        List<Suggestion> suggestions = rorSuggestionHandler.generateSuggestions(new HashMap<>(), "query");

        // then
        assertThat(suggestions)
            .extracting(Suggestion::getValue, Suggestion::getDetails)
            .containsExactlyInAnyOrder(tuple(expectedSuggestionValue, expectedSuggestionDetails));
    }

    @Test
    @DisplayName("Should generate suggestions for all returned solr rors")
    void generateSuggestions_all_returned_solr_rors() {
        // given
        RorDto rorSolr1 = new RorDto()
                .setRorUrl("https://ror.org/013cjyk83")
                .setName("ror1");
        RorDto rorSolr2 = new RorDto()
                .setRorUrl("https://ror.org/013cjyk84")
                .setName("ror2");
        RorDto rorSolr3 = new RorDto()
                .setRorUrl("https://ror.org/013cjyk85")
                .setName("ror3");
        when(rorSolrDataFinder.findRorData("query", 5)).thenReturn(Lists.newArrayList(rorSolr1, rorSolr2, rorSolr3));

        // when
        List<Suggestion> suggestions = rorSuggestionHandler.generateSuggestions(new HashMap<>(), "query");

        // then
        assertThat(suggestions)
            .extracting(Suggestion::getValue)
            .containsExactly("https://ror.org/013cjyk83", "https://ror.org/013cjyk84", "https://ror.org/013cjyk85");
    }

    // -------------------- PRIVATE --------------------

    private static Stream<Arguments> provideRorArgumentsWithExpectedSuggestion() {

        RorDto rorWithIdAndName = new RorDto("rorWithIdAndName", "https://ror.org/rorWithIdAndName",
                "PSL Research University",
                null, null, null, null,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());

        RorDto rorWithIdNameAndCountry = new RorDto("rorWithIdNameAndCountry", "https://ror.org/rorWithIdNameAndCountry",
                "PSL Research University",
                "France", "FR", null, null,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());

        RorDto rorWithAllFields = new RorDto("rorWithAllFields", "https://ror.org/rorWithAllFields",
                "PSL Research University",
                "France", "FR", "Paris", "https://www.psl.eu/en/university",
                Lists.newArrayList("Université PSL", "Alias2"),
                Lists.newArrayList("PSL", "PSLRU"),
                Lists.newArrayList("Université de recherche Paris Sciences et Lettres", "Label2"));
        
        return Stream.of(
          Arguments.of(rorWithIdAndName, "https://ror.org/rorWithIdAndName",
                  "PSL Research University."),
          Arguments.of(rorWithIdNameAndCountry, "https://ror.org/rorWithIdNameAndCountry",
                  "PSL Research University (France)."),
          Arguments.of(rorWithAllFields, "https://ror.org/rorWithAllFields",
                  "PSL Research University (France). Other names: "
                  + "Université PSL; Alias2; PSL; PSLRU; "
                  + "Université de recherche Paris Sciences et Lettres; Label2")
        );
    }
}
