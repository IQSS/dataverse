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
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SortBy;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

@Stateless
@Named
public class SavedSearchServiceBean {

    private static final Logger logger = Logger.getLogger(SavedSearchServiceBean.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DatasetLinkingServiceBean datasetLinkingService;
    @EJB
    DataverseLinkingServiceBean dataverseLinkingService;
    @EJB
    EjbDataverseEngine commandEngine;
    @Context
    HttpServletRequest httpReq;

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
            System.out.println("exeption: " + ex);
        }
        return persisted;
    }

    public boolean delete(long id) {
        SavedSearch doomed = find(id);
        boolean wasDeleted = false;
        if (doomed != null) {
            System.out.println("deleting saved search id " + doomed.getId());
            em.remove(doomed);
            em.flush();
            wasDeleted = true;
        } else {
            System.out.println("problem deleting saved search id " + id);
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

    public JsonObjectBuilder makeLinksForAllSavedSearches(boolean debugFlag) throws SearchException, CommandException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        List<SavedSearch> allSavedSearches = findAll();
        JsonArrayBuilder savedSearchArrayBuilder = Json.createArrayBuilder();
        for (SavedSearch savedSearch : allSavedSearches) {
            JsonObjectBuilder perSavedSearchResponse = makeLinksForSingleSavedSearch(savedSearch, debugFlag);
            savedSearchArrayBuilder.add(perSavedSearchResponse);
        }
        response.add("hits by saved search", savedSearchArrayBuilder);
        return response;
    }

    public JsonObjectBuilder makeLinksForSingleSavedSearch(SavedSearch savedSearch, boolean debugFlag) throws SearchException, CommandException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder savedSearchArrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder infoPerHit = Json.createArrayBuilder();
        SolrQueryResponse queryResponse = findHits(savedSearch);
        for (SolrSearchResult solrSearchResult : queryResponse.getSolrSearchResults()) {

            JsonObjectBuilder hitInfo = Json.createObjectBuilder();
            hitInfo.add("name", solrSearchResult.getNameSort());
            hitInfo.add("dvObjectId", solrSearchResult.getEntityId());

            DvObject dvObjectThatDefinitionPointWillLinkTo = dvObjectService.findDvObject(solrSearchResult.getEntityId());
            if (dvObjectThatDefinitionPointWillLinkTo == null) {
                hitInfo.add(resultString, "Could not find DvObject with id " + solrSearchResult.getEntityId());
                infoPerHit.add(hitInfo);
                break;
            }
            mutateHttpRequestSoJsfCanMakeLinks();
            DataverseRequest dvReq = new DataverseRequest(savedSearch.getCreator(), httpReq);
            if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataverse()) {
                Dataverse dataverseToLinkTo = (Dataverse) dvObjectThatDefinitionPointWillLinkTo;
                if (wouldResultInLinkingToItself(savedSearch.getDefinitionPoint(), dataverseToLinkTo)) {
                    hitInfo.add(resultString, "Skipping because dataverse id " + dataverseToLinkTo.getId() + " would link to itself.");
                } else if (alreadyLinkedToTheDataverse(savedSearch.getDefinitionPoint(), dataverseToLinkTo)) {
                    hitInfo.add(resultString, "Skipping because dataverse " + savedSearch.getDefinitionPoint().getId() + " already links to dataverse " + dataverseToLinkTo.getId() + ".");
                } else if (dataverseToLinkToIsAlreadyPartOfTheSubtree(savedSearch.getDefinitionPoint(), dataverseToLinkTo)) {
                    hitInfo.add(resultString, "Skipping because " + dataverseToLinkTo + " is already part of the subtree for " + savedSearch.getDefinitionPoint());
                } else {
                    DataverseLinkingDataverse link = commandEngine.submitInNewTransaction(new LinkDataverseCommand(dvReq, savedSearch.getDefinitionPoint(), dataverseToLinkTo));
                    hitInfo.add(resultString, "Persisted DataverseLinkingDataverse id " + link.getId() + " link of " + dataverseToLinkTo + " to " + savedSearch.getDefinitionPoint());
                }
            } else if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataset()) {
                Dataset datasetToLinkTo = (Dataset) dvObjectThatDefinitionPointWillLinkTo;
                if (alreadyLinkedToTheDataset(savedSearch.getDefinitionPoint(), datasetToLinkTo)) {
                    hitInfo.add(resultString, "Skipping because dataverse " + savedSearch.getDefinitionPoint() + " already links to dataset " + datasetToLinkTo + ".");
                } else if (datasetToLinkToIsAlreadyPartOfTheSubtree(savedSearch.getDefinitionPoint(), datasetToLinkTo)) {
                    // already there from normal search/browse
                    hitInfo.add(resultString, "Skipping because dataset " + datasetToLinkTo.getId() + " is already part of the subtree for " + savedSearch.getDefinitionPoint().getAlias());
                } else if (datasetAncestorAlreadyLinked(savedSearch.getDefinitionPoint(), datasetToLinkTo)) {
                    hitInfo.add(resultString, "FIXME: implement this?");
                } else {
                    DatasetLinkingDataverse link = commandEngine.submitInNewTransaction(new LinkDatasetCommand(dvReq, savedSearch.getDefinitionPoint(), datasetToLinkTo));
                    hitInfo.add(resultString, "Persisted DatasetLinkingDataverse id " + link.getId() + " link of " + link.getDataset() + " to " + link.getLinkingDataverse());
                }
            } else if (dvObjectThatDefinitionPointWillLinkTo.isInstanceofDataFile()) {
                hitInfo.add(resultString, "Skipping because the search matched a file. The matched file id was " + dvObjectThatDefinitionPointWillLinkTo.getId() + ".");
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
        return response;
    }

    /**
     * This method fixes the "java.lang.IllegalStateException: Not inside a
     * request scope" issue reported at
     * https://github.com/IQSS/dataverse/issues/2718 where it was impossible to
     * use the "Link Dataset" feature from JSF.
     */
    private void mutateHttpRequestSoJsfCanMakeLinks() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            httpReq = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        } else {
            /**
             * No-op. When the "makeLinksForSingleSavedSearch" method is called
             * from the API such as from `curl -X PUT
             * http://localhost:8080/api/admin/savedsearches/makelinks/all` is
             * is expected that facesContext will be null and it is not
             * necessary to mutate the httpReq. The links are able to be made
             * (in the datasetlinkingdataverse table, for example) and we don't
             * get "java.lang.IllegalStateException: Not inside a request
             * scope". It works fine.
             */
        }
    }

    private SolrQueryResponse findHits(SavedSearch savedSearch) throws SearchException {
        String sortField = SearchFields.RELEVANCE;
        String sortOrder = SortBy.DESCENDING;
        SortBy sortBy = new SortBy(sortField, sortOrder);
        int paginationStart = 0;
        boolean dataRelatedToMe = false;
        int numResultsPerPage = Integer.MAX_VALUE;
        SolrQueryResponse solrQueryResponse = searchService.search(
                savedSearch.getCreator(),
                savedSearch.getDefinitionPoint(),
                savedSearch.getQuery(),
                savedSearch.getFilterQueriesAsStrings(),
                sortBy.getField(),
                sortBy.getOrder(),
                paginationStart,
                dataRelatedToMe,
                numResultsPerPage
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

    private boolean alreadyLinkedToTheDataverse(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        return dataverseLinkingService.alreadyLinked(definitionPoint, dataverseToLinkTo);
    }

    private boolean alreadyLinkedToTheDataset(Dataverse definitionPoint, Dataset linkToThisDataset) {
        return datasetLinkingService.alreadyLinked(definitionPoint, linkToThisDataset);
    }

    private static boolean wouldResultInLinkingToItself(Dataverse savedSearchDefinitionPoint, Dataverse dataverseToLinkTo) {
        return savedSearchDefinitionPoint.equals(dataverseToLinkTo);
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

}
