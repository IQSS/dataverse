package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionRolledbackLocalException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

@Stateless
@Named
public class SolrSearchServiceBean implements SearchService {

    private static final Logger logger = Logger.getLogger(SolrSearchServiceBean.class.getCanonicalName());

    private static final String ALL_GROUPS = "*";

    /**
     * We're trying to make the SolrSearchServiceBean lean, mean, and fast, with as
     * few injections of EJBs as possible.
     */
    /**
     * @todo Can we do without the DatasetFieldServiceBean?
     */
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    GroupServiceBean groupService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    SolrClientService solrClientService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    
    
    @Override
    public String getServiceName() {
        return "solr";
    }
    
    @Override
    public String getDisplayName() {
        return "Dataverse Standard Search";
    }
    /**
     * @param dataverseRequest
     * @param dataverses
     * @param query
     * @param filterQueries
     * @param sortField
     * @param sortOrder
     * @param paginationStart
     * @param onlyDatatRelatedToMe
     * @param numResultsPerPage
     * @param retrieveEntities - look up dvobject entities with .find() (potentially expensive!)
     * @param geoPoint e.g. "35,15"
     * @param geoRadius e.g. "5"
     * @param addFacets boolean
     * @param addHighlights boolean
     * @return
     * @throws SearchException
     */
    @Override
    public SolrQueryResponse search(
            DataverseRequest dataverseRequest,
            List<Dataverse> dataverses,
            String query,
            List<String> filterQueries,
            String sortField, 
            String sortOrder,
            int paginationStart,
            boolean onlyDatatRelatedToMe,
            int numResultsPerPage,
            boolean retrieveEntities,
            String geoPoint,
            String geoRadius,
            boolean addFacets,
            boolean addHighlights
    ) throws SearchException {

        if (paginationStart < 0) {
            throw new IllegalArgumentException("paginationStart must be 0 or greater");
        }
        if (numResultsPerPage < 1) {
            throw new IllegalArgumentException("numResultsPerPage must be 1 or greater");
        }

        SolrQuery solrQuery = new SolrQuery();
        query = SearchUtil.sanitizeQuery(query);

        solrQuery.setQuery(query);
        if (sortField != null) {
            // is it ok not to specify any sort? - there are cases where we 
            // don't care, and it must cost some extra cycles -- L.A.
            solrQuery.setSort(new SortClause(sortField, sortOrder));
        }
        
        solrQuery.setParam("fl", "*,score");
        solrQuery.setParam("qt", "/select");
        solrQuery.setParam("facet", "true");
        
        /**
         * @todo: do we need facet.query?
         */
        solrQuery.setParam("facet.query", "*");
        solrQuery.addFacetField(SearchFields.TYPE); // this one is always performed

        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }
        if (geoPoint != null && !geoPoint.isBlank() && geoRadius != null && !geoRadius.isBlank()) {
            solrQuery.setParam("pt", geoPoint);
            solrQuery.setParam("d", geoRadius);
            // See https://solr.apache.org/guide/8_11/spatial-search.html#bbox
            solrQuery.addFilterQuery("{!bbox sfield=" + SearchFields.GEOLOCATION + "}");
        }
        
        List<DataverseMetadataBlockFacet> metadataBlockFacets = new LinkedList<>();

