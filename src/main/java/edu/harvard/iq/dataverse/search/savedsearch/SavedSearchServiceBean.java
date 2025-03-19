package edu.harvard.iq.dataverse.search.savedsearch;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetLinkingDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseLinkingDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Named
public class SavedSearchServiceBean {

    private static final Logger logger = Logger.getLogger(SavedSearchServiceBean.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    protected DatasetLinkingServiceBean dsLinkingService;
    @EJB
    protected DataverseLinkingServiceBean dvLinkingService;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    SystemConfig systemConfig;

    private final String resultString = "result";

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public SavedSearch find(long id) {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o WHERE o.id = :id", SavedSearch.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
    public  List<SavedSearch> findByAuthenticatedUser(AuthenticatedUser user) {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o WHERE o.creator.id = :id", SavedSearch.class);
        typedQuery.setParameter("id", user.getId());
        try {
            return typedQuery.getResultList();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<SavedSearch> findAll() {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o ORDER BY o.id", SavedSearch.class);
        return typedQuery.getResultList();
    }

    public SavedSearch add(SavedSearch toPersist) {
        /**
         * @todo Don't let anyone persist the same saved search twice. What does
         * "same" mean. For the first cut we'll check for a String match of both
         * query and filterQueries.
         *
         * @todo Don't allow wildcard queries.
         */
        SavedSearch persisted = null;
        try {
            persisted = em.merge(toPersist);
        } catch (Exception ex) {
            logger.fine("Failed to add SavedSearch" + ex);
        }
        return persisted;
    }

    public boolean delete(long id, boolean unlink) throws SearchException, CommandException {
        SavedSearch doomed = find(id);
        boolean wasDeleted = false;
        if (doomed != null) {
            logger.info("Deleting saved search id " + doomed.getId());
            if(unlink) {
                DataverseRequest dataverseRequest = new DataverseRequest(doomed.getCreator(), getHttpServletRequest());
                removeLinks(dataverseRequest, doomed);
            }
            em.remove(doomed);
            em.flush();
            wasDeleted = true;
        } else {
            logger.info("Problem deleting saved search id " + id);
        }
        return wasDeleted;
    }

    public SavedSearch save(SavedSearch savedSearch) {
        if (savedSearch.getId() == null) {
            em.persist(savedSearch);
            return savedSearch;
        } else {
            return em.merge(savedSearch);
        }
    }
    
    
    @Schedule(dayOfWeek="0", hour="0", minute="30", persistent = false)
    public void makeLinksForAllSavedSearchesTimer() {
        if (systemConfig.isTimerServer()) {
            logger.info("Linking saved searches");
            try {
                JsonObjectBuilder makeLinksForAllSavedSearches = makeLinksForAllSavedSearches(false);
            } catch (SearchException | CommandException ex) {
                Logger.getLogger(SavedSearchServiceBean.class.getName()).log(Level.SEVERE, null, ex);
            }       
        }
    }

    public JsonObjectBuilder makeLinksForAllSavedSearches(boolean debugFlag) throws SearchException, CommandException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        List<SavedSearch> allSavedSearches = findAll();
        JsonArrayBuilder savedSearchArrayBuilder = Json.createArrayBuilder();
        for (SavedSearch savedSearch : allSavedSearches) {
            DataverseRequest dataverseRequest = new DataverseRequest(savedSearch.getCreator(), getHttpServletRequest());
            JsonObjectBuilder perSavedSearchResponse = makeLinksForSingleSavedSearch(dataverseRequest, savedSearch, debugFlag);
            savedSearchArrayBuilder.add(perSavedSearchResponse);
        }
        response.add("hits by saved search", savedSearchArrayBuilder);
        return response;
    }

    /**
     * The "Saved Search" and highly related "Linked Dataverses and Linked
     * Datasets" features can be thought of as periodic execution of the
     * LinkDataverseCommand and LinkDatasetCommand. As of this writing that
     * periodic execution can be triggered via a cron job but we'd like to put
     * it on an EJB timer as part of
     * https://github.com/IQSS/dataverse/issues/2543 .
     *
     * The commands are executed by the creator of the SavedSearch. What happens
     * if the users loses the permission that the command requires? Should the
     * commands continue to be executed periodically as some "system" user?
     *
     * @return Debug information in the form of a JSON object, which is much
     * more structured that a simple String.
     */
    public JsonObjectBuilder makeLinksForSingleSavedSearch(DataverseRequest dvReq, SavedSearch savedSearch, boolean debugFlag) throws SearchException, CommandException {
        logger.info("SAVED SEARCH (" + savedSearch.getId() + ") START search and link process");
        Date start = new Date();
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder savedSearchArrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder infoPerHit = Json.createArrayBuilder();
        SolrQueryResponse queryResponse = findHits(savedSearch);

        List skipList = new ArrayList(); // a list for the definition point itself and already linked objects
        skipList.add(savedSearch.getDefinitionPoint().getId());
        
        TypedQuery<Long> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findIdsByLinkingDataverseId", Long.class)
            .setParameter("linkingDataverseId", savedSearch.getDefinitionPoint().getId());
        skipList.addAll(typedQuery.getResultList());
        
        typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findIdsByLinkingDataverseId", Long.class)
            .setParameter("linkingDataverseId", savedSearch.getDefinitionPoint().getId());
        skipList.addAll(typedQuery.getResultList());
           
        for (SolrSearchResult solrSearchResult : queryResponse.getSolrSearchResults()) {

            JsonObjectBuilder hitInfo = Json.createObjectBuilder();
            hitInfo.add("name", solrSearchResult.getNameSort());
            hitInfo.add("dvObjectId", solrSearchResult.getEntityId());
            
            if (skipList.contains(solrSearchResult.getEntityId())) {
                hitInfo.add(resultString, "Skipping because would link to itself or an already linked entity.");
                infoPerHit.add(hitInfo);
                continue;
            }

            DvObject dvObjectThatDefinitionPointWillLinkTo = dvObjectService.findDvObject(solrSearchResult.getEntityId());
            if (dvObjectThatDefinitionPointWillLinkTo == null) {
                hitInfo.add(resultString, "Could not find DvObject with id " + solrSearchResult.getEntityId());
                infoPerHit.add(hitInfo);
                continue;
            }   
                    
            if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataverse()) {
                Dataverse dataverseToLinkTo = (Dataverse) dvObjectThatDefinitionPointWillLinkTo;
                if (dataverseToLinkToIsAlreadyPartOfTheSubtree(savedSearch.getDefinitionPoint(), dataverseToLinkTo)) {
                    hitInfo.add(resultString, "Skipping because " + dataverseToLinkTo + " is already part of the subtree for " + savedSearch.getDefinitionPoint());
                } else {
                    DataverseLinkingDataverse link = commandEngine.submitInNewTransaction(new LinkDataverseCommand(dvReq, savedSearch.getDefinitionPoint(), dataverseToLinkTo));
                    hitInfo.add(resultString, "Persisted DataverseLinkingDataverse id " + link.getId() + " link of " + dataverseToLinkTo + " to " + savedSearch.getDefinitionPoint());
                }
            } else if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataset()) {
                Dataset datasetToLinkTo = (Dataset) dvObjectThatDefinitionPointWillLinkTo;
                if (datasetToLinkToIsAlreadyPartOfTheSubtree(savedSearch.getDefinitionPoint(), datasetToLinkTo)) {
                    // already there from normal search/browse
                    hitInfo.add(resultString, "Skipping because dataset " + datasetToLinkTo.getId() + " is already part of the subtree for " + savedSearch.getDefinitionPoint().getAlias());
                } else if (datasetAncestorAlreadyLinked(savedSearch.getDefinitionPoint(), datasetToLinkTo)) {
                    hitInfo.add(resultString, "FIXME: implement this?");
                }
                else {
                    DatasetLinkingDataverse link = commandEngine.submitInNewTransaction(new LinkDatasetCommand(dvReq, savedSearch.getDefinitionPoint(), datasetToLinkTo));
                    hitInfo.add(resultString, "Persisted DatasetLinkingDataverse id " + link.getId() + " link of " + link.getDataset() + " to " + link.getLinkingDataverse());
                }
            } else {
                hitInfo.add(resultString, "Unexpected DvObject type.");
            }
            infoPerHit.add(hitInfo);
        }
        
        JsonObjectBuilder info = getInfo(savedSearch, infoPerHit);
        if (debugFlag) {
            info.add("debug", getDebugInfo(savedSearch));
        }
        savedSearchArrayBuilder.add(info);
        response.add("hits for saved search id " + savedSearch.getId(), savedSearchArrayBuilder);
        
        logger.info("SAVED SEARCH (" + savedSearch.getId() + ") total time in ms: " + (new Date().getTime() - start.getTime()));
        return response;
    }

    /**
     * This method to the reverse of a makeLinksForSingleSavedSearch method.
     * It removes all Dataset and Dataverse links that match savedSearch's query.
     * @param dvReq
     * @param savedSearch
     * @throws SearchException
     * @throws CommandException
     */
    public void removeLinks(DataverseRequest dvReq, SavedSearch savedSearch) throws SearchException, CommandException {
        logger.fine("UNLINK SAVED SEARCH (" + savedSearch.getId() + ") START search and unlink process");
        Date start = new Date();
        Dataverse linkingDataverse = savedSearch.getDefinitionPoint();

        SolrQueryResponse queryResponse = findHits(savedSearch);
        for (SolrSearchResult solrSearchResult : queryResponse.getSolrSearchResults()) {

            DvObject dvObjectThatDefinitionPointWillLinkTo = dvObjectService.findDvObject(solrSearchResult.getEntityId());
            if (dvObjectThatDefinitionPointWillLinkTo == null) {
                continue;
            }

            if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataverse()) {
                Dataverse linkedDataverse = (Dataverse) dvObjectThatDefinitionPointWillLinkTo;
                DataverseLinkingDataverse dvld = dvLinkingService.findDataverseLinkingDataverse(linkedDataverse.getId(), linkingDataverse.getId());
                if(dvld != null) {
                    Dataverse dv = commandEngine.submitInNewTransaction(new DeleteDataverseLinkingDataverseCommand(dvReq, linkingDataverse, dvld, true));
                }
            } else if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataset()) {
                Dataset linkedDataset = (Dataset) dvObjectThatDefinitionPointWillLinkTo;
                DatasetLinkingDataverse dsld = dsLinkingService.findDatasetLinkingDataverse(linkedDataset.getId(), linkingDataverse.getId());
                if(dsld != null) {
                    Dataset ds = commandEngine.submitInNewTransaction(new DeleteDatasetLinkingDataverseCommand(dvReq, linkedDataset, dsld, true));
                }
            }
        }

