package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;

/**
 * @todo: move to service bean
 */
@Named
public class AutoCompleteBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(AutoCompleteBean.class.getCanonicalName());

    @EJB
    SystemConfig systemConfig;
    
    private static SolrClient solrServer;
    
    public SolrClient getSolrServer(){
        if(systemConfig.solrUsesJAAS()){
          System.setProperty("java.security.auth.login.config",systemConfig.getSolrJAASClientConfFile());
          HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
        }
        if(solrServer == null){
            if (systemConfig.isSolrCloudZookeeperEnabled()) {
                solrServer = new CloudSolrClient(systemConfig.getSolrZookeeperEnsemble());
                ((CloudSolrClient)solrServer).setDefaultCollection(systemConfig.getSolrCollectionName());
                ((CloudSolrClient)solrServer).connect();
            }else{
               solrServer = new HttpSolrClient(systemConfig.getSolrUrlSchema() + systemConfig.getSolrHostColonPort() + "/" + systemConfig.getSolrServiceName() + "/" + systemConfig.getSolrCollectionName());
            }
        }
        return solrServer;
    }

    public List<String> complete(String query) {
        List<String> results = new ArrayList<>();

        solrServer = getSolrServer();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setParam("qt", "/terms");
        solrQuery.setTermsLower(query);
        solrQuery.setTermsPrefix(query);
        // dataverses and files use SearchFields.NAME
//        solrQuery.addTermsField(SearchFields.NAME);
        solrQuery.addTermsField("text");
//        long datasetFieldDescription = 33L;
//        String solrFieldDatasetFieldDescription = datasetFieldService.find(datasetFieldDescription).getSolrField();
//        solrQuery.addTermsField(solrFieldDatasetFieldDescription);
        List<Term> items = null;

        try {
            logger.info("Solr query: " + solrQuery);
            QueryResponse qr = solrServer.query(solrQuery);
            TermsResponse resp = qr.getTermsResponse();
//            items = resp.getTerms(SearchFields.NAME);
            items = resp.getTerms("text");
//            items = resp.getTerms(solrFieldDatasetFieldDescription);
        } catch (SolrServerException | IOException e) {
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