        if (addFacets) {

            

            // -----------------------------------
            // Facets to Retrieve
            // -----------------------------------
            solrQuery.addFacetField(SearchFields.METADATA_TYPES);
            solrQuery.addFacetField(SearchFields.DATASET_TYPE);
            solrQuery.addFacetField(SearchFields.DATAVERSE_CATEGORY);
            solrQuery.addFacetField(SearchFields.METADATA_SOURCE);
            solrQuery.addFacetField(SearchFields.PUBLICATION_YEAR);
            /*
            * We talked about this in slack on 2021-09-14, Users can see objects on draft/unpublished 
            *  if the owner gives permissions to all users so it makes sense to expose this facet 
            *  to all users. The request of this change started because the order of the facets were 
            *  changed with the PR #9635 and this was unintended.
            */
            solrQuery.addFacetField(SearchFields.PUBLICATION_STATUS);
            solrQuery.addFacetField(SearchFields.DATASET_LICENSE);
            /**
             * @todo when a new method on datasetFieldService is available
             * (retrieveFacetsByDataverse?) only show the facets that the
             * dataverse in question wants to show (and in the right order):
             * https://redmine.hmdc.harvard.edu/issues/3490
             *
             * also, findAll only returns advancedSearchField = true... we
             * should probably introduce the "isFacetable" boolean rather than
             * caring about if advancedSearchField is true or false
             *
             */

            if (dataverses != null) {
                for (Dataverse dataverse : dataverses) {
                    if (dataverse != null) {
                        for (DataverseFacet dataverseFacet : dataverse.getDataverseFacets()) {
                            DatasetFieldType datasetField = dataverseFacet.getDatasetFieldType();
                            solrQuery.addFacetField(datasetField.getSolrField().getNameFacetable());
                        }

                        // Get all metadata block facets configured to be displayed
                        metadataBlockFacets.addAll(dataverse.getMetadataBlockFacets());
                    }
                }
            }
            
            solrQuery.addFacetField(SearchFields.FILE_TYPE);
            /**
            * @todo: hide the extra line this shows in the GUI... at least it's
            * last...
            */
            solrQuery.addFacetField(SearchFields.FILE_TAG);
            if (!systemConfig.isPublicInstall()) {
                solrQuery.addFacetField(SearchFields.ACCESS);
            }
        }

        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        Map<String, String> solrFieldsToHightlightOnMap = new HashMap<>();
        if (addHighlights) {
            solrQuery.setHighlight(true).setHighlightSnippets(1).setHighlightRequireFieldMatch(true);
            Integer fragSize = systemConfig.getSearchHighlightFragmentSize();
            if (fragSize != null) {
                solrQuery.setHighlightFragsize(fragSize);
            }
            solrQuery.setHighlightSimplePre("<span class=\"search-term-match\">");
            solrQuery.setHighlightSimplePost("</span>");

            // TODO: Do not hard code "Name" etc as English here.
            solrFieldsToHightlightOnMap.put(SearchFields.NAME, "Name");
            solrFieldsToHightlightOnMap.put(SearchFields.AFFILIATION, "Affiliation");
            solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_FRIENDLY, "File Type");
            solrFieldsToHightlightOnMap.put(SearchFields.DESCRIPTION, "Description");
            solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_NAME, "Variable Name");
            solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_LABEL, "Variable Label");
            solrFieldsToHightlightOnMap.put(SearchFields.LITERAL_QUESTION, BundleUtil.getStringFromBundle("search.datasets.literalquestion"));
            solrFieldsToHightlightOnMap.put(SearchFields.INTERVIEW_INSTRUCTIONS, BundleUtil.getStringFromBundle("search.datasets.interviewinstructions"));
            solrFieldsToHightlightOnMap.put(SearchFields.POST_QUESTION, BundleUtil.getStringFromBundle("search.datasets.postquestion"));
            solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_UNIVERSE, BundleUtil.getStringFromBundle("search.datasets.variableuniverse"));
            solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_NOTES, BundleUtil.getStringFromBundle("search.datasets.variableNotes"));

            solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_SEARCHABLE, "File Type");
            solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PUBLICATION_DATE, "Publication Year");
            solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.datasets.persistentId"));
            solrFieldsToHightlightOnMap.put(SearchFields.FILE_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.files.persistentId"));
            /**
             * @todo Dataverse subject and affiliation should be highlighted but
             * this is commented out right now because the "friendly" names are
             * not being shown on the dataverse cards. See also
             * https://github.com/IQSS/dataverse/issues/1431
             */
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_SUBJECT, "Subject");
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_AFFILIATION, "Affiliation");
            /**
             * @todo: show highlight on file card?
             * https://redmine.hmdc.harvard.edu/issues/3848
             */
            solrFieldsToHightlightOnMap.put(SearchFields.FILENAME_WITHOUT_EXTENSION, "Filename Without Extension");
            solrFieldsToHightlightOnMap.put(SearchFields.FILE_TAG_SEARCHABLE, "File Tag");

            for (DatasetFieldType datasetFieldType : datasetFields) {
                String solrField = datasetFieldType.getSolrField().getNameSearchable();
                String displayName = datasetFieldType.getDisplayName();
                solrFieldsToHightlightOnMap.put(solrField, displayName);
            }
            for (Map.Entry<String, String> entry : solrFieldsToHightlightOnMap.entrySet()) {
                String solrField = entry.getKey();
                // String displayName = entry.getValue();
                solrQuery.addHighlightField(solrField);
            }
        }

        // -----------------------------------
        // PERMISSION FILTER QUERY
        // -----------------------------------
        String permissionFilterQuery = getPermissionFilterQuery(dataverseRequest, solrQuery, onlyDatatRelatedToMe, addFacets);
        if (!permissionFilterQuery.isEmpty()) {
            String[] filterParts = permissionFilterQuery.split("&q1=");
            solrQuery.addFilterQuery(filterParts[0]);
            if(filterParts.length > 1 ) {
                solrQuery.add("q1", filterParts[1]);
            }
        }
        
        /**
         * @todo: do sanity checking... throw error if negative
         */
        solrQuery.setStart(paginationStart);
        /**
         * @todo: decide if year CITATION_YEAR is good enough or if we should
         * support CITATION_DATE
         */
//        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.UK);
//        calendar.set(2010, 1, 1);
//        Date start = calendar.getTime();
//        calendar.set(2013, 1, 1);
//        Date end = calendar.getTime();
//        solrQuery.addDateRangeFacet(SearchFields.CITATION_DATE, start, end, "+1MONTH");
        /**
         * @todo make this configurable
         */
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        /**
         * @todo: odd or even makes a difference. Couldn't find value of 2014
         * when this was set to 2000
         */
        final int citationYearRangeStart = 1901;
        final int citationYearRangeEnd = thisYear;
        final int citationYearRangeSpan = 2;
        /**
         * @todo: these are dates and should be "range facets" not "field
         * facets"
         *
         * right now they are lumped in with the datasetFieldService.findAll()
         * above
         */
