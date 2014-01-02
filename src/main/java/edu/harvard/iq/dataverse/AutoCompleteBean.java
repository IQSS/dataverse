package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;

/**
 * @todo: move to service bean
 */
@Named
public class AutoCompleteBean {

    private static final Logger logger = Logger.getLogger(AutoCompleteBean.class.getCanonicalName());

    public List<String> complete(String query) {
        List<String> results = new ArrayList<>();

        /**
         * @todo make "localhost" and port number a config option
         */
        SolrServer solrServer = new HttpSolrServer("http://localhost:8983/solr");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setParam("qt", "/terms");
        solrQuery.setTermsLower(query);
        solrQuery.setTermsPrefix(query);
        solrQuery.addTermsField(SearchFields.NAME);
        List<Term> items = null;

        try {
            logger.info("Solr query: " + solrQuery);
            QueryResponse qr = solrServer.query(solrQuery);
            TermsResponse resp = qr.getTermsResponse();
            items = resp.getTerms(SearchFields.NAME);
        } catch (SolrServerException e) {
            items = null;
        }

        if (items != null) {
            for (Term term : items) {
                logger.info("term: " + term.getTerm());
                results.add(term.getTerm());
            }
        } else {
            logger.info("no terms found");
        }

        return results;
    }

}
