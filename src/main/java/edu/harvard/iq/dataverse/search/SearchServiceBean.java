package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.Stateless;
import javax.ejb.TransactionRolledbackLocalException;
import javax.inject.Named;
import javax.persistence.NoResultException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

@Stateless
@Named
public class SearchServiceBean {

    private static final Logger logger = Logger.getLogger(SearchServiceBean.class.getCanonicalName());

    /**
     * We're trying to make the SearchServiceBean lean, mean, and fast, with as
     * few injections of EJBs as possible.
     */
    /**
     * @todo Can we do without the DatasetFieldServiceBean?
     */
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    GroupServiceBean groupService;
    @EJB
    SystemConfig systemConfig;

    public static final JsfHelper JH = new JsfHelper();
    private SolrServer solrServer;
    
    @PostConstruct
    public void init(){
        solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
    }
    
    @PreDestroy
    public void close(){
        if(solrServer != null){
            solrServer.shutdown();
            solrServer = null;
        }
    }

    /**
     * Import note: "onlyDatatRelatedToMe" relies on filterQueries for providing
     * access to Private Data for the correct user
     *
     * In other words "onlyDatatRelatedToMe", negates other filter Queries
     * related to permissions
     *
     *
     * @param dataverseRequest
     * @param dataverse
     * @param query
     * @param filterQueries
     * @param sortField
     * @param sortOrder
     * @param paginationStart
     * @param onlyDatatRelatedToMe
     * @param numResultsPerPage
     * @return
     * @throws SearchException
     */
    public SolrQueryResponse search(DataverseRequest dataverseRequest, Dataverse dataverse, String query, List<String> filterQueries, String sortField, String sortOrder, int paginationStart, boolean onlyDatatRelatedToMe, int numResultsPerPage) throws SearchException {
        return search(dataverseRequest, dataverse, query, filterQueries, sortField, sortOrder, paginationStart, onlyDatatRelatedToMe, numResultsPerPage, true);
    }
    
    /**
     * Import note: "onlyDatatRelatedToMe" relies on filterQueries for providing
     * access to Private Data for the correct user
     *
     * In other words "onlyDatatRelatedToMe", negates other filter Queries
     * related to permissions
     *
     *
     * @param user
     * @param dataverse
     * @param query
     * @param filterQueries
     * @param sortField
     * @param sortOrder
     * @param paginationStart
     * @param onlyDatatRelatedToMe
     * @param numResultsPerPage
     * @param retrieveEntities - look up dvobject entities with .find() (potentially expensive!) 
     * @return
     * @throws SearchException
     */
    public SolrQueryResponse search(DataverseRequest dataverseRequest, Dataverse dataverse, String query, List<String> filterQueries, String sortField, String sortOrder, int paginationStart, boolean onlyDatatRelatedToMe, int numResultsPerPage, boolean retrieveEntities) throws SearchException {

        if (paginationStart < 0) {
            throw new IllegalArgumentException("paginationStart must be 0 or greater");
        }
        if (numResultsPerPage < 1) {
            throw new IllegalArgumentException("numResultsPerPage must be 1 or greater");
        }

        SolrQuery solrQuery = new SolrQuery();
        query = SearchUtil.sanitizeQuery(query);
        solrQuery.setQuery(query);
//        SortClause foo = new SortClause("name", SolrQuery.ORDER.desc);
//        if (query.equals("*") || query.equals("*:*")) {
//            solrQuery.setSort(new SortClause(SearchFields.NAME_SORT, SolrQuery.ORDER.asc));
        solrQuery.setSort(new SortClause(sortField, sortOrder));
//        } else {
//            solrQuery.setSort(sortClause);
//        }
//        solrQuery.setSort(sortClause);
        solrQuery.setHighlight(true).setHighlightSnippets(1);
        Integer fragSize = systemConfig.getSearchHighlightFragmentSize();
        if (fragSize != null) {
            solrQuery.setHighlightFragsize(fragSize);
        }
        solrQuery.setHighlightSimplePre("<span class=\"search-term-match\">");
        solrQuery.setHighlightSimplePost("</span>");
        Map<String, String> solrFieldsToHightlightOnMap = new HashMap<>();
        solrFieldsToHightlightOnMap.put(SearchFields.NAME, "Name");
        solrFieldsToHightlightOnMap.put(SearchFields.AFFILIATION, "Affiliation");
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_FRIENDLY, "File Type");
        solrFieldsToHightlightOnMap.put(SearchFields.DESCRIPTION, "Description");
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_NAME, "Variable Name");
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_LABEL, "Variable Label");
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_SEARCHABLE, "File Type");
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PUBLICATION_DATE, "Publication Date");
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PERSISTENT_ID, localize("advanced.search.datasets.persistentId"));
        /**
         * @todo Dataverse subject and affiliation should be highlighted but
         * this is commented out right now because the "friendly" names are not
         * being shown on the dataverse cards. See also
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
        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
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
        solrQuery.setParam("fl", "*,score");
        solrQuery.setParam("qt", "/spell");
        solrQuery.setParam("facet", "true");
        /**
         * @todo: do we need facet.query?
         */
        solrQuery.setParam("facet.query", "*");
        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }

        // -----------------------------------
        // PERMISSION FILTER QUERY
        // -----------------------------------
        String permissionFilterQuery = this.getPermissionFilterQuery(dataverseRequest, solrQuery, dataverse, onlyDatatRelatedToMe);
        if (permissionFilterQuery != null) {
            solrQuery.addFilterQuery(permissionFilterQuery);
        }

        // -----------------------------------
        // Facets to Retrieve
        // -----------------------------------
