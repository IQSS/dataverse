package edu.harvard.iq.dataverse.search;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.FriendlyFileTypeUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FacetLabel;
import edu.harvard.iq.dataverse.search.response.Highlight;
import edu.harvard.iq.dataverse.search.response.PublicationStatusCounts;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.Stateless;
import javax.ejb.TransactionRolledbackLocalException;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Stateless
public class SearchServiceBean {

    private static final Logger logger = Logger.getLogger(SearchServiceBean.class.getCanonicalName());

    private static final String FACETBUNDLE_MASK_GROUP_AND_VALUE = "facets.search.fieldtype.%s.%s.label";
    private static final String FACETBUNDLE_MASK_VALUE = "facets.search.fieldtype.%s.label";
    private static final String FACETBUNDLE_MASK_DVCATEGORY_VALUE = "dataverse.type.selectTab.%s";

    public enum SortOrder {

        asc, desc;


        public static Optional<SortOrder> fromString(String sortOrderString) {
            return Try.of(() -> SortOrder.valueOf(sortOrderString))
                    .toJavaOptional();

        }

        public static List<String> allowedOrderStrings() {
            return Lists.newArrayList(SortOrder.values()).stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
    }

    /*
      We're trying to make the SearchServiceBean lean, mean, and fast, with as
      few injections of EJBs as possible.
     */
    /**
     * @todo Can we do without the DatasetFieldServiceBean?
     */
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @Inject
    SettingsServiceBean settingsService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    private PermissionFilterQueryBuilder permissionQueryBuilder;
    @Inject
    private SolrClient solrServer;
    @Inject
    private SolrQuerySanitizer querySanitizer;

    // -------------------- LOGIC --------------------

    public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query, SearchForTypes typesToSearch, List<String> filterQueries, String sortField, SortOrder sortOrder, int paginationStart, int numResultsPerPage) throws SearchException {
        return search(dataverseRequest, dataverses, query, typesToSearch, filterQueries, sortField, sortOrder, paginationStart, numResultsPerPage, true, false);
    }

