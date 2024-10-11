package edu.harvard.iq.dataverse.search.savedsearch;


import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@Transactional(TransactionMode.ROLLBACK)
public class SavedSearchServiceIT extends WebappArquillianDeployment {

    @Inject
    private SavedSearchService savedSearchService;
    
    @Inject
    private DataverseDao dataverseDao;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DataverseSession dataverseSession;

    @BeforeEach
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void saveSavedDataverseSearch() {
        //given
        Dataverse dataverseToBeLinked = dataverseDao.find(19L);
        String searchQuery = "testQuery";
        String facetQuery = "facetQuery";

        //when
        SavedSearch savedSearch = savedSearchService.saveSavedDataverseSearch(searchQuery, Lists.newArrayList(facetQuery), dataverseToBeLinked);

        //then
        Assertions.assertEquals(savedSearch.getQuery(), searchQuery);
        Assertions.assertEquals(1, savedSearch.getFilterQueriesAsStrings().size());
        Assertions.assertEquals(facetQuery, savedSearch.getFilterQueriesAsStrings().get(0));
        Assertions.assertEquals(savedSearch.getDefinitionPoint(), dataverseToBeLinked);


    }
}
