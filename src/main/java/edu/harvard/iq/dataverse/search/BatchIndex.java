/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author roberttreacy
 */
public class BatchIndex {
private static final Logger logger = Logger.getLogger(BatchIndex.class.getCanonicalName());
    private Collection<SolrInputDocument> docs;
    long batchsize;
    private SolrClient server;

    
    public BatchIndex(long size, SolrClient serv){
        batchsize = size;
        docs = new ArrayList<>();
        server = serv;
    }
    
    public void add(SolrInputDocument doc){
        docs.add(doc);
        if (docs.size()== batchsize){
            indexDocs(getDocs());
        }
    }
    
    public String indexDocs(Collection<SolrInputDocument> docs) {
        logger.info("Starting BATCHindex of "+docs.size()+" documents");
        try {
            getServer().add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            getServer().commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        docs.clear();
        return "indexed " + batchsize + "documents";

    }
    
    public String finish(){
        return indexDocs(docs);
    }

    /**
     * @return the docs
     */
    public Collection<SolrInputDocument> getDocs() {
        return docs;
    }

    /**
     * @param docs the docs to set
     */
    public void setDocs(Collection<SolrInputDocument> docs) {
        this.docs = docs;
    }

    /**
     * @return the server
     */
    public SolrClient getServer() {
        return server;
    }

}