    /**
     * @param retrieveEntities look up dvobject entities with .find()
     *                         (potentially expensive!)
     * @param countsOnly after executing solr query only found object counts
     *                   (ie. datasets, dataverses & files) would be filled in
     *                   returned object, so it is unsuitable for other uses.
     */
    public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query, SearchForTypes typesToSearch,
                                    List<String> filterQueries, String sortField, SortOrder sortOrder, int paginationStart,
                                    int numResultsPerPage, boolean retrieveEntities, boolean countsOnly)
            throws SearchException {
        if (paginationStart < 0) {
            throw new IllegalArgumentException("paginationStart must be 0 or greater");
        }
        if (numResultsPerPage < 1) {
            throw new IllegalArgumentException("numResultsPerPage must be 1 or greater");
        }

        SolrQuery solrQuery = new SolrQuery();

        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        Map<String, List<DatasetFieldType>> fieldIndex = datasetFields.stream()
                .collect(Collectors.groupingBy(DatasetFieldType::getName, Collectors.toList()));

        query = querySanitizer.sanitizeQuery(query, datasetFields);
        solrQuery.setQuery(query);

        solrQuery.setSort(new SortClause(sortField, sortOrder == SortOrder.asc ? ORDER.asc : ORDER.desc));
        solrQuery.setHighlight(true).setHighlightSnippets(1);
        Integer fragSize = settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.SearchHighlightFragmentSize);
        if (fragSize != null) {
            solrQuery.setHighlightFragsize(fragSize);
        }
        solrQuery.setHighlightSimplePre("<span class=\"search-term-match\">");
        solrQuery.setHighlightSimplePost("</span>");
        Map<String, String> solrFieldsToHightlightOnMap = new HashMap<>();
        solrFieldsToHightlightOnMap.put(SearchFields.NAME, BundleUtil.getStringFromBundle("name"));
        solrFieldsToHightlightOnMap.put(SearchFields.AFFILIATION, BundleUtil.getStringFromBundle("affiliation"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_FRIENDLY, BundleUtil.getStringFromBundle("advanced.search.files.fileType"));
        solrFieldsToHightlightOnMap.put(SearchFields.DESCRIPTION, BundleUtil.getStringFromBundle("description"));
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_NAME, BundleUtil.getStringFromBundle("advanced.search.files.variableName"));
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_LABEL, BundleUtil.getStringFromBundle("advanced.search.files.variableLabel"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_SEARCHABLE, BundleUtil.getStringFromBundle("advanced.search.files.fileType"));
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PUBLICATION_DATE, BundleUtil.getStringFromBundle("dataset.metadata.publicationYear"));
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.datasets.persistentId"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.files.persistentId"));
        /*
          @todo Dataverse subject and affiliation should be highlighted but
         * this is commented out right now because the "friendly" names are not
         * being shown on the dataverse cards. See also
         * https://github.com/IQSS/dataverse/issues/1431
         */
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_SUBJECT, "Subject");
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_AFFILIATION, "Affiliation");
        /*
          @todo: show highlight on file card?
         * https://redmine.hmdc.harvard.edu/issues/3848
         */
        solrFieldsToHightlightOnMap.put(SearchFields.FILENAME_WITHOUT_EXTENSION, BundleUtil.getStringFromBundle("facets.search.fieldtype.fileNameWithoutExtension.label"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_EXTENSION, BundleUtil.getStringFromBundle("advanced.search.files.fileExtension"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TAG_SEARCHABLE, BundleUtil.getStringFromBundle("facets.search.fieldtype.fileTag.label"));
        for (DatasetFieldType datasetFieldType : datasetFields) {

            SolrField dsfSolrField = SolrField.of(datasetFieldType.getName(), datasetFieldType.getFieldType(),
                    datasetFieldType.isThisOrParentAllowsMultipleValues(), datasetFieldType.isFacetable());

            String solrField = dsfSolrField.getNameSearchable();
            String displayName = datasetFieldType.getDisplayName();
            solrFieldsToHightlightOnMap.put(solrField, displayName);
        }

        solrQuery = addHighlightFields(solrQuery, solrFieldsToHightlightOnMap);

        solrQuery.setHighlightRequireFieldMatch(true);
        solrQuery.setParam("fl", "*,score");
        solrQuery.setParam("qt", "/select");
        solrQuery.setParam("facet", "true");
        //  @todo: do we need facet.query?
        solrQuery.setParam("facet.query", "*");


        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }

        addDvObjectTypeFilterQuery(solrQuery, typesToSearch);

        String permissionFilterQuery = permissionQueryBuilder.buildPermissionFilterQuery(dataverseRequest);
        if (!permissionFilterQuery.isEmpty()) {
            solrQuery.addFilterQuery(permissionFilterQuery);
        }

        // -----------------------------------
        // Facets to Retrieve
        // -----------------------------------
        solrQuery.addFacetField(SearchFields.DATAVERSE_CATEGORY);
        solrQuery.addFacetField(SearchFields.METADATA_SOURCE);
        solrQuery.addFacetField(SearchFields.PUBLICATION_YEAR);
        /*
          @todo when a new method on datasetFieldService is available
         * (retrieveFacetsByDataverse?) only show the facets that the dataverse
         * in question wants to show (and in the right order):
         * https://redmine.hmdc.harvard.edu/issues/3490
         *
         * also, findAll only returns advancedSearchField = true... we should
         * probably introduce the "isFacetable" boolean rather than caring about
         * if advancedSearchField is true or false

         */

        if (dataverseRequest.getUser().isAuthenticated()) {
            solrQuery.addFacetField(SearchFields.PUBLICATION_STATUS);
        }

        if (dataverses != null) {
            for (Dataverse dataverse : dataverses) {

                for (DataverseFacet dataverseFacet : dataverse.getDataverseFacets()) {
                    DatasetFieldType datasetField = dataverseFacet.getDatasetFieldType();

                    SolrField dsfSolrField = SolrField.of(datasetField.getName(), datasetField.getFieldType(),
                            datasetField.isThisOrParentAllowsMultipleValues(), datasetField.isFacetable());
                    solrQuery.addFacetField(dsfSolrField.getNameFacetable());
                }
            }
        }

        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        // @todo: hide the extra line this shows in the GUI... at least it's
        solrQuery.addFacetField(SearchFields.TYPE);
        solrQuery.addFacetField(SearchFields.FILE_TAG);
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall)) {
            solrQuery.addFacetField(SearchFields.ACCESS);
        }
        // @todo: do sanity checking... throw error if negative
        solrQuery.setStart(paginationStart);
        // @todo: decide if year CITATION_YEAR is good enough or if we should support CITATION_DATE