//        solrQuery.addNumericRangeFacet(SearchFields.PRODUCTION_DATE_YEAR_ONLY, citationYearRangeStart, citationYearRangeEnd, citationYearRangeSpan);
//        solrQuery.addNumericRangeFacet(SearchFields.DISTRIBUTION_DATE_YEAR_ONLY, citationYearRangeStart, citationYearRangeEnd, citationYearRangeSpan);
        solrQuery.setRows(numResultsPerPage);
        logger.fine("Solr query:" + solrQuery);

        // -----------------------------------
        // Make the solr query
        // -----------------------------------
        QueryResponse queryResponse = null;
        
        try {
            queryResponse = solrClientService.getSolrClient().query(solrQuery);

        } catch (RemoteSolrException ex) {
            String messageFromSolr = ex.getLocalizedMessage();
            
            logger.fine("message from the solr exception: "+messageFromSolr+"; code: "+ex.code());
            
            SolrQueryResponse exceptionSolrQueryResponse = new SolrQueryResponse(solrQuery);

            // We probably shouldn't be assuming that this is necessarily a 
            // "search syntax error", as the code below implies - could be 
            // something else too - ? 
            
            // Specifically, we now rely on the Solr "circuit breaker" mechanism
            // to start dropping requests with 503, when the service is 
            // overwhelmed with requests load (with the assumption that this is
            // a transient condition): 
            
            if (ex.code() == 503) {
                // actual logic for communicating this state back to the local 
                // client code TBD (@todo)
                exceptionSolrQueryResponse.setSolrTemporarilyUnavailable(true);
            }
            
            String error = "Search Syntax Error: ";
            String stringToHide = "org.apache.solr.search.SyntaxError: ";
            if (messageFromSolr.startsWith(stringToHide)) {
                // hide "org.apache.solr..."
                error += messageFromSolr.substring(stringToHide.length());
            } else {
                error += messageFromSolr;
            }
            logger.info(error);
            exceptionSolrQueryResponse.setError(error);

            // we can't show anything because of the search syntax error
                        
            long zeroNumResultsFound = 0;
            long zeroGetResultsStart = 0;
            List<SolrSearchResult> emptySolrSearchResults = new ArrayList<>();
            List<FacetCategory> exceptionFacetCategoryList = new ArrayList<>();
            Map<String, List<String>> emptySpellingSuggestion = new HashMap<>();
            exceptionSolrQueryResponse.setNumResultsFound(zeroNumResultsFound);
            exceptionSolrQueryResponse.setResultsStart(zeroGetResultsStart);
            exceptionSolrQueryResponse.setSolrSearchResults(emptySolrSearchResults);
            exceptionSolrQueryResponse.setFacetCategoryList(exceptionFacetCategoryList);
            exceptionSolrQueryResponse.setTypeFacetCategories(exceptionFacetCategoryList);
            exceptionSolrQueryResponse.setSpellingSuggestionsByToken(emptySpellingSuggestion);
            return exceptionSolrQueryResponse;
        } catch (SolrServerException | IOException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }
        
        int statusCode = queryResponse.getStatus();
        
        logger.fine("status code of the query response: "+statusCode);
        logger.fine("_size from query response: "+queryResponse._size());
        logger.fine("qtime: "+queryResponse.getQTime());

        SolrDocumentList docs = queryResponse.getResults();
        List<SolrSearchResult> solrSearchResults = new ArrayList<>();

        /**
         * @todo refactor SearchFields to a hashmap (or something? put in
         * database? internationalize?) to avoid the crazy reflection and string
         * manipulation below
         */
        Object searchFieldsObject = new SearchFields();
        Field[] staticSearchFields = searchFieldsObject.getClass().getDeclaredFields();
        String titleSolrField = null;
        try {
            DatasetFieldType titleDatasetField = datasetFieldService.findByName(DatasetFieldConstant.title);
            titleSolrField = titleDatasetField.getSolrField().getNameSearchable();
        } catch (EJBTransactionRolledbackException ex) {
            logger.info("Couldn't find " + DatasetFieldConstant.title);
            if (ex.getCause() instanceof TransactionRolledbackLocalException) {
                if (ex.getCause().getCause() instanceof NoResultException) {
                    logger.info("Caught NoResultException");
                }
            }
        }
        Map<String, String> datasetfieldFriendlyNamesBySolrField = new HashMap<>();
        Map<String, String> staticSolrFieldFriendlyNamesBySolrField = new HashMap<>();
        String baseUrl = systemConfig.getDataverseSiteUrl();

        //Going through the results
        for (SolrDocument solrDocument : docs) {
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String type = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            float score = (Float) solrDocument.getFieldValue(SearchFields.RELEVANCE);
            logger.fine("score for " + id + ": " + score);
            String identifier = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER);
            String citation = (String) solrDocument.getFieldValue(SearchFields.DATASET_CITATION);
            String citationPlainHtml = (String) solrDocument.getFieldValue(SearchFields.DATASET_CITATION_HTML);
            String datasetType = (String) solrDocument.getFieldValue(SearchFields.DATASET_TYPE);
            String persistentUrl = (String) solrDocument.getFieldValue(SearchFields.PERSISTENT_URL);
            String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            String nameSort = (String) solrDocument.getFieldValue(SearchFields.NAME_SORT);
//            ArrayList titles = (ArrayList) solrDocument.getFieldValues(SearchFields.TITLE);
            String title = (String) solrDocument.getFieldValue(titleSolrField);
            Long datasetVersionId = (Long) solrDocument.getFieldValue(SearchFields.DATASET_VERSION_ID);
            String deaccessionReason = (String) solrDocument.getFieldValue(SearchFields.DATASET_DEACCESSION_REASON);