        logger.fine("UNLINK SAVED SEARCH (" + savedSearch.getId() + ") total time in ms: " + (new Date().getTime() - start.getTime()));
    }

    private SolrQueryResponse findHits(SavedSearch savedSearch) throws SearchException {
        String sortField = SearchFields.TYPE; // first return dataverses, then datasets
        String sortOrder = SortBy.DESCENDING;
        SortBy sortBy = new SortBy(sortField, sortOrder);
        int paginationStart = 0;
        boolean dataRelatedToMe = false;
        int numResultsPerPage = Integer.MAX_VALUE;
        List<Dataverse> dataverses = new ArrayList<>();
        dataverses.add(savedSearch.getDefinitionPoint());
        
        // since saved search can only link Dataverses and Datasets, we can limit our search
        List<String> searchFilterQueries = savedSearch.getFilterQueriesAsStrings();        
        searchFilterQueries.add("dvObjectType:(dataverses OR datasets)");
                        
        // run the search as GuestUser to only link published objects
        SolrQueryResponse solrQueryResponse = searchService.search(
                new DataverseRequest(GuestUser.get(), getHttpServletRequest()),
                dataverses,
                savedSearch.getQuery(),
                searchFilterQueries,
                sortBy.getField(),
                sortBy.getOrder(),
                paginationStart,
                dataRelatedToMe,
                numResultsPerPage,
                false, // do not retrieve entities
                null,
                null
        );
        return solrQueryResponse;
    }

    private JsonObjectBuilder getInfo(SavedSearch savedSearch, JsonArrayBuilder infoPerHit) {
        JsonObjectBuilder info = Json.createObjectBuilder();
        info.add("definitionPointAlias", savedSearch.getDefinitionPoint().getAlias());
        info.add("savedSearchId", savedSearch.getId());
        info.add("hitInfo", infoPerHit);
        return info;
    }

    private JsonObjectBuilder getDebugInfo(SavedSearch savedSearch) {
        JsonObjectBuilder debug = Json.createObjectBuilder();
        debug.add("creatorId", savedSearch.getCreator().getId());
        debug.add("query", savedSearch.getQuery());
        debug.add("filterQueries", getFilterQueries(savedSearch));
        return debug;
    }

    private JsonArrayBuilder getFilterQueries(SavedSearch savedSearch) {
        JsonArrayBuilder filterQueriesArrayBuilder = Json.createArrayBuilder();
        for (String filterQueryToAdd : savedSearch.getFilterQueriesAsStrings()) {
            filterQueriesArrayBuilder.add(filterQueryToAdd);
        }
        return filterQueriesArrayBuilder;
    }

    private boolean datasetToLinkToIsAlreadyPartOfTheSubtree(Dataverse definitionPoint, Dataset datasetWeMayLinkTo) {
        Dataverse ancestor = datasetWeMayLinkTo.getOwner();
        while (ancestor != null) {
            if (ancestor.equals(definitionPoint)) {
                return true;
            }
            ancestor = ancestor.getOwner();
        }
        return false;
    }

    private boolean dataverseToLinkToIsAlreadyPartOfTheSubtree(Dataverse definitionPoint, Dataverse dataverseWeMayLinkTo) {
        StringBuilder sb = new StringBuilder();
        while (dataverseWeMayLinkTo != null) {
            String alias = dataverseWeMayLinkTo.getAlias();
            logger.fine("definitionPoint " + definitionPoint.getAlias() + " may link to " + alias);
            sb.append(alias + " ");
            if (dataverseWeMayLinkTo.equals(definitionPoint)) {
                return true;
            }
            dataverseWeMayLinkTo = dataverseWeMayLinkTo.getOwner();
        }
        logger.fine("dataverse aliases seen on the way to root: " + sb);
        return false;
    }

    /**
     * @todo Should we implement this? If so, also do the check at the files
     * level if there is a match on a file.
     */
    private boolean datasetAncestorAlreadyLinked(Dataverse definitionPoint, Dataset datasetToLinkTo) {
        return false;
    }

    public static HttpServletRequest getHttpServletRequest() {
        /**
         * This HttpServletRequest object is purposefully set to null. "There's
         * another issue here, though - the IP address. The request is sent from
         * a cron job - I assume localhost? - and it's source IP address is
         * different from the one the user may have, and quite possibly more
         * privileged. It maybe safest to pass in a null http request at this
         * stage." -- michbarsinai
         *
         * When Saved Search was designed, there was no DataverseRequest object
         * so what is persisted is the id of the AuthenticatedUser. When a Saved
         * Search is later re-executed via cron, the AuthenticatedUser is used
         * but Saved Search has no memory of which IP address was used when the
         * Saved Search was created. The default IP address in the
         * DataverseRequest constructor is used instead, which as of this
         * writing is 0.0.0.0 to mean "undefined". Is this a feature or a bug?
         * This is not an issue for the search itself, since it is now run as the
         * Guest User, but would present a problem if the user does not have
         * permission to create links and could only create the saved search due to 
         * a granted permission from the IP Group.
         * As of this writing Saved Search is a superuser-only feature; so IP
         * Groups are irrelevant because all superusers can create links.
         */
        return null;
    }

}
