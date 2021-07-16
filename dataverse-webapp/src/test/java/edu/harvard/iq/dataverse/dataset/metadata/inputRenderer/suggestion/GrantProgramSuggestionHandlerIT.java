package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class GrantProgramSuggestionHandlerIT extends WebappArquillianDeployment {

    @Inject
    private Instance<SuggestionHandler> suggestionHandlers;

    private SuggestionHandler grantProgramSuggestionHandler;


    @Before
    public void setUp() {
        suggestionHandlers.forEach(handler -> {
            if (StringUtils.equals(handler.getName(), GrantProgramSuggestionHandler.class.getSimpleName())) {
                grantProgramSuggestionHandler = handler;
            }
        });
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions() {
        //when
        List<Suggestion> grantAgencySuggestions = grantProgramSuggestionHandler
                .generateSuggestions(new HashMap<>(), "200");
        //then
        assertThat(grantAgencySuggestions)
            .extracting(Suggestion::getValue)
            .containsExactlyInAnyOrder("Program No. 2000", "Program No. 2001");
    }

    @Test
    public void createSuggestions_with_agency_filter() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantProgramSuggestionHandler
                .generateSuggestions(ImmutableMap.of(GrantSuggestion.SUGGESTION_NAME_COLUMN, "Some institution"), "200");
        //then
        assertThat(grantAgencyAcronymSuggestions)
            .extracting(Suggestion::getValue)
            .containsExactlyInAnyOrder("Program No. 2000", "Program No. 2001");
    }

    @Test
    public void createSuggestions_with_agency_alternative_name_filter() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantProgramSuggestionHandler
                .generateSuggestions(ImmutableMap.of(GrantSuggestion.SUGGESTION_NAME_COLUMN, "Jakaś instytucja"), "200");
        //then
        assertThat(grantAgencyAcronymSuggestions)
            .extracting(Suggestion::getValue)
            .containsExactlyInAnyOrder("Program No. 2000");
    }

    @Test
    public void createSuggestions_with_agency_non_exact_filter_match() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantProgramSuggestionHandler
                .generateSuggestions(ImmutableMap.of(GrantSuggestion.SUGGESTION_NAME_COLUMN, "Some"), "200");
        //then
        assertThat(grantAgencyAcronymSuggestions).isEmpty();
    }

}