//            logger.info("titleSolrField: " + titleSolrField);
//            logger.info("title: " + title);
            String filetype = (String) solrDocument.getFieldValue(SearchFields.FILE_TYPE_FRIENDLY);
            String fileContentType = (String) solrDocument.getFieldValue(SearchFields.FILE_CONTENT_TYPE);
            Date release_or_create_date = (Date) solrDocument.getFieldValue(SearchFields.RELEASE_OR_CREATE_DATE);
            String dvTree = (String) solrDocument.getFirstValue(SearchFields.SUBTREE);
            String identifierOfDataverse = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER_OF_DATAVERSE);
            String nameOfDataverse = (String) solrDocument.getFieldValue(SearchFields.DATAVERSE_NAME);
            String dataverseAffiliation = (String) solrDocument.getFieldValue(SearchFields.DATAVERSE_AFFILIATION);
            String dataverseParentAlias = (String) solrDocument.getFieldValue(SearchFields.DATAVERSE_PARENT_ALIAS);
            String dataverseParentName = (String) solrDocument.getFieldValue(SearchFields.PARENT_NAME);
            Long embargoEndDate = (Long) solrDocument.getFieldValue(SearchFields.EMBARGO_END_DATE);
            Long retentionEndDate = (Long) solrDocument.getFieldValue(SearchFields.RETENTION_END_DATE);
            //
            Boolean datasetValid = (Boolean) solrDocument.getFieldValue(SearchFields.DATASET_VALID);
            Long fileCount = (Long) solrDocument.getFieldValue(SearchFields.FILE_COUNT);
            
            List<String> matchedFields = new ArrayList<>();
            
            SolrSearchResult solrSearchResult = new SolrSearchResult(query, name);
            
            if (addHighlights) {
                List<Highlight> highlights = new ArrayList<>();
                Map<SolrField, Highlight> highlightsMap = new HashMap<>();
                Map<SolrField, List<String>> highlightsMap2 = new HashMap<>();
                Map<String, Highlight> highlightsMap3 = new HashMap<>();
                if (queryResponse.getHighlighting().get(id) != null) {
                    for (Map.Entry<String, String> entry : solrFieldsToHightlightOnMap.entrySet()) {
                        String field = entry.getKey();
                        String displayName = entry.getValue();

                        List<String> highlightSnippets = queryResponse.getHighlighting().get(id).get(field);
                        if (highlightSnippets != null) {
                            matchedFields.add(field);
                            /**
                             * @todo only SolrField.SolrType.STRING? that's not
                             * right... knit the SolrField object more into the
                             * highlighting stuff
                             */
                            SolrField solrField = new SolrField(field, SolrField.SolrType.STRING, true, true);
                            Highlight highlight = new Highlight(solrField, highlightSnippets, displayName);
                            highlights.add(highlight);
                            highlightsMap.put(solrField, highlight);
                            highlightsMap2.put(solrField, highlightSnippets);
                            highlightsMap3.put(field, highlight);
                        }
                    }

                }

                solrSearchResult.setHighlightsAsList(highlights);
                solrSearchResult.setHighlightsMap(highlightsMap);
                solrSearchResult.setHighlightsAsMap(highlightsMap3);
            }
            
            
            /**
             * @todo put all this in the constructor?
             */
            List<String> states = (List<String>) solrDocument.getFieldValue(SearchFields.PUBLICATION_STATUS);
            if (states != null) {
                // set list of all statuses
                // this method also sets booleans for individual statuses
                solrSearchResult.setPublicationStatuses(states);
            }
            String externalStatus = (String) solrDocument.getFieldValue(SearchFields.EXTERNAL_STATUS);
            if (externalStatus != null) {
                solrSearchResult.setExternalStatus(externalStatus);
            }
