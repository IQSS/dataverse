package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.search.Highlight;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * @todo stop indexing with curl (commands below)
 */
//mkdir data
//curl http://localhost:8080/api/dataverses > data/dataverses.json
//curl http://localhost:8983/solr/update/json?commit=true -H 'Content-type:application/json' --data-binary @data/dataverses.json
@Stateless
@Named
public class SearchServiceBean {

    private static final Logger logger = Logger.getLogger(SearchServiceBean.class.getCanonicalName());

    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DataverseUserServiceBean dataverseUserService;

    PublishedToggle publishedToggle = PublishedToggle.PUBLISHED;

    public enum PublishedToggle {

        PUBLISHED, UNPUBLISHED
    };

    public SolrQueryResponse search(DataverseUser dataverseUser, Dataverse dataverse, String query, List<String> filterQueries, String sortField, String sortOrder, int paginationStart, PublishedToggle publishedToggle) {
        if (publishedToggle.equals(PublishedToggle.PUBLISHED)) {
            filterQueries.add(SearchFields.PUBLICATION_STATUS + ":" + IndexServiceBean.getPUBLISHED_STRING());
        } else {
            filterQueries.add(SearchFields.PUBLICATION_STATUS + ":" + IndexServiceBean.getUNPUBLISHED_STRING());
        }
        /**
         * @todo make "localhost" and port number a config option
         */
        SolrServer solrServer = new HttpSolrServer("http://localhost:8983/solr");
        SolrQuery solrQuery = new SolrQuery();
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
        solrQuery.setHighlightSimplePre("<span class=\"search-term-match\">");
        solrQuery.setHighlightSimplePost("</span>");
        Map<String,String> solrFieldsToHightlightOnMap = new HashMap<>();
        solrFieldsToHightlightOnMap.put(SearchFields.NAME, "Name");
        solrFieldsToHightlightOnMap.put(SearchFields.AFFILIATION, "Affiliation");
        solrFieldsToHightlightOnMap.put(SearchFields.CITATION, "Citation");
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_MIME, "File Type");
        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        for (DatasetFieldType datasetFieldType: datasetFields) {
            String solrField = datasetFieldType.getSolrField().getNameSearchable();
            String displayName = datasetFieldType.getDisplayName();
            solrFieldsToHightlightOnMap.put(solrField, displayName);
        }
        for (Map.Entry<String, String> entry : solrFieldsToHightlightOnMap.entrySet()) {
            String solrField = entry.getKey();
            // String displayName = entry.getValue();
            solrQuery.addHighlightField(solrField);
        }
        solrQuery.setParam("qt", "/spell");
        solrQuery.setParam("facet", "true");
        /**
         * @todo: do we need facet.query?
         */
        solrQuery.setParam("facet.query", "*");
        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }

        String publicOnly = "{!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getPublicGroupString();
        // initialize to public only to be safe
        String permissionFilterQuery = publicOnly;
        if (dataverseUser != null) {
            if (dataverseUser.isGuest()) {
                permissionFilterQuery = publicOnly;
            } else {
                /**
                 * Non-guests might get more than public stuff with an OR or
                 * two.
                 *
                 * Unless you're part of some special group, you get the "User
                 * Private Group" (UGP) that corresponds to your username:
                 * https://access.redhat.com/site/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Deployment_Guide/ch-Managing_Users_and_Groups.html#s2-users-groups-private-groups
                 */
                String publicPlusUserPrivateGroup = "("
                        + publicOnly
                        + " OR {!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getGroupPerUserPrefix() + dataverseUser.getId() + ")";
                /**
                 * @todo: replace this with a real group... look up the user's
                 * groups (once you can)
                 */
                if (dataverseUser.getPosition().equals("Signals Intelligence")) {
                    String publicPlusUserPrivateGroupPlusNSA = "("
                            + publicOnly
                            + " OR {!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getGroupPerUserPrefix() + dataverseUser.getId()
                            + " OR {!join from=" + SearchFields.GROUPS + " to=" + SearchFields.PERMS + "}id:" + IndexServiceBean.getGroupPrefix() + IndexServiceBean.getTmpNsaGroupId()
                            + ")";
                    permissionFilterQuery = publicPlusUserPrivateGroupPlusNSA;
                } else {
                    // not part of any particular group 
                    permissionFilterQuery = publicPlusUserPrivateGroup;
                }
            }
        }
        solrQuery.addFilterQuery(permissionFilterQuery);

//        solrQuery.addFacetField(SearchFields.HOST_DATAVERSE);
//        solrQuery.addFacetField(SearchFields.AUTHOR_STRING);
        solrQuery.addFacetField(SearchFields.AFFILIATION);
        solrQuery.addFacetField(SearchFields.RELEASE_DATE);
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
        for (DataverseFacet dataverseFacet: dataverse.getDataverseFacets()) {
            DatasetFieldType datasetField = dataverseFacet.getDatasetFieldType();
            solrQuery.addFacetField(datasetField.getSolrField().getNameFacetable());
        }
        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        /**
         * @todo: hide the extra line this shows in the GUI... at least it's last...
         */
        solrQuery.addFacetField(SearchFields.TYPE);
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
         * @todo: these are dates and should be "range facets" not "field facets"
         * 
         * right now they are lumped in with the datasetFieldService.findAll() above
         */
//        solrQuery.addNumericRangeFacet(SearchFields.PRODUCTION_DATE_YEAR_ONLY, citationYearRangeStart, citationYearRangeEnd, citationYearRangeSpan);
//        solrQuery.addNumericRangeFacet(SearchFields.DISTRIBUTION_DATE_YEAR_ONLY, citationYearRangeStart, citationYearRangeEnd, citationYearRangeSpan);
        /**
         * @todo: make the number of results per page configurable?
         */
        int numResultsPerPage = 10;
        solrQuery.setRows(numResultsPerPage);
        logger.info("Solr query:" + solrQuery);

        QueryResponse queryResponse;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (SolrServerException ex) {
            throw new RuntimeException("Is the Solr server down?");
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
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String type = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            String nameSort = (String) solrDocument.getFieldValue(SearchFields.NAME_SORT);
//            ArrayList titles = (ArrayList) solrDocument.getFieldValues(SearchFields.TITLE);
            String title = (String) solrDocument.getFieldValue(titleSolrField);
//            logger.info("titleSolrField: " + titleSolrField);
//            logger.info("title: " + title);
            String filetype = (String) solrDocument.getFieldValue(SearchFields.FILE_TYPE_MIME);
            Date release_or_create_date = (Date) solrDocument.getFieldValue(SearchFields.RELEASE_OR_CREATE_DATE);
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
//            logger.info(id + ": " + description);
            solrSearchResult.setDescriptionNoSnippet(description);
            solrSearchResult.setId(id);
            solrSearchResult.setEntityId(entityid);
            solrSearchResult.setType(type);
            solrSearchResult.setNameSort(nameSort);
            solrSearchResult.setReleaseOrCreateDate(release_or_create_date);
            solrSearchResult.setMatchedFields(matchedFields);
            solrSearchResult.setHighlightsAsList(highlights);
            solrSearchResult.setHighlightsMap(highlightsMap);
            solrSearchResult.setHighlightsAsMap(highlightsMap3);
            Map<String, String> parent = new HashMap<>();
            if (type.equals("dataverses")) {
                solrSearchResult.setName(name);
            } else if (type.equals("datasets")) {
                if (title != null) {
//                    solrSearchResult.setTitle((String) titles.get(0));
                    solrSearchResult.setTitle((String) title);
                }
                else {
                    solrSearchResult.setTitle("NULL: NO TITLE INDEXED OR PROBLEM FINDING TITLE DATASETFIELD");
                }
            } else if (type.equals("files")) {
                solrSearchResult.setName(name);
                solrSearchResult.setFiletype(filetype);
            }
            /**
             * @todo store PARENT_ID as a long instead and cast as such
             */
            parent.put("id", (String) solrDocument.getFieldValue(SearchFields.PARENT_ID));
            parent.put("name", (String) solrDocument.getFieldValue(SearchFields.PARENT_NAME));
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
        for (FacetField facetField : queryResponse.getFacetFields()) {
            FacetCategory facetCategory = new FacetCategory();
            List<FacetLabel> facetLabelList = new ArrayList<>();
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
                }
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
                facetCategoryList.add(facetCategory);
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

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse();
        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        solrQueryResponse.setFacetCategoryList(facetCategoryList);
        solrQueryResponse.setNumResultsFound(queryResponse.getResults().getNumFound());
        solrQueryResponse.setResultsStart(queryResponse.getResults().getStart());
        solrQueryResponse.setDatasetfieldFriendlyNamesBySolrField(datasetfieldFriendlyNamesBySolrField);
        solrQueryResponse.setStaticSolrFieldFriendlyNamesBySolrField(staticSolrFieldFriendlyNamesBySolrField);
        solrQueryResponse.setFilterQueriesActual(Arrays.asList(solrQuery.getFilterQueries()));
        return solrQueryResponse;
    }

    public String getCapitalizedName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
