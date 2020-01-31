package edu.harvard.iq.dataverse.search.savedsearch;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.apache.commons.collections4.CollectionUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;

/**
 * Class responsible for saving user typed query for dataverse.
 */
@Stateless
public class SavedSearchService {

    private DataverseSession dataverseSession;
    private DataverseRequestServiceBean dvRequestService;
    private EjbDataverseEngine commandEngine;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SavedSearchService() {
    }

    @Inject
    public SavedSearchService(DataverseSession dataverseSession, DataverseRequestServiceBean dvRequestService, EjbDataverseEngine commandEngine) {
        this.dataverseSession = dataverseSession;
        this.dvRequestService = dvRequestService;
        this.commandEngine = commandEngine;
    }
// -------------------- LOGIC --------------------

    public SavedSearch saveSavedDataverseSearch(String query, Collection<String> facetQueries, Dataverse dataverseToBeLinked) {

        SavedSearch savedSearch = new SavedSearch(query, dataverseToBeLinked, (AuthenticatedUser) dataverseSession.getUser());

        facetQueries.stream()
                .filter(facetQuery -> CollectionUtils.isNotEmpty(facetQueries))
                .forEach(facetQuery -> savedSearch.getSavedSearchFilterQueries().add(new SavedSearchFilterQuery(facetQuery, savedSearch)));

        return commandEngine.submit(new CreateSavedSearchCommand(dvRequestService.getDataverseRequest(), dataverseToBeLinked, savedSearch));
    }
}