//        solrQuery.addFacetField(SearchFields.HOST_DATAVERSE);
//        solrQuery.addFacetField(SearchFields.AUTHOR_STRING);
        solrQuery.addFacetField(SearchFields.DATAVERSE_CATEGORY);
        solrQuery.addFacetField(SearchFields.METADATA_SOURCE);
//        solrQuery.addFacetField(SearchFields.AFFILIATION);
        solrQuery.addFacetField(SearchFields.PUBLICATION_DATE);
//        solrQuery.addFacetField(SearchFields.CATEGORY);
//        solrQuery.addFacetField(SearchFields.FILE_TYPE_MIME);
//        solrQuery.addFacetField(SearchFields.DISTRIBUTOR);
//        solrQuery.addFacetField(SearchFields.KEYWORD);
        /**
         * @todo when a new method on datasetFieldService is available
         * (retrieveFacetsByDataverse?) only show the facets that the dataverse
         * in question wants to show (and in the right order):
         * https://redmine.hmdc.harvard.edu/issues/3490
         *
         * also, findAll only returns advancedSearchField = true... we should
         * probably introduce the "isFacetable" boolean rather than caring about
         * if advancedSearchField is true or false
         *
         */
        if (dataverse != null) {
            for (DataverseFacet dataverseFacet : dataverse.getDataverseFacets()) {
                DatasetFieldType datasetField = dataverseFacet.getDatasetFieldType();
                solrQuery.addFacetField(datasetField.getSolrField().getNameFacetable());
            }
        }
        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        /**
         * @todo: hide the extra line this shows in the GUI... at least it's
         * last...
         */
        solrQuery.addFacetField(SearchFields.TYPE);
        solrQuery.addFacetField(SearchFields.FILE_TAG);
        solrQuery.addFacetField(SearchFields.ACCESS);
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
        QueryResponse queryResponse;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (RemoteSolrException ex) {
            String messageFromSolr = ex.getLocalizedMessage();
            String error = "Search Syntax Error: ";
            String stringToHide = "org.apache.solr.search.SyntaxError: ";
            if (messageFromSolr.startsWith(stringToHide)) {
                // hide "org.apache.solr..."
                error += messageFromSolr.substring(stringToHide.length());
            } else {
                error += messageFromSolr;
            }
            logger.info(error);
            SolrQueryResponse exceptionSolrQueryResponse = new SolrQueryResponse(solrQuery);
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
        } catch (SolrServerException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }

        SolrDocumentList docs = queryResponse.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
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
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String type = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            float score = (Float) solrDocument.getFieldValue(SearchFields.RELEVANCE);
            logger.fine("score for " + id + ": " + score);
            String identifier = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER);
            String citation = (String) solrDocument.getFieldValue(SearchFields.DATASET_CITATION);
            String citationPlainHtml = (String) solrDocument.getFieldValue(SearchFields.DATASET_CITATION_HTML);
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
            String dateToDisplayOnCard = (String) solrDocument.getFirstValue(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT);
            String dvTree = (String) solrDocument.getFirstValue(SearchFields.SUBTREE);
            List<String> matchedFields = new ArrayList<>();
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
            SolrSearchResult solrSearchResult = new SolrSearchResult(query, name);
            /**
             * @todo put all this in the constructor?
             */
            List<String> states = (ArrayList<String>) solrDocument.getFieldValue(SearchFields.PUBLICATION_STATUS);
            if (states != null) {
                // set list of all statuses
                // this method also sets booleans for individual statuses
                solrSearchResult.setPublicationStatuses(states);
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
            solrSearchResult.setDateToDisplayOnCard(dateToDisplayOnCard);
            solrSearchResult.setMatchedFields(matchedFields);
            solrSearchResult.setHighlightsAsList(highlights);
            solrSearchResult.setHighlightsMap(highlightsMap);
            solrSearchResult.setHighlightsAsMap(highlightsMap3);
            Map<String, String> parent = new HashMap<>();
            String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
            solrSearchResult.setDescriptionNoSnippet(description);
            solrSearchResult.setDeaccessionReason(deaccessionReason);
            solrSearchResult.setDvTree(dvTree);
            
            String originSource = (String) solrDocument.getFieldValue(SearchFields.METADATA_SOURCE);
            if (IndexServiceBean.HARVESTED.equals(originSource)) {
                solrSearchResult.setHarvested(true);
            }
            
            /**
             * @todo start using SearchConstants class here
             */
            if (type.equals("dataverses")) {
                solrSearchResult.setName(name);
                solrSearchResult.setHtmlUrl(baseUrl + "/dataverse/" + identifier);
                solrSearchResult.setImageUrl(baseUrl + "/api/access/dvCardImage/" + entityid);
                /**
                 * @todo Expose this API URL after "dvs" is changed to
                 * "dataverses". Also, is an API token required for published
                 * dataverses? Michael: url changed.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/dataverses/" + entityid);
            } else if (type.equals("datasets")) {
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?globalId=" + identifier);
                solrSearchResult.setApiUrl(baseUrl + "/api/datasets/" + entityid);
                solrSearchResult.setImageUrl(baseUrl + "/api/access/dsCardImage/" + datasetVersionId);
                /**
                 * @todo Could use getFieldValues (plural) here.
                 */
                ArrayList<String> datasetDescriptions = (ArrayList<String>) solrDocument.getFieldValue(SearchFields.DATASET_DESCRIPTION);
                if (datasetDescriptions != null) {
                    String firstDatasetDescription = datasetDescriptions.get(0);
                    if (firstDatasetDescription != null) {
                        solrSearchResult.setDescriptionNoSnippet(firstDatasetDescription);
                    }
                }
                solrSearchResult.setDatasetVersionId(datasetVersionId);

                solrSearchResult.setCitation(citation);
                solrSearchResult.setCitationHtml(citationPlainHtml);
                if (title != null) {
//                    solrSearchResult.setTitle((String) titles.get(0));
                    solrSearchResult.setTitle((String) title);
                } else {
                    logger.fine("No title indexed. Setting to empty string to prevent NPE. Dataset id " + entityid + " and version id " + datasetVersionId);
                    solrSearchResult.setTitle("");
                }
                List<String> authors = (ArrayList) solrDocument.getFieldValues(DatasetFieldConstant.authorName);
                if (authors != null) {
                    solrSearchResult.setDatasetAuthors(authors);
                }
            } else if (type.equals("files")) {
                String parentGlobalId = null;
                Object parentGlobalIdObject = solrDocument.getFieldValue(SearchFields.PARENT_IDENTIFIER);
                if (parentGlobalIdObject != null) {
                    parentGlobalId = (String) parentGlobalIdObject;
                    parent.put(SolrSearchResult.PARENT_IDENTIFIER, parentGlobalId);
                }
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?persistentId=" + parentGlobalId);
                solrSearchResult.setDownloadUrl(baseUrl + "/api/access/datafile/" + entityid);
                /**
                 * @todo We are not yet setting the API URL for files because
                 * not all files have metadata. Only subsettable files (those
                 * with a datatable) seem to have metadata. Furthermore, the
                 * response is in XML whereas the rest of the Search API returns
                 * JSON.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/meta/datafile/" + entityid);
                solrSearchResult.setImageUrl(baseUrl + "/api/access/fileCardImage/" + entityid);
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
                    solrSearchResult.setFileChecksumType((DataFile.ChecksumType) DataFile.ChecksumType.fromString((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_TYPE)));
                } catch (IllegalArgumentException ex) {
                    logger.info("Exception setting setFileChecksumType: " + ex);
                }
                solrSearchResult.setFileChecksumValue((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_VALUE));
                solrSearchResult.setUnf((String) solrDocument.getFieldValue(SearchFields.UNF));
                solrSearchResult.setDatasetVersionId(datasetVersionId);
                List<String> fileCategories = (ArrayList) solrDocument.getFieldValues(SearchFields.FILE_TAG);
                if (fileCategories != null) {
                    solrSearchResult.setFileCategories(fileCategories);
                }
                List<String> tabularDataTags = (ArrayList) solrDocument.getFieldValues(SearchFields.TABDATA_TAG);
                if (tabularDataTags != null) {
                    Collections.sort(tabularDataTags);
                    solrSearchResult.setTabularDataTags(tabularDataTags);
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

        List<FacetCategory> facetCategoryList = new ArrayList<FacetCategory>();
        List<FacetCategory> typeFacetCategories = new ArrayList<>();
        boolean hidePublicationStatusFacet = true;
        boolean draftsAvailable = false;
        boolean unpublishedAvailable = false;
        boolean deaccessionedAvailable = false;
        boolean hideMetadataSourceFacet = true;
        for (FacetField facetField : queryResponse.getFacetFields()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
            int numMetadataSources = 0;
            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                /**
                 * @todo we do want to show the count for each facet
                 */
//                logger.info("field: " + facetField.getName() + " " + facetFieldCount.getName() + " (" + facetFieldCount.getCount() + ")");
                if (facetFieldCount.getCount() > 0) {
                    FacetLabel facetLabel = new FacetLabel(facetFieldCount.getName(), facetFieldCount.getCount());
                    // quote field facets
                    facetLabel.setFilterQuery(facetField.getName() + ":\"" + facetFieldCount.getName() + "\"");
                    facetLabelList.add(facetLabel);
                    if (facetField.getName().equals(SearchFields.PUBLICATION_STATUS)) {
                        if (facetLabel.getName().equals(IndexServiceBean.getUNPUBLISHED_STRING())) {
                            unpublishedAvailable = true;
                        } else if (facetLabel.getName().equals(IndexServiceBean.getDRAFT_STRING())) {
                            draftsAvailable = true;
                        } else if (facetLabel.getName().equals(IndexServiceBean.getDEACCESSIONED_STRING())) {
                            deaccessionedAvailable = true;
                        }
                    }
                    if (facetField.getName().equals(SearchFields.METADATA_SOURCE)) {
                        numMetadataSources++;
                    }
                }
            }
            if (numMetadataSources > 1) {
                hideMetadataSourceFacet = false;
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
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(SearchServiceBean.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(SearchServiceBean.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (staticSearchField != null && facetField.getName().equals(staticSearchField)) {
                    String[] parts = name.split("_");
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String part : parts) {
                        stringBuilder.append(getCapitalizedName(part.toLowerCase()) + " ");
                    }
                    String friendlyNameWithTrailingSpace = stringBuilder.toString();
                    String friendlyName = friendlyNameWithTrailingSpace.replaceAll(" $", "");
                    facetCategory.setFriendlyName(friendlyName);
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
                } else {
                    facetCategoryList.add(facetCategory);
                }
            }
        }

        // for now the only range facet is citation year
        for (RangeFacet rangeFacet : queryResponse.getFacetRanges()) {
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
                    FacetLabel facetLabel = new FacetLabel(start + "-" + end, new Long(rangeFacetCount.getCount()));
                    // special [12 TO 34] syntax for range facets
                    facetLabel.setFilterQuery(rangeFacet.getName() + ":" + "[" + start + " TO " + end + "]");
                    facetLabelList.add(facetLabel);
                }
            }
            facetCategory.setName(rangeFacet.getName());
            facetCategory.setFacetLabel(facetLabelList);
            // reverse to show the newest citation year range at the top
            List<FacetLabel> facetLabelListReversed = new ArrayList<>();
            ListIterator li = facetLabelList.listIterator(facetLabelList.size());
            while (li.hasPrevious()) {
                facetLabelListReversed.add((FacetLabel) li.previous());
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

    private static String localize(String bundleKey) {
        try {
            String value = JH.localize(bundleKey);
            return value;
        } catch (Exception e) {
            // can throw MissingResourceException
            return "Match";
        }
    }

    public String getCapitalizedName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Moved this logic out of the "search" function
     *
     * @return
     */
    private String getPermissionFilterQuery(DataverseRequest dataverseRequest, SolrQuery solrQuery, Dataverse dataverse, boolean onlyDatatRelatedToMe) {

        User user = dataverseRequest.getUser();
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }
        if (solrQuery == null) {
            throw new NullPointerException("solrQuery cannot be null");
        }
        /**
         * @todo For people who are not logged in, should we show stuff indexed
         * with "AllUsers" group or not? If so, uncomment the allUsersString
         * stuff below.
         */
//        String allUsersString = IndexServiceBean.getGroupPrefix() + AllUsers.get().getAlias();
//        String publicOnly = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}" + SearchFields.DISCOVERABLE_BY + ":(" + IndexServiceBean.getPublicGroupString() + " OR " + allUsersString + ")";
        String publicOnly = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}" + SearchFields.DISCOVERABLE_BY + ":(" + IndexServiceBean.getPublicGroupString() + ")";
