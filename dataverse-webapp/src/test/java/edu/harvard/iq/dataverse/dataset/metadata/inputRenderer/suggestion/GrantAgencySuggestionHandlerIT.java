package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
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
public class GrantAgencySuggestionHandlerIT extends WebappArquillianDeployment {

    @Inject
    private Instance<SuggestionHandler> suggestionHandlers;

    private SuggestionHandler grantAgencySuggestionHandler;


    @Before
    public void setUp() {
        suggestionHandlers.forEach(handler -> {
            if (StringUtils.equals(handler.getName(), GrantAgencySuggestionHandler.class.getSimpleName())) {
                grantAgencySuggestionHandler = handler;
            }
        });
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions() {
        //when
        List<Suggestion> grantAgencySuggestions = grantAgencySuggestionHandler.generateSuggestions(new HashMap<>(), "Turk");
        //then
        assertThat(grantAgencySuggestions)
            .extracting(Suggestion::getValue)
            .containsExactly("Turkish Agency");
    }

    @Test
    public void createSuggestions_match_by_middle_of_string() {
        // when
        List<Suggestion> grantAgencySuggestions = grantAgencySuggestionHandler.generateSuggestions(new HashMap<>(), "urk");
        //then
        assertThat(grantAgencySuggestions)
            .extracting(Suggestion::getValue)
            .containsExactly("Turkish Agency");
    }

}