package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(TransactionMode.ROLLBACK)
public class GrantAgencyAcronymSuggestionHandlerIT extends WebappArquillianDeployment {

    @Inject
    private Instance<SuggestionHandler> suggestionHandlers;

    private SuggestionHandler grantAgencyAcronymSuggestionHandler;


    @BeforeEach
    public void setUp() {
        suggestionHandlers.forEach(handler -> {
            if (StringUtils.equals(handler.getName(), GrantAgencyAcronymSuggestionHandler.class.getSimpleName())) {
                grantAgencyAcronymSuggestionHandler = handler;
            }
        });
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantAgencyAcronymSuggestionHandler
                .generateSuggestions(new HashMap<>(), "tur");
        //then
        assertThat(grantAgencyAcronymSuggestions)
            .extracting(Suggestion::getValue)
            .containsExactlyInAnyOrder("TUR", "TURA");
    }

    @Test
    public void createSuggestions_with_agency_filter() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantAgencyAcronymSuggestionHandler
                .generateSuggestions(ImmutableMap.of(GrantSuggestion.SUGGESTION_NAME_COLUMN, "Turkish Agency"), "tur");
        //then
        assertThat(grantAgencyAcronymSuggestions)
            .extracting(Suggestion::getValue)
            .containsExactlyInAnyOrder("TUR");
    }

    @Test
    public void createSuggestions_with_non_exact_agency_filter_match() {
        //when
        List<Suggestion> grantAgencyAcronymSuggestions = grantAgencyAcronymSuggestionHandler
                .generateSuggestions(ImmutableMap.of(GrantSuggestion.SUGGESTION_NAME_COLUMN, "Turkish"), "tur");
        //then
        assertThat(grantAgencyAcronymSuggestions).isEmpty();
    }

}