//        String publicOnly = "{!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getPublicGroupString();
        // initialize to public only to be safe
        String dangerZoneNoSolrJoin = null;

        if (user instanceof PrivateUrlUser) {
            user = GuestUser.get();
        }

        // ----------------------------------------------------
        // (1) Is this a GuestUser?  
        // Yes, see if GuestUser is part of any groups such as IP Groups.
        // ----------------------------------------------------
        if (user instanceof GuestUser) {
            String groupsFromProviders = "";
            Set<Group> groups = groupService.collectAncestors(groupService.groupsFor(dataverseRequest));
            StringBuilder sb = new StringBuilder();
            for (Group group : groups) {
                logger.fine("found group " + group.getIdentifier() + " with alias " + group.getAlias());
                String groupAlias = group.getAlias();
                if (groupAlias != null && !groupAlias.isEmpty()) {
                    sb.append(" OR ");
                    // i.e. group_builtIn/all-users, ip/ipGroup3
                    sb.append(IndexServiceBean.getGroupPrefix()).append(groupAlias);
                }
            }
            groupsFromProviders = sb.toString();
            logger.fine("groupsFromProviders:" + groupsFromProviders);
            String guestWithGroups = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}" + SearchFields.DISCOVERABLE_BY + ":(" + IndexServiceBean.getPublicGroupString() + groupsFromProviders + ")";
            logger.fine(guestWithGroups);
            return guestWithGroups;
        }

        // ----------------------------------------------------
        // (2) Retrieve Authenticated User
        // ----------------------------------------------------
        if (!(user instanceof AuthenticatedUser)) {
            logger.severe("Should never reach here. A User must be an AuthenticatedUser or a Guest");
            throw new IllegalStateException("A User must be an AuthenticatedUser or a Guest");
        }

        AuthenticatedUser au = (AuthenticatedUser) user;

        // Logged in user, has publication status facet
        //
        solrQuery.addFacetField(SearchFields.PUBLICATION_STATUS);

        // ----------------------------------------------------
        // (3) Is this a Super User?  
        //      Yes, give back everything
        // ----------------------------------------------------        
        if (au.isSuperuser()) {
            // dangerous because this user will be able to see
            // EVERYTHING in Solr with no regard to permissions!

            return dangerZoneNoSolrJoin;
        }

        // ----------------------------------------------------
        // (4) User is logged in AND onlyDatatRelatedToMe == true
        // Yes, give back everything -> the settings will be in
        //          the filterqueries given to search
        // ----------------------------------------------------    
        if (onlyDatatRelatedToMe == true) {
            if (systemConfig.myDataDoesNotUsePermissionDocs()) {
                logger.fine("old 4.2 behavior: MyData is not using Solr permission docs");
                return dangerZoneNoSolrJoin;
            } else {
                logger.fine("new post-4.2 behavior: MyData is using Solr permission docs");
            }
        }

        // ----------------------------------------------------
        // (5) Work with Authenticated User who is not a Superuser
        // ----------------------------------------------------
        /**
         * @todo all this code needs cleanup and clarification.
         */
        /**
         * Every AuthenticatedUser is part of a "User Private Group" (UGP), a
         * concept we borrow from RHEL:
         * https://access.redhat.com/site/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Deployment_Guide/ch-Managing_Users_and_Groups.html#s2-users-groups-private-groups
         */
        /**
         * @todo rename this from publicPlusUserPrivateGroup. Confusing
         */
        // safe default: public only
        String publicPlusUserPrivateGroup = publicOnly;