//            logger.info(id + ": " + description);
            solrSearchResult.setId(id);
            solrSearchResult.setEntityId(entityid);
            if (retrieveEntities) {
                solrSearchResult.setEntity(dvObjectService.findDvObject(entityid));
            }
            solrSearchResult.setIdentifier(identifier);
            solrSearchResult.setPersistentUrl(persistentUrl);
            solrSearchResult.setType(type);
            solrSearchResult.setScore(score);
            solrSearchResult.setNameSort(nameSort);
            solrSearchResult.setReleaseOrCreateDate(release_or_create_date);
            solrSearchResult.setMatchedFields(matchedFields);
            
            Map<String, String> parent = new HashMap<>();
            String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
            solrSearchResult.setDescriptionNoSnippet(description);
            solrSearchResult.setDeaccessionReason(deaccessionReason);
            solrSearchResult.setDvTree(dvTree);
            solrSearchResult.setDatasetValid(datasetValid);
            solrSearchResult.setFileCount(fileCount);

            if (Boolean.TRUE.equals((Boolean) solrDocument.getFieldValue(SearchFields.IS_HARVESTED))) {
                solrSearchResult.setHarvested(true);
            }

            solrSearchResult.setEmbargoEndDate(embargoEndDate);
            solrSearchResult.setRetentionEndDate(retentionEndDate);

            /**
             * @todo start using SearchConstants class here
             */
            if (type.equals("dataverses")) {
                solrSearchResult.setName(name);
                solrSearchResult.setHtmlUrl(baseUrl + SystemConfig.DATAVERSE_PATH + identifier);
                solrSearchResult.setDataverseAffiliation(dataverseAffiliation);
                solrSearchResult.setDataverseParentAlias(dataverseParentAlias);
                solrSearchResult.setDataverseParentName(dataverseParentName);
                solrSearchResult.setImageUrl(thumbnailServiceWrapper.getDataverseCardImageAsUrl(solrSearchResult));
                /**
                 * @todo Expose this API URL after "dvs" is changed to
                 * "dataverses". Also, is an API token required for published
                 * dataverses? Michael: url changed.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/dataverses/" + entityid);
            } else if (type.equals("datasets")) {
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?globalId=" + identifier);
                solrSearchResult.setApiUrl(baseUrl + "/api/datasets/" + entityid);
                solrSearchResult.setImageUrl(thumbnailServiceWrapper.getDatasetCardImageAsUrl(solrSearchResult));
                //Image url now set via thumbnail api
                //solrSearchResult.setImageUrl(baseUrl + "/api/access/dsCardImage/" + datasetVersionId);
                // No, we don't want to set the base64 thumbnails here.
                // We want to do it inside SearchIncludeFragment, AND ONLY once the rest of the
                // page has already loaded.
                //DatasetVersion datasetVersion = datasetVersionService.find(datasetVersionId);
                //if (datasetVersion != null){
                //    solrSearchResult.setDatasetThumbnail(datasetVersion.getDataset().getDatasetThumbnail(datasetVersion));
                //}
                /**
                 * @todo Could use getFieldValues (plural) here.
                 */
                List<String> datasetDescriptions = (List<String>) solrDocument.getFieldValue(SearchFields.DATASET_DESCRIPTION);
                if (datasetDescriptions != null) {
                    String firstDatasetDescription = datasetDescriptions.get(0);
                    if (firstDatasetDescription != null) {
                        solrSearchResult.setDescriptionNoSnippet(String.join(" ", datasetDescriptions));
                    }
                }
                solrSearchResult.setDatasetVersionId(datasetVersionId);

                solrSearchResult.setCitation(citation);
                solrSearchResult.setCitationHtml(citationPlainHtml);

                solrSearchResult.setIdentifierOfDataverse(identifierOfDataverse);
                solrSearchResult.setNameOfDataverse(nameOfDataverse);

                if (title != null) {
//                    solrSearchResult.setTitle((String) titles.get(0));
                    solrSearchResult.setTitle(title);
                } else {
                    logger.fine("No title indexed. Setting to empty string to prevent NPE. Dataset id " + entityid + " and version id " + datasetVersionId);
                    solrSearchResult.setTitle("");
                }
                List<String> authors = (List) solrDocument.getFieldValues(DatasetFieldConstant.authorName);
                if (authors != null) {
                    solrSearchResult.setDatasetAuthors(authors);
                }
                solrSearchResult.setDatasetType(datasetType);
            } else if (type.equals("files")) {
                String parentGlobalId = null;
                Object parentGlobalIdObject = solrDocument.getFieldValue(SearchFields.PARENT_IDENTIFIER);
                if (parentGlobalIdObject != null) {
                    parentGlobalId = (String) parentGlobalIdObject;
                    parent.put(SolrSearchResult.PARENT_IDENTIFIER, parentGlobalId);
                }
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?persistentId=" + parentGlobalId);
                solrSearchResult.setDownloadUrl(baseUrl + "/api/access/datafile/" + entityid);
                solrSearchResult.setImageUrl(thumbnailServiceWrapper.getFileCardImageAsUrl(solrSearchResult));
                /**
                 * @todo We are not yet setting the API URL for files because
                 * not all files have metadata. Only subsettable files (those
                 * with a datatable) seem to have metadata. Furthermore, the
                 * response is in XML whereas the rest of the Search API returns
                 * JSON.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/meta/datafile/" + entityid);
                //solrSearchResult.setImageUrl(baseUrl + "/api/access/fileCardImage/" + entityid);
                solrSearchResult.setName(name);
                solrSearchResult.setFiletype(filetype);
                solrSearchResult.setFileContentType(fileContentType);
                Object fileSizeInBytesObject = solrDocument.getFieldValue(SearchFields.FILE_SIZE_IN_BYTES);
                if (fileSizeInBytesObject != null) {
                    try {
                        long fileSizeInBytesLong = (long) fileSizeInBytesObject;
                        solrSearchResult.setFileSizeInBytes(fileSizeInBytesLong);
                    } catch (ClassCastException ex) {
                        logger.info("Could not cast file " + entityid + " to long for " + SearchFields.FILE_SIZE_IN_BYTES + ": " + ex.getLocalizedMessage());
                    }
                }
                solrSearchResult.setFileMd5((String) solrDocument.getFieldValue(SearchFields.FILE_MD5));
                try {
                    solrSearchResult.setFileChecksumType(DataFile.ChecksumType.fromString((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_TYPE)));
                } catch (IllegalArgumentException ex) {
                    logger.info("Exception setting setFileChecksumType: " + ex);
                }
                solrSearchResult.setFileChecksumValue((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_VALUE));

                if (solrDocument.getFieldValue(SearchFields.FILE_RESTRICTED) != null) {
                    solrSearchResult.setFileRestricted((Boolean) solrDocument.getFieldValue(SearchFields.FILE_RESTRICTED));
                }

                if (solrSearchResult.getEntity() != null) {
                    solrSearchResult.setCanDownloadFile(permissionService.hasPermissionsFor(dataverseRequest, solrSearchResult.getEntity(), EnumSet.of(Permission.DownloadFile)));
                }

                solrSearchResult.setUnf((String) solrDocument.getFieldValue(SearchFields.UNF));
                solrSearchResult.setDatasetVersionId(datasetVersionId);
                List<String> fileCategories = (List) solrDocument.getFieldValues(SearchFields.FILE_TAG);
                if (fileCategories != null) {
                    solrSearchResult.setFileCategories(fileCategories);
                }
                List<String> tabularDataTags = (List) solrDocument.getFieldValues(SearchFields.TABDATA_TAG);
                if (tabularDataTags != null) {
                    Collections.sort(tabularDataTags);
                    solrSearchResult.setTabularDataTags(tabularDataTags);
                }
                Long observations = (Long) solrDocument.getFieldValue(SearchFields.OBSERVATIONS);
                solrSearchResult.setObservations(observations);
                Long tabCount = (Long) solrDocument.getFieldValue(SearchFields.VARIABLE_COUNT);
                solrSearchResult.setTabularDataCount(tabCount);
                String filePID = (String) solrDocument.getFieldValue(SearchFields.FILE_PERSISTENT_ID);
                if(null != filePID && !"".equals(filePID) && !"".equals("null")) {
                    solrSearchResult.setFilePersistentId(filePID);
                }
            }
            /**
             * @todo store PARENT_ID as a long instead and cast as such
             */
            parent.put("id", (String) solrDocument.getFieldValue(SearchFields.PARENT_ID));
            parent.put("name", (String) solrDocument.getFieldValue(SearchFields.PARENT_NAME));
            parent.put("citation", (String) solrDocument.getFieldValue(SearchFields.PARENT_CITATION));
            solrSearchResult.setParent(parent);
            solrSearchResults.add(solrSearchResult);
        }
        Map<String, List<String>> spellingSuggestionsByToken = new HashMap<>();
        SpellCheckResponse spellCheckResponse = queryResponse.getSpellCheckResponse();
        if (spellCheckResponse != null) {
            List<SpellCheckResponse.Suggestion> suggestions = spellCheckResponse.getSuggestions();
            for (SpellCheckResponse.Suggestion suggestion : suggestions) {
                spellingSuggestionsByToken.put(suggestion.getToken(), suggestion.getAlternatives());
            }
        }

        List<FacetCategory> facetCategoryList = new ArrayList<>();
        List<FacetCategory> typeFacetCategories = new ArrayList<>();
        boolean hidePublicationStatusFacet = true;
        boolean draftsAvailable = false;
        boolean unpublishedAvailable = false;
        boolean deaccessionedAvailable = false;
        boolean hideMetadataSourceFacet = true;
        boolean hideLicenseFacet = true;
        boolean hideDatasetTypeFacet = true;
        for (FacetField facetField : queryResponse.getFacetFields()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
            int numMetadataSources = 0;
            int numLicenses = 0;
            int numDatasetTypes = 0;
            String metadataBlockName = "";
            String datasetFieldName = "";
            /**
             * To find the metadata block name to which the facetField belongs to
             * ===facetField: authorName_ss   metadatablockname : citation
             * ===facetField: dvCategory  metadatablockname : ""
             */
            for (DatasetFieldType datasetField : datasetFields) {
                String solrFieldNameForDataset = datasetField.getSolrField().getNameFacetable();
                if (solrFieldNameForDataset != null && facetField.getName().equals(solrFieldNameForDataset)) {
                    metadataBlockName = datasetField.getMetadataBlock().getName() ;
                    datasetFieldName = datasetField.getName();
                    facetCategory.setDatasetFieldTypeId(datasetField.getId());
                    break;
                }
            }


            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                /**
                 * @todo we do want to show the count for each facet
                 */
//                logger.info("field: " + facetField.getName() + " " + facetFieldCount.getName() + " (" + facetFieldCount.getCount() + ")");
                String localefriendlyName = null;
                if (facetFieldCount.getCount() > 0) {
                    if(metadataBlockName.length() > 0 ) {
                        localefriendlyName = getLocaleTitle(datasetFieldName,facetFieldCount.getName(), metadataBlockName);
                    } else if (facetField.getName().equals(SearchFields.METADATA_TYPES)) {
                        Optional<DataverseMetadataBlockFacet> metadataBlockFacet = metadataBlockFacets.stream().filter(blockFacet -> blockFacet.getMetadataBlock().getName().equals(facetFieldCount.getName())).findFirst();
                        if (metadataBlockFacet.isEmpty()) {
                           // metadata block facet is not configured to be displayed => ignore
                           continue;
                        }

                        localefriendlyName = metadataBlockFacet.get().getMetadataBlock().getLocaleDisplayFacet();
                    } else if (facetField.getName().equals(SearchFields.DATASET_LICENSE)) {
                        try {
                            localefriendlyName = BundleUtil.getStringFromPropertyFile("license." + facetFieldCount.getName().toLowerCase().replace(" ","_") + ".name", "License");
                        } catch (Exception e) {
                            localefriendlyName = facetFieldCount.getName();
                        }
                    } else {
                        try {
                            // This is where facets are capitalized.
                            // This will be a problem for the API clients because they get back a string like this from the Search API...
                            // {"datasetType":{"friendly":"Dataset Type","labels":[{"Dataset":1},{"Software":1}]}
                            // ... but they will need to use the lower case version (e.g. "software") to narrow results.
                           localefriendlyName = BundleUtil.getStringFromPropertyFile(facetFieldCount.getName(), "Bundle");
                        } catch (Exception e) {
                           localefriendlyName = facetFieldCount.getName();
                        }
                    }
                    FacetLabel facetLabel = new FacetLabel(localefriendlyName, facetFieldCount.getCount());
                    // quote field facets
                    facetLabel.setFilterQuery(facetField.getName() + ":\"" + facetFieldCount.getName() + "\"");
                    facetLabelList.add(facetLabel);
                    if (facetField.getName().equals(SearchFields.PUBLICATION_STATUS)) {
                        if (facetFieldCount.getName().equals(IndexServiceBean.getUNPUBLISHED_STRING())) {
                            unpublishedAvailable = true;
                        } else if (facetFieldCount.getName().equals(IndexServiceBean.getDRAFT_STRING())) {
                            draftsAvailable = true;
                        } else if (facetFieldCount.getName().equals(IndexServiceBean.getDEACCESSIONED_STRING())) {
                            deaccessionedAvailable = true;
                        }
                    } else if (facetField.getName().equals(SearchFields.METADATA_SOURCE)) {
                        numMetadataSources++;
                    } else if (facetField.getName().equals(SearchFields.DATASET_LICENSE)) {
                        numLicenses++;
                    } else if (facetField.getName().equals(SearchFields.DATASET_TYPE)) {
                        numDatasetTypes++;
                    }
                }
            }
            if (numMetadataSources > 1) {
                hideMetadataSourceFacet = false;
            }
            if (numLicenses > 1) {
                hideLicenseFacet = false;
            }
            if (numDatasetTypes > 1 ) {
                hideDatasetTypeFacet = false;
            }
            facetCategory.setName(facetField.getName());
            // hopefully people will never see the raw facetField.getName() because it may well have an _s at the end
            facetCategory.setFriendlyName(facetField.getName());
            // try to find a friendlier name to display as a facet
            /**
             * @todo hmm, we thought we wanted the datasetFields array to go
             * away once we have more granularity than findAll() available per
             * the todo above but we need a way to lookup by Solr field, so
             * we'll build a hashmap
             */
            for (DatasetFieldType datasetField : datasetFields) {
                String solrFieldNameForDataset = datasetField.getSolrField().getNameFacetable();
                String friendlyName = datasetField.getDisplayName();
                if (solrFieldNameForDataset != null && facetField.getName().endsWith(datasetField.getTmpNullFieldTypeIdentifier())) {
                    // give it the non-friendly name so we remember to update the reference data script for datasets
                    facetCategory.setName(facetField.getName());
                } else if (solrFieldNameForDataset != null && facetField.getName().equals(solrFieldNameForDataset)) {
                    if (friendlyName != null && !friendlyName.isEmpty()) {
                        facetCategory.setFriendlyName(friendlyName);
                        // stop examining available dataset fields. we found a match
                        break;
                    }
                }
                datasetfieldFriendlyNamesBySolrField.put(datasetField.getSolrField().getNameFacetable(), friendlyName);
            }
            /**
             * @todo get rid of this crazy reflection, per todo above... or
             * should we... let's put into a hash the friendly names of facet
             * categories, indexed by Solr field
             */
            for (Field fieldObject : staticSearchFields) {
                String name = fieldObject.getName();
                String staticSearchField = null;
                try {
                    staticSearchField = (String) fieldObject.get(searchFieldsObject);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SolrSearchServiceBean.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (staticSearchField != null && facetField.getName().equals(staticSearchField)) {
                    String friendlyName = BundleUtil.getStringFromPropertyFile("staticSearchFields."+staticSearchField, "staticSearchFields");
                    if(friendlyName != null && friendlyName.length() > 0) {
                        facetCategory.setFriendlyName(friendlyName);
                    } else {
                        String[] parts = name.split("_");
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String part : parts) {
                            stringBuilder.append(getCapitalizedName(part.toLowerCase()) + " ");
                        }
                        String friendlyNameWithTrailingSpace = stringBuilder.toString();
                        friendlyName = friendlyNameWithTrailingSpace.replaceAll(" $", "");
                        facetCategory.setFriendlyName(friendlyName);
                    }

//                    logger.info("adding <<<" + staticSearchField + ":" + friendlyName + ">>>");
                    staticSolrFieldFriendlyNamesBySolrField.put(staticSearchField, friendlyName);
                    // stop examining the declared/static fields in the SearchFields object. we found a match
                    break;
                }
            }

            facetCategory.setFacetLabel(facetLabelList);
            if (!facetLabelList.isEmpty()) {
                if (facetCategory.getName().equals(SearchFields.TYPE)) {
                    // the "type" facet is special, these are not
                    typeFacetCategories.add(facetCategory);
                } else if (facetCategory.getName().equals(SearchFields.PUBLICATION_STATUS)) {
                    if (unpublishedAvailable || draftsAvailable || deaccessionedAvailable) {
                        hidePublicationStatusFacet = false;
                    }
                    if (!hidePublicationStatusFacet) {
                        facetCategoryList.add(facetCategory);
                    }
                } else if (facetCategory.getName().equals(SearchFields.METADATA_SOURCE)) {
                    if (!hideMetadataSourceFacet) {
                        facetCategoryList.add(facetCategory);
                    }
                } else if (facetCategory.getName().equals(SearchFields.DATASET_LICENSE)) {
                    if (!hideLicenseFacet) {
                        facetCategoryList.add(facetCategory);
                    }
                } else if (facetCategory.getName().equals(SearchFields.DATASET_TYPE)) {
                    if (!hideDatasetTypeFacet) {
                        facetCategoryList.add(facetCategory);
                    }
                } else {
                    facetCategoryList.add(facetCategory);
                }
            }
        }

        // for now the only range facet is citation year
        for (RangeFacet<String, String> rangeFacet : queryResponse.getFacetRanges()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
            for (Object rfObj : rangeFacet.getCounts()) {
                RangeFacet.Count rangeFacetCount = (RangeFacet.Count) rfObj;
                String valueString = rangeFacetCount.getValue();
                Integer start = Integer.parseInt(valueString);
                Integer end = start + Integer.parseInt(rangeFacet.getGap().toString());
                // to avoid overlapping dates
                end = end - 1;
                if (rangeFacetCount.getCount() > 0) {
                    FacetLabel facetLabel = new FacetLabel(start + "-" + end, Long.valueOf(rangeFacetCount.getCount()));
                    // special [12 TO 34] syntax for range facets
                    facetLabel.setFilterQuery(rangeFacet.getName() + ":" + "[" + start + " TO " + end + "]");
                    facetLabelList.add(facetLabel);
                }
            }
            facetCategory.setName(rangeFacet.getName());
            facetCategory.setFacetLabel(facetLabelList);
            // reverse to show the newest citation year range at the top
            List<FacetLabel> facetLabelListReversed = new ArrayList<>();
            ListIterator<FacetLabel> li = facetLabelList.listIterator(facetLabelList.size());
            while (li.hasPrevious()) {
                facetLabelListReversed.add(li.previous());
            }
            facetCategory.setFacetLabel(facetLabelListReversed);
            if (!facetLabelList.isEmpty()) {
                facetCategoryList.add(facetCategory);
            }
        }

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse(solrQuery);
        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        solrQueryResponse.setFacetCategoryList(facetCategoryList);
        solrQueryResponse.setTypeFacetCategories(typeFacetCategories);
        solrQueryResponse.setNumResultsFound(queryResponse.getResults().getNumFound());
        solrQueryResponse.setResultsStart(queryResponse.getResults().getStart());
        solrQueryResponse.setDatasetfieldFriendlyNamesBySolrField(datasetfieldFriendlyNamesBySolrField);
        solrQueryResponse.setStaticSolrFieldFriendlyNamesBySolrField(staticSolrFieldFriendlyNamesBySolrField);
        String[] filterQueriesArray = solrQuery.getFilterQueries();
        if (filterQueriesArray != null) {
            // null check added because these tests were failing: mvn test -Dtest=SearchIT
            List<String> actualFilterQueries = Arrays.asList(filterQueriesArray);
            logger.fine("actual filter queries: " + actualFilterQueries);
            solrQueryResponse.setFilterQueriesActual(actualFilterQueries);
        } else {
            // how often is this null?
            logger.info("solrQuery.getFilterQueries() was null");
        }

        solrQueryResponse.setDvObjectCounts(queryResponse.getFacetField("dvObjectType"));
        solrQueryResponse.setPublicationStatusCounts(queryResponse.getFacetField("publicationStatus"));

        return solrQueryResponse;
    }

    public String getLocaleTitle(String title,  String controlledvoc , String propertyfile) {

        String output = "";
        try {
            if(controlledvoc != "" ) {
                output =  BundleUtil.getStringFromPropertyFile("controlledvocabulary." + title +"."+ controlledvoc.toLowerCase().replace(" ","_")  , propertyfile);
            } else {
                output = BundleUtil.getStringFromPropertyFile("datasetfieldtype." + title + ".title", propertyfile);
            }
        } catch (MissingResourceException e1) {
            if(controlledvoc != "" ) {
                return controlledvoc;
            } else {
                return title;
            }
        }

        if(output != null && output.length() >0) {
            return output;
        }
        else
            return title;
    }


    public String getCapitalizedName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Moved this logic out of the "search" function
     *
     * @return
     */
    private String getPermissionFilterQuery(DataverseRequest dataverseRequest, SolrQuery solrQuery, boolean onlyDatatRelatedToMe, boolean addFacets) {

        User user = dataverseRequest.getUser();
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }
        if (solrQuery == null) {
            throw new NullPointerException("solrQuery cannot be null");
        }
        
        if (user instanceof PrivateUrlUser) {
            user = GuestUser.get();
        }

        ArrayList<String> groupList = new ArrayList<String>();
        AuthenticatedUser au = null;
        Set<Group> groups;

        boolean avoidJoin = FeatureFlags.AVOID_EXPENSIVE_SOLR_JOIN.enabled();

        if (user instanceof AuthenticatedUser) {
            au = (AuthenticatedUser) user;

            // ----------------------------------------------------
            // Is this a Super User?
            // If so, they can see everything
            // ----------------------------------------------------
            if (au.isSuperuser()) {
                // Somewhat dangerous because this user (a superuser) will be able
                // to see everything in Solr with no regard to permissions. But it's
                // been this way since Dataverse 4.0. So relax. :)

                return buildPermissionFilterQuery(avoidJoin, ALL_GROUPS);
            }

            // ----------------------------------------------------
            // User is logged in AND onlyDatatRelatedToMe == true
            // Yes, give back everything -> the settings will be in
            // the filterqueries given to search
            // ----------------------------------------------------
            if (onlyDatatRelatedToMe == true) {
                if (systemConfig.myDataDoesNotUsePermissionDocs()) {
                    logger.fine("old 4.2 behavior: MyData is not using Solr permission docs");
                    return buildPermissionFilterQuery(avoidJoin, ALL_GROUPS);
                } else {
                    // fall-through
                    logger.fine("new post-4.2 behavior: MyData is using Solr permission docs");
                }
            }
            // ----------------------------------------------------
            // Work with Authenticated User who is not a Superuser
            // ----------------------------------------------------
            groupList.add(IndexServiceBean.getGroupPerUserPrefix() + au.getId());
        }
        
        // In addition to the user referenced directly, we will also
        // add joins on all the non-public groups that may exist for the
        // user:

        // Authenticated users, *and the GuestUser*, may be part of one or more groups; such
        // as IP Groups.
        groups = groupService.collectAncestors(groupService.groupsFor(dataverseRequest));

        for (Group group : groups) {
            String groupAlias = group.getAlias();
            if (groupAlias != null && !groupAlias.isEmpty() && (!avoidJoin || !groupAlias.startsWith("builtIn"))) {
                groupList.add(IndexServiceBean.getGroupPrefix() + groupAlias);
            }
        }

        if (!avoidJoin) {
            // Add the public group
            groupList.add(0, IndexServiceBean.getPublicGroupString());
        } 
        
        String groupString = null;
        //If we have additional groups, format them correctly into a search string, with parens if there is more than one
        if (groupList.size() > 1) {
            groupString = "(" + StringUtils.join(groupList, " OR ") + ")";
        } else if (groupList.size() == 1) {
            groupString = groupList.get(0);
        }
        logger.fine("Groups: " + groupString);
        String permissionQuery = buildPermissionFilterQuery(avoidJoin, groupString);
        logger.fine("Permission Query: " + permissionQuery);
        return permissionQuery;
    }

    private String buildPermissionFilterQuery(boolean avoidJoin, String permissionFilterGroups) {
        String query = (avoidJoin&& !isAllGroups(permissionFilterGroups)) ? SearchFields.PUBLIC_OBJECT + ":" + true : "";
        if (permissionFilterGroups != null && !isAllGroups(permissionFilterGroups)) {
            if (!query.isEmpty()) {
                query = "(" + query + " OR " + "{!join from=" + SearchFields.DEFINITION_POINT + " to=id v=$q1})&q1=" + SearchFields.DISCOVERABLE_BY + ":" + permissionFilterGroups;
            } else {
                query = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id v=$q1}&q1=" + SearchFields.DISCOVERABLE_BY + ":" + permissionFilterGroups;
            }
        }
        return query;
    }
    
    private boolean isAllGroups(String groups) {
        return (groups!=null &&groups.equals(ALL_GROUPS));
    }
}