//        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.UK);
//        calendar.set(2010, 1, 1);
//        Date start = calendar.getTime();
//        calendar.set(2013, 1, 1);
//        Date end = calendar.getTime();
//        solrQuery.addDateRangeFacet(SearchFields.CITATION_DATE, start, end, "+1MONTH");
        //  @todo make this configurable
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        solrQuery.setRows(numResultsPerPage);
        logger.fine("Solr query:" + solrQuery);

        // -----------------------------------
        // Make the solr query
        // -----------------------------------
        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (RemoteSolrException | SolrServerException | IOException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }
        SolrDocumentList docs = queryResponse.getResults();
        List<SolrSearchResult> solrSearchResults = new ArrayList<>();

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse(solrQuery);
        solrQueryResponse.setDvObjectCounts(convertFacetToDvObjectCounts(queryResponse.getFacetField(SearchFields.TYPE)));
        if (countsOnly) {
            return solrQueryResponse;
        }
        /*
          @todo refactor SearchFields to a hashmap (or something? put in
         * database? internationalize?) to avoid the crazy reflection and string
         * manipulation below
         */
        Object searchFieldsObject = new SearchFields();
        Field[] staticSearchFields = searchFieldsObject.getClass().getDeclaredFields();
        String titleSolrField = null;
        try {
            DatasetFieldType titleDatasetField = datasetFieldService.findByName(DatasetFieldConstant.title);
            titleSolrField = SolrField.of(titleDatasetField.getName(), titleDatasetField.getFieldType(),
                    titleDatasetField.isThisOrParentAllowsMultipleValues(), titleDatasetField.isFacetable())
                    .getNameSearchable();
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
        long result_t = System.currentTimeMillis();
        for (SolrDocument solrDocument : docs) {
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String solrType = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            SearchObjectType type = SearchObjectType.fromSolrValue(solrType);

            float score = (Float) solrDocument.getFieldValue(SearchFields.RELEVANCE);
            logger.fine("score for " + id + ": " + score);
            String identifier = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER);
            String citation = getLocalizedValueWithFallback(solrDocument, SearchFields.DATASET_CITATION);
            String citationPlainHtml = getLocalizedValueWithFallback(solrDocument, SearchFields.DATASET_CITATION_HTML);
            String persistentUrl = (String) solrDocument.getFieldValue(SearchFields.PERSISTENT_URL);
            String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            String nameSort = (String) solrDocument.getFieldValue(SearchFields.NAME_SORT);
            ArrayList titles = (ArrayList) solrDocument.getFieldValues(titleSolrField);
            Long datasetVersionId = (Long) solrDocument.getFieldValue(SearchFields.DATASET_VERSION_ID);
            String deaccessionReason = (String) solrDocument.getFieldValue(SearchFields.DATASET_DEACCESSION_REASON);
            String fileContentType = (String) solrDocument.getFieldValue(SearchFields.FILE_CONTENT_TYPE);
            Date release_or_create_date = (Date) solrDocument.getFieldValue(SearchFields.RELEASE_OR_CREATE_DATE);
            String dateToDisplayOnCard = (String) solrDocument.getFirstValue(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT);
            String dvTree = (String) solrDocument.getFirstValue(SearchFields.SUBTREE);
            String identifierOfDataverse = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER_OF_DATAVERSE);
            String nameOfDataverse = (String) solrDocument.getFieldValue(SearchFields.DATAVERSE_NAME);

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
                        /*
                          @todo only SolrField.SolrType.STRING? that's not
                         * right... knit the SolrField object more into the
                         * highlighting stuff
                         */
                        SolrField solrField = new SolrField(field, SolrField.SolrType.STRING, true, true, false);
                        Highlight highlight = new Highlight(solrField, highlightSnippets, displayName);
                        highlights.add(highlight);
                        highlightsMap.put(solrField, highlight);
                        highlightsMap2.put(solrField, highlightSnippets);
                        highlightsMap3.put(field, highlight);
                    }
                }

            }
            SolrSearchResult solrSearchResult = new SolrSearchResult(query, name);
            // @todo put all this in the constructor?
            List<String> states = (List<String>) solrDocument.getFieldValue(SearchFields.PUBLICATION_STATUS);
            if (states != null) {
                // set list of all statuses
                // this method also sets booleans for individual statuses
                List<SearchPublicationStatus> publicationStates = states.stream()
                        .map(solrStatus -> SearchPublicationStatus.fromSolrValue(solrStatus))
                        .collect(Collectors.toList());
                solrSearchResult.setPublicationStatuses(publicationStates);
            }
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

            if (type == SearchObjectType.DATAVERSES) {
                solrSearchResult.setName(name);
                solrSearchResult.setHtmlUrl(baseUrl + SystemConfig.DATAVERSE_PATH + identifier);
                // Do not set the ImageUrl, let the search include fragment fill in
                // the thumbnail, similarly to how the dataset and datafile cards
                // are handled.
                //solrSearchResult.setImageUrl(baseUrl + "/api/access/dvCardImage/" + entityid);
                /*
                  @todo Expose this API URL after "dvs" is changed to
                 * "dataverses". Also, is an API token required for published
                 * dataverses? Michael: url changed.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/dataverses/" + entityid);
            } else if (type == SearchObjectType.DATASETS) {
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?globalId=" + identifier);
                solrSearchResult.setApiUrl(baseUrl + "/api/datasets/" + entityid);
                //Image url now set via thumbnail api
                //solrSearchResult.setImageUrl(baseUrl + "/api/access/dsCardImage/" + datasetVersionId);
                // No, we don't want to set the base64 thumbnails here.
                // We want to do it inside SearchIncludeFragment, AND ONLY once the rest of the
                // page has already loaded.
                //DatasetVersion datasetVersion = datasetVersionService.find(datasetVersionId);
                //if (datasetVersion != null){
                //    solrSearchResult.setDatasetThumbnail(datasetVersion.getDataset().getDatasetThumbnail(datasetVersion));
                //}

                // @todo Could use getFieldValues (plural) here.
                List<String> datasetDescriptions = (List) solrDocument.getFieldValues(SearchFields.DATASET_DESCRIPTION);
                if (datasetDescriptions != null) {
                    String firstDatasetDescription = datasetDescriptions.get(0);
                    if (firstDatasetDescription != null) {
                        solrSearchResult.setDescriptionNoSnippet(firstDatasetDescription);
                    }
                }
                solrSearchResult.setDatasetVersionId(datasetVersionId);

                solrSearchResult.setCitation(citation);
                solrSearchResult.setCitationHtml(citationPlainHtml);

                solrSearchResult.setIdentifierOfDataverse(identifierOfDataverse);
                solrSearchResult.setNameOfDataverse(nameOfDataverse);

                if (titles != null) {
                    solrSearchResult.setTitle((String) titles.get(0));
                } else {
                    logger.fine("No title indexed. Setting to empty string to prevent NPE. Dataset id " + entityid + " and version id " + datasetVersionId);
                    solrSearchResult.setTitle("");
                }
                List<String> authors = (List) solrDocument.getFieldValues(DatasetFieldConstant.authorName);
                if (authors != null) {
                    solrSearchResult.setDatasetAuthors(authors);
                }
            } else if (type == SearchObjectType.FILES) {
                String parentGlobalId = null;
                Object parentGlobalIdObject = solrDocument.getFieldValue(SearchFields.PARENT_IDENTIFIER);
                if (parentGlobalIdObject != null) {
                    parentGlobalId = (String) parentGlobalIdObject;
                    parent.put(SolrSearchResult.PARENT_IDENTIFIER, parentGlobalId);
                }
                solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?persistentId=" + parentGlobalId);
                solrSearchResult.setDownloadUrl(baseUrl + "/api/access/datafile/" + entityid);
                /*
                  @todo We are not yet setting the API URL for files because
                 * not all files have metadata. Only subsettable files (those
                 * with a datatable) seem to have metadata. Furthermore, the
                 * response is in XML whereas the rest of the Search API returns
                 * JSON.
                 */
//                solrSearchResult.setApiUrl(baseUrl + "/api/meta/datafile/" + entityid);
                //solrSearchResult.setImageUrl(baseUrl + "/api/access/fileCardImage/" + entityid);
                solrSearchResult.setName(name);
                solrSearchResult.setFiletype(FriendlyFileTypeUtil.getUserFriendlyFileTypeForDisplay(fileContentType));
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
                String filePID = (String) solrDocument.getFieldValue(SearchFields.FILE_PERSISTENT_ID);
                if (null != filePID && !"".equals(filePID)) {
                    solrSearchResult.setFilePersistentId(filePID);
                }

                String fileAccess = (String) solrDocument.getFirstValue(SearchFields.ACCESS);
                solrSearchResult.setFileAccess(fileAccess);
            }
            // @todo store PARENT_ID as a long instead and cast as such
            parent.put("id", (String) solrDocument.getFieldValue(SearchFields.PARENT_ID));
            parent.put("name", (String) solrDocument.getFieldValue(SearchFields.PARENT_NAME));
            parent.put("citation", getLocalizedValueWithFallback(solrDocument, SearchFields.PARENT_CITATION));
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
        boolean hidePublicationStatusFacet = false;
        boolean draftsAvailable = false;
        boolean unpublishedAvailable = false;
        boolean deaccessionedAvailable = false;
        boolean hideMetadataSourceFacet = true;
        for (FacetField facetField : queryResponse.getFacetFields()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
            int numMetadataSources = 0;
            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                // @todo we do want to show the count for each facet
                if (facetFieldCount.getCount() > 0) {
                    FacetLabel facetLabel = new FacetLabel(facetFieldCount.getName(),
                            getLocaleFacetLabelName(facetFieldCount.getName(), facetField.getName(), fieldIndex),
                            facetFieldCount.getCount());
                    // quote field facets
                    facetLabel.setFilterQuery(facetField.getName() + ":\"" + facetFieldCount.getName() + "\"");
                    facetLabelList.add(facetLabel);
                    if (facetField.getName().equals(SearchFields.PUBLICATION_STATUS)) {
                        if (facetLabel.getName().equals(SearchPublicationStatus.UNPUBLISHED.getSolrValue())) {
                            unpublishedAvailable = true;
                        } else if (facetLabel.getName().equals(SearchPublicationStatus.DRAFT.getSolrValue())) {
                            draftsAvailable = true;
                        } else if (facetLabel.getName().equals(SearchPublicationStatus.DEACCESSIONED.getSolrValue())) {
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
            /*
              @todo hmm, we thought we wanted the datasetFields array to go
             * away once we have more granularity than findAll() available per
             * the todo above but we need a way to lookup by Solr field, so
             * we'll build a hashmap
             */
            for (DatasetFieldType datasetField : datasetFields) {
                SolrField dsfSolrField = SolrField.of(datasetField.getName(), datasetField.getFieldType(),
                        datasetField.isThisOrParentAllowsMultipleValues(), datasetField.isFacetable());

                String solrFieldNameForDataset = dsfSolrField.getNameFacetable();
                String friendlyName = datasetField.getDisplayName();
                if (solrFieldNameForDataset != null && facetField.getName().equals(solrFieldNameForDataset)) {
                    if (friendlyName != null && !friendlyName.isEmpty()) {
                        facetCategory.setFriendlyName(friendlyName);
                        // stop examining available dataset fields. we found a match
                        break;
                    }
                }
                datasetfieldFriendlyNamesBySolrField.put(dsfSolrField.getNameFacetable(), friendlyName);
            }
            /*
              @todo get rid of this crazy reflection, per todo above... or
             * should we... let's put into a hash the friendly names of facet
             * categories, indexed by Solr field
             */
            for (Field fieldObject : staticSearchFields) {
                String staticSearchField = null;
                try {
                    staticSearchField = (String) fieldObject.get(searchFieldsObject);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(SearchServiceBean.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (staticSearchField != null && facetField.getName().equals(staticSearchField)) {
                    String friendlyName = getLocaleFacetCategoryName(facetField.getName(), fieldIndex);
                    facetCategory.setFriendlyName(friendlyName);
                    staticSolrFieldFriendlyNamesBySolrField.put(staticSearchField, friendlyName);
                    break;
                }
            }
            facetCategory.setFacetLabel(facetLabelList);
            if (!facetLabelList.isEmpty()) {
                if (facetCategory.getName().equals(SearchFields.TYPE)) {
                    // the "type" facet is special, these are not
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
        for (RangeFacet<String, String> rangeFacet : queryResponse.getFacetRanges()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
            for (Object rfObj : rangeFacet.getCounts()) {
                RangeFacet.Count rangeFacetCount = (RangeFacet.Count) rfObj;
                String valueString = rangeFacetCount.getValue();
                Integer start = Integer.parseInt(valueString);
                Integer end = start + Integer.parseInt(rangeFacet.getGap());
                // to avoid overlapping dates
                end = end - 1;
                if (rangeFacetCount.getCount() > 0) {
                    FacetLabel facetLabel = new FacetLabel(start + "-" + end,start + "-" + end, (long) rangeFacetCount.getCount());
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

        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        solrQueryResponse.setFacetCategoryList(facetCategoryList);
        solrQueryResponse.setNumResultsFound(queryResponse.getResults().getNumFound());
        solrQueryResponse.setResultsStart(queryResponse.getResults().getStart());
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

        solrQueryResponse.setPublicationStatusCounts(convertFacetToPublicationStatusCounts(queryResponse.getFacetField(SearchFields.PUBLICATION_STATUS)));

        return solrQueryResponse;
    }

    public String getLocaleFacetCategoryName(String facetCategoryName) {
        return getLocaleFacetCategoryName(facetCategoryName, null);
    }

    public String getLocaleFacetCategoryName(String facetCategoryName, Map<String, List<DatasetFieldType>> index) {
        final String formattedFacetFieldName = removeSolrFieldSuffix(facetCategoryName);
        List<DatasetFieldType> matchingFields = index != null
                ? index.getOrDefault(formattedFacetFieldName, Collections.emptyList())
                : datasetFieldService.findAllOrderedByName().stream()
                .filter(dsField -> dsField.getName().equals(formattedFacetFieldName))
                .collect(Collectors.toList());

        if (matchingFields.size() > 1) {
            throw new IllegalStateException("DatasetFieldType.name should be unique");
        } else if (matchingFields.size() == 0) {
            return getNonDatasetFieldFacetCategoryName(facetCategoryName);
        } else {
            DatasetFieldType matchedDatasetField = matchingFields.get(0);
            return getDatasetFieldFacetCategoryName(matchedDatasetField);
        }
    }

    public String getLocaleFacetLabelName(String facetLabelName, String facetCategoryName) {
        return getLocaleFacetLabelName(facetLabelName, facetCategoryName, null);
    }

    public String getLocaleFacetLabelName(String facetLabelName, String facetCategoryName,
                                          Map<String, List<DatasetFieldType>> index) {
        String formattedFacetCategoryName = removeSolrFieldSuffix(facetCategoryName);
        String formattedFacetLabelName = toBundleNameFormat(facetLabelName);
        List<DatasetFieldType> matchingFields = index != null
                ? index.getOrDefault(formattedFacetCategoryName, Collections.emptyList())
                : datasetFieldService.findAllOrderedByName().stream()
                    .filter(dsField -> dsField.getName().equals(formattedFacetCategoryName))
                    .collect(Collectors.toList());
        if (matchingFields.size() > 1) {
            throw new IllegalStateException("DatasetFieldType.name should be unique");
        } else if (matchingFields.size() == 0) {
            return getNonDatasetFieldFacetLabelName(facetLabelName, formattedFacetCategoryName);
        } else {
            DatasetFieldType matchedDatasetField = matchingFields.get(0);

            return getDatasetFieldFacetLabelName(facetLabelName, formattedFacetLabelName, matchedDatasetField);
        }
    }

    // -------------------- PRIVATE --------------------

    private String getDatasetFieldFacetCategoryName(DatasetFieldType matchedDatasetField) {
        if (matchedDatasetField.isFacetable() && !matchedDatasetField.isHasParent()) {
            String key = format(FACETBUNDLE_MASK_VALUE, matchedDatasetField.getName());
            return Optional.ofNullable(BundleUtil.getStringFromBundle(key))
                    .filter(name -> !name.isEmpty())
                    .orElse(matchedDatasetField.getDisplayName());
        }
        return matchedDatasetField.getDisplayName();
    }

    private String getNonDatasetFieldFacetCategoryName(String facetCategoryName) {
        if(facetCategoryName.equals(SearchFields.TYPE)) {
            return facetCategoryName;
        }
        String key = format(FACETBUNDLE_MASK_VALUE, facetCategoryName);
        return BundleUtil.getStringFromBundle(key);
    }

    private String getDatasetFieldFacetLabelName(String facetLabelName, String formattedFacetLabelName,
                                                 DatasetFieldType matchedDatasetField) {
        if (matchedDatasetField.isControlledVocabulary()) {
            String key = "controlledvocabulary." + matchedDatasetField.getName() + "." + formattedFacetLabelName;
            String bundleName = matchedDatasetField.getMetadataBlock().getName().toLowerCase();
            return BundleUtil.getStringFromNonDefaultBundle(key, bundleName);
        }
        return facetLabelName;
    }

    private String getNonDatasetFieldFacetLabelName(String facetLabelName, String formattedFacetCategoryName) {
        String formattedFacetLabelName = toBundleNameFormat(facetLabelName);
        List<String> translatableNonDictionaryFacets = Lists.newArrayList(SearchFields.PUBLICATION_STATUS,
                SearchFields.DATAVERSE_CATEGORY, SearchFields.FILE_TYPE, SearchFields.ACCESS);

        if(translatableNonDictionaryFacets.contains(formattedFacetCategoryName)) {
            if(formattedFacetCategoryName.equals(SearchFields.DATAVERSE_CATEGORY)) {
                String key = format(FACETBUNDLE_MASK_DVCATEGORY_VALUE, formattedFacetLabelName);
                return BundleUtil.getStringFromBundle(key);
            }
            String key = format(FACETBUNDLE_MASK_GROUP_AND_VALUE, formattedFacetCategoryName, formattedFacetLabelName);
            return BundleUtil.getStringFromBundle(key);
        }

        if(formattedFacetCategoryName.equals(SearchFields.METADATA_SOURCE) && formattedFacetLabelName.equals("harvested")) {
            return BundleUtil.getStringFromBundle(formattedFacetLabelName);
        }

        return facetLabelName;
    }

    private SolrQuery addHighlightFields(SolrQuery solrQuery, Map<String, String> solrFieldsToHightlightOnMap) {
        Set<String> dynamicDatasetFieldsPrefixes = new HashSet<>();

        for(String field : solrFieldsToHightlightOnMap.keySet()) {
            if(isFieldDynamic(field)) {
                dynamicDatasetFieldsPrefixes.add(field.substring(0, 8));
            } else {
                solrQuery.addHighlightField(field);
            }
        }

        for (String dynamicFieldPrefix : dynamicDatasetFieldsPrefixes) {
            solrQuery.addHighlightField(dynamicFieldPrefix + "*");
        }

        return solrQuery;
    }

    private boolean isFieldDynamic(String field) {
        return field.length() > 8 && SearchDynamicFieldPrefix.contains(field.substring(0, 8));
    }

    private String removeSolrFieldSuffix(String name) {
        if(name.endsWith("_ss")) {
            name = name.substring(0, name.length() - 3);
        } else if (name.endsWith("_s")) {
            name = name.substring(0, name.length() - 2);
        }
        return name;
    }

    /**
     * if exist, multi word bundle names are connected with underscores and formatted toLowerCase
     * @param name text for which we want to create its bundle name
     * @return text with replaced spaces with underscores, and leading/trailing whitespaces removed, toLowerCased
     */
    private String toBundleNameFormat(String name) {
        return StringUtils.stripAccents(name.toLowerCase().replace(" ", "_"));
    }

    private DvObjectCounts convertFacetToDvObjectCounts(FacetField dvObjectFacetField) {

        DvObjectCounts dvObjectCounts = DvObjectCounts.emptyDvObjectCounts();
        if (dvObjectFacetField == null) {
            return dvObjectCounts;
        }

        for (Count count: dvObjectFacetField.getValues()) {
            SearchObjectType dvType = SearchObjectType.fromSolrValue(count.getName());
            dvObjectCounts.setCountByObjectType(dvType, count.getCount());
        }
        return dvObjectCounts;
    }

    private PublicationStatusCounts convertFacetToPublicationStatusCounts(FacetField publicationStatusFacetField) {

        PublicationStatusCounts publicationStatusCounts = PublicationStatusCounts.emptyPublicationStatusCounts();
        if (publicationStatusFacetField == null) {
            return publicationStatusCounts;
        }

        for (Count count: publicationStatusFacetField.getValues()) {
            SearchPublicationStatus status = SearchPublicationStatus.fromSolrValue(count.getName());
            publicationStatusCounts.setCountByPublicationStatus(status, count.getCount());
        }
        return publicationStatusCounts;
    }

    private void addDvObjectTypeFilterQuery(SolrQuery query, SearchForTypes typesToSearch) {
        String filterValue = typesToSearch.getTypes().stream()
                .sorted()
                .map(SearchObjectType::getSolrValue)
                .collect(Collectors.joining(" OR "));

        query.addFilterQuery(SearchFields.TYPE + ":(" + filterValue + ")");
    }

    private String getLocalizedValueWithFallback(SolrDocument document, String fieldName) {
        String suffix = "_" + BundleUtil.getCurrentLocale().getLanguage();
        return (String) (document.containsKey(fieldName + suffix)
                ? document.getFieldValue(fieldName + suffix)
                : document.getFieldValue(fieldName + "_en"));
    }
}