//                    + (onlyDatatRelatedToMe ? "" : (publicOnly + " OR "))
//                    + "{!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getGroupPerUserPrefix() + au.getId() + ")";

//            /**
//             * @todo add onlyDatatRelatedToMe option into the experimental JOIN
//             * before enabling it.
//             */
        /**
         * From a search perspective, we don't care about if the group was
         * created within one dataverse or another. We just want a list of *all*
         * the groups the user is part of. We are greedy. We want all BuiltIn
         * Groups, Shibboleth Groups, IP Groups, "system" groups, everything.
         *
         * A JOIN on "permission documents" will determine if the user can find
         * a given "content document" (dataset version, etc) in Solr.
         */
        String groupsFromProviders = "";
        Set<Group> groups = groupService.collectAncestors(groupService.groupsFor(dataverseRequest));
        StringBuilder sb = new StringBuilder();
        for (Group group : groups) {
            logger.fine("found group " + group.getIdentifier() + " with alias " + group.getAlias());
            String groupAlias = group.getAlias();
            if (groupAlias != null && !groupAlias.isEmpty()) {
                sb.append(" OR ");
                // i.e. group_builtIn/all-users, group_builtIn/authenticated-users, group_1-explictGroup1, group_shib/2
                sb.append(IndexServiceBean.getGroupPrefix() + groupAlias);
            }
        }
        groupsFromProviders = sb.toString();

        logger.fine(groupsFromProviders);
        if (true) {
            /**
             * @todo get rid of "experimental" in name
             */
            String experimentalJoin = "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}" + SearchFields.DISCOVERABLE_BY + ":(" + IndexServiceBean.getPublicGroupString() + " OR " + IndexServiceBean.getGroupPerUserPrefix() + au.getId() + groupsFromProviders + ")";
            publicPlusUserPrivateGroup = experimentalJoin;
        }

        //permissionFilterQuery = publicPlusUserPrivateGroup;
        logger.fine(publicPlusUserPrivateGroup);

        return publicPlusUserPrivateGroup;

    }

}
