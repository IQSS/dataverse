package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceTest {

    @InjectMocks
    private SavedSearchService savedSearchService;

    @Mock
    private DataverseSession dataverseSession;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Test
    public void saveSavedDataverseSearch() {
        //given
        Dataverse dataverse = new Dataverse();
        dataverse.setId(11L);

        String query = "testQuery";
        String filteredQuery = "fq:testQuery";

        ArgumentCaptor<CreateSavedSearchCommand> commandArgumentCaptor = ArgumentCaptor.forClass(CreateSavedSearchCommand.class);

        //when
        savedSearchService.saveSavedDataverseSearch(query, Collections.singletonList(filteredQuery), dataverse);

        //then
        Mockito.verify(commandEngine, Mockito.times(1)).submit(commandArgumentCaptor.capture());

        SavedSearch savedSearch = commandArgumentCaptor.getValue().getSavedSearchToCreate();
        Assert.assertEquals(savedSearch.getQuery(), query);
        Assert.assertTrue(savedSearch.getFilterQueriesAsStrings().contains(filteredQuery));
        Assert.assertEquals(savedSearch.getDefinitionPoint(), dataverse);

    }
}