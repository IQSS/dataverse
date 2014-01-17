package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;
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

    public SolrQueryResponse search(String query, List<String> filterQueries, int paginationStart) {
        /**
         * @todo make "localhost" and port number a config option
         */
        SolrServer solrServer = new HttpSolrServer("http://localhost:8983/solr");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setHighlight(true).setHighlightSnippets(1);
        solrQuery.setParam("hl.fl", SearchFields.DESCRIPTION);
        solrQuery.setParam("qt", "/spell");
        solrQuery.setParam("facet", "true");
        /**
         * @todo: do we need facet.query?
         */
        solrQuery.setParam("facet.query", "*");
        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }
        solrQuery.addFacetField(SearchFields.TYPE);
        solrQuery.addFacetField(SearchFields.SUBTREE);
        solrQuery.addFacetField(SearchFields.ORIGINAL_DATAVERSE);
        solrQuery.addFacetField(SearchFields.AUTHOR_STRING);
        solrQuery.addFacetField(SearchFields.AFFILIATION);
        solrQuery.addFacetField(SearchFields.CATEGORY);
//        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        solrQuery.addFacetField(SearchFields.FILE_TYPE_GROUP);
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
        final int citationYearRangeStart = 2000;
        final int citationYearRangeEnd = thisYear;
        final int citationYearRangeSpan = 2;
        solrQuery.addNumericRangeFacet(SearchFields.CITATION_YEAR, citationYearRangeStart, citationYearRangeEnd, citationYearRangeSpan);
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
        List<String> highlightSnippets = null;
        List<SolrSearchResult> solrSearchResults = new ArrayList<>();
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
            String affiliation = (String) solrDocument.getFieldValue(SearchFields.AFFILIATION);
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String type = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            ArrayList titles = (ArrayList) solrDocument.getFieldValues(SearchFields.TITLE);
            String filetype = (String) solrDocument.getFieldValue(SearchFields.FILE_TYPE);
            if (queryResponse.getHighlighting().get(id) != null) {
                highlightSnippets = queryResponse.getHighlighting().get(id).get(SearchFields.DESCRIPTION);
//                logger.info("highlight snippets: " + highlightSnippets);
            }
            SolrSearchResult solrSearchResult = new SolrSearchResult(query, highlightSnippets, name);
            /**
             * @todo put all this in the constructor?
             */
//            logger.info(id + ": " + description);
            solrSearchResult.setDescriptionNoSnippet(description);
            solrSearchResult.setId(id);
            solrSearchResult.setEntityId(entityid);
            solrSearchResult.setType(type);
            solrSearchResult.setAffiliation(affiliation);
            Map<String, String> parent = new HashMap<>();
            if (type.equals("dataverses")) {
                solrSearchResult.setName(name);
                parent.put("type", "dataverses");
            } else if (type.equals("datasets")) {
                if (titles != null) {
                    solrSearchResult.setTitle((String) titles.get(0));
                }
                parent.put("type", "datasets");
            } else if (type.equals("files")) {
                solrSearchResult.setName(name);
                solrSearchResult.setFiletype(filetype);
                parent.put("type", "files");
            }
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
            facetCategory.setFacetLabel(facetLabelList);
            facetCategoryList.add(facetCategory);
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
            facetCategoryList.add(facetCategory);
        }

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse();
        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        solrQueryResponse.setFacetCategoryList(facetCategoryList);
        solrQueryResponse.setNumResultsFound(queryResponse.getResults().getNumFound());
        solrQueryResponse.setResultsStart(queryResponse.getResults().getStart());
        return solrQueryResponse;
    }
}
