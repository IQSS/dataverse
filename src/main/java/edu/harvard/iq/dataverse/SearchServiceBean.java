package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    public SolrQueryResponse search(String queryFromUser) {
        /**
         * @todo make "localhost" and port number a config option
         */
        SolrServer solrServer = new HttpSolrServer("http://localhost:8983/solr");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryFromUser);
        solrQuery.setHighlight(true).setHighlightSnippets(1);
        solrQuery.setParam("hl.fl", SearchFields.DESCRIPTION);
        solrQuery.setParam("qt", "/spell");
        solrQuery.setParam("facet", "true");
        solrQuery.setParam("facet.query", "*");
        solrQuery.addFacetField(SearchFields.AUTHOR);

        QueryResponse queryResponse;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (SolrServerException ex) {
            throw new RuntimeException("Is the Solr server down?");
        }
        SolrDocumentList docs = queryResponse.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
        String name = null;
        List<String> highlightSnippets = null;
        List<SolrSearchResult> solrSearchResults = new ArrayList<>();
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
            String id = (String) solrDocument.getFieldValue(SearchFields.ID);
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String type = (String) solrDocument.getFieldValue(SearchFields.TYPE);
            logger.info(id + ": " + description);
            name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            Collection<String> fieldNames = solrDocument.getFieldNames();
            for (String fieldName : fieldNames) {
//                logger.info("field name: " + fieldName);
            }
            if (queryResponse.getHighlighting().get(id) != null) {
                highlightSnippets = queryResponse.getHighlighting().get(id).get(SearchFields.DESCRIPTION);
                logger.info("highlight snippets: " + highlightSnippets);
            }
            SolrSearchResult solrSearchResult = new SolrSearchResult(queryFromUser, highlightSnippets, name);
            /**
             * @todo put all this in the constructor?
             */
            solrSearchResult.setId(id);
            solrSearchResult.setEntityId(entityid);
            solrSearchResult.setType(type);
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

        List<String> facets = new ArrayList<>();
        for (FacetField facetField : queryResponse.getFacetFields()) {
            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                logger.info("field: " + facetField.getName() + " " + facetFieldCount.getName() + " (" + facetFieldCount.getCount() + ")");
                facets.add(facetField.getName() + ": " + facetFieldCount.getName() + " (" + facetFieldCount.getCount() + ")");
            }
        }

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse();
        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setFacets(facets);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        return solrQueryResponse;
    }
}
