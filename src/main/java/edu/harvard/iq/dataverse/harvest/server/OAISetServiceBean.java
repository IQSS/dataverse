/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.harvest.client.ClientHarvestRun;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author Leonid Andreev
 * dedicated service for managing OAI sets, 
 * for the Harvesting server.
 */

@Stateless
@Named
public class OAISetServiceBean implements java.io.Serializable {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    SystemConfig systemConfig;
    
    @EJB
    OAIRecordServiceBean oaiRecordService;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean");
    
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    public OAISet find(Object pk) {
        return (OAISet) em.find(OAISet.class, pk);
    }
    
    public boolean specExists(String spec) {
        boolean specExists = false;
        OAISet set = findBySpec(spec);
        
        if (set != null) {
            specExists = true;
        }
        return specExists;
    }

    public OAISet findBySpec(String spec) {
        String query = "SELECT o FROM OAISet o where o.spec = :specName";
        OAISet oaiSet = null;
        logger.fine("Query: "+query+"; spec: "+spec);
        try {
            oaiSet = (OAISet) em.createQuery(query).setParameter("specName", spec).getSingleResult();
        } catch (Exception e) {
            // Do nothing, just return null. 
        }
        return oaiSet;
    }

    public List<OAISet> findAll() {
        try {
            logger.fine("setService, findAll; query: select object(o) from OAISet as o order by o.name");
            List<OAISet> oaiSets = em.createQuery("select object(o) from OAISet as o order by o.name").getResultList();
            logger.fine((oaiSets != null ? oaiSets.size() : 0) + " results found.");
            return oaiSets;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Asynchronous
    public void remove(Long setId) {
        OAISet oaiSet = find(setId);
        if (oaiSet == null) {
            return;
        }
        em.createQuery("delete from OAIRecord hs where hs.setName = '" + oaiSet.getSpec() + "'").executeUpdate();
        //OAISet merged = em.merge(oaiSet);
        em.remove(oaiSet);
    }
    
    public OAISet findById(Long id) {
       return em.find(OAISet.class,id);
    }   
    
    private SolrServer solrServer = null;
    
    private SolrServer getSolrServer () {
        if (solrServer == null) {
        }
        solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        
        return solrServer;
        
    }
    
    @Asynchronous
    public void exportOaiSetAsync(OAISet oaiSet) {
        exportOaiSet(oaiSet);
    }
    
    public void exportOaiSet(OAISet oaiSet) {
        exportOaiSet(oaiSet, logger);
    }
    
    public void exportOaiSet(OAISet oaiSet, Logger exportLogger) {
        OAISet managedSet = find(oaiSet.getId());
        
        String query = managedSet.getDefinition();

        List<Long> datasetIds = null;
        try {
            datasetIds = expandSetQuery(query);
            exportLogger.info("set query expanded to " + datasetIds.size() + " datasets.");
        } catch (OaiSetException ose) {
            datasetIds = null;
        }

        // We still DO want to update the set, when the search query does not 
        // find any datasets! - This way if there are records already in the set
        // they will be properly marked as "deleted"! -- L.A. 4.5
        //if (datasetIds != null && !datasetIds.isEmpty()) {
        exportLogger.info("Calling OAI Record Service to re-export " + datasetIds.size() + " datasets.");
        oaiRecordService.updateOaiRecords(managedSet.getSpec(), datasetIds, new Date(), true, exportLogger);
        //}
        managedSet.setUpdateInProgress(false);

    } 
    
    public void exportAllSets() {
        String logTimestamp = logFormatter.format(new Date());
        Logger exportLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.OAISetServiceBean." + "UpdateAllSets." + logTimestamp);
        String logFileName = "../logs" + File.separator + "oaiSetsUpdate_" + logTimestamp + ".log";
        FileHandler fileHandler = null;
        boolean fileHandlerSuceeded = false;
        try {
            fileHandler = new FileHandler(logFileName);
            exportLogger.setUseParentHandlers(false);
            fileHandlerSuceeded = true;
        } catch (IOException ex) {
            Logger.getLogger(DatasetServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(DatasetServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (fileHandlerSuceeded) {
            exportLogger.addHandler(fileHandler);
        } else {
            exportLogger = null;
            exportLogger = logger;
        }
        
        List<OAISet> allSets = findAll();
        
        if (allSets != null) {
            for (OAISet set : allSets) {
                exportOaiSet(set, exportLogger);
            }
        }
        
        if (fileHandlerSuceeded) {
            // no, we are not potentially de-referencing a NULL pointer - 
            // it's not NULL if fileHandlerSucceeded is true.
            fileHandler.close();
        }
    }
        
    public int validateDefinitionQuery(String query) throws OaiSetException {
        
        List<Long> resultIds = expandSetQuery(query);
        //logger.fine("Datasets found: "+StringUtils.join(resultIds, ","));
        
        if (resultIds != null) {
            //logger.fine("returning "+resultIds.size());
            return resultIds.size();
        }
        
        return 0;
    }
    
    /**
     * @deprecated Consider using commented out solrQuery.addFilterQuery
     * examples instead.
     */
    @Deprecated
    public String addQueryRestrictions(String query) {
        // "sanitizeQuery()" does something special that's needed to be able 
        // to search on global ids; which we will most likely need. 
        query = SearchUtil.sanitizeQuery(query);
        // fix case in "and" and "or" operators: 
        query = query.replaceAll(" [Aa][Nn][Dd] ", " AND ");
        query = query.replaceAll(" [Oo][Rr] ", " OR ");
        query = "(" + query + ")";
        // append the search clauses that limit the search to a) datasets
        // b) published and c) local: 
        // SearchFields.TYPE
        query = query.concat(" AND " + SearchFields.TYPE + ":" + SearchConstants.DATASETS + " AND " + SearchFields.IS_HARVESTED + ":" + false + " AND " + SearchFields.PUBLICATION_STATUS + ":" + IndexServiceBean.PUBLISHED_STRING);
        
        return query;
    }
    
    public List<Long> expandSetQuery(String query) throws OaiSetException {
        // We do not allow "keyword" queries (like "king") - we require
        // that they search on specific fields, for ex., "authorName:king":
        if (query == null || !(query.indexOf(':') > 0)) {
            throw new OaiSetException("Invalid search query.");
        }
        SolrQuery solrQuery = new SolrQuery();
        String restrictedQuery = addQueryRestrictions(query);
        
        solrQuery.setQuery(restrictedQuery);

        // addFilterQuery equivalent to addQueryRestrictions
//        solrQuery.setQuery(query);
//        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + SearchConstants.DATASETS);
//        solrQuery.addFilterQuery(SearchFields.IS_HARVESTED + ":" + false);
//        solrQuery.addFilterQuery(SearchFields.PUBLICATION_STATUS + ":" + IndexServiceBean.PUBLISHED_STRING);

        solrQuery.setRows(Integer.MAX_VALUE);

        
        QueryResponse queryResponse = null;
        try {
            queryResponse = getSolrServer().query(solrQuery);
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
            logger.fine(error);
            throw new OaiSetException(error);
        } catch (SolrServerException ex) {
            logger.fine("Internal Dataverse Search Engine Error");
            throw new OaiSetException("Internal Dataverse Search Engine Error");
        }
        
        SolrDocumentList docs = queryResponse.getResults();
        Iterator<SolrDocument> iter = docs.iterator();
        List<Long> resultIds = new ArrayList<>();
        
        while (iter.hasNext()) {
            SolrDocument solrDocument = iter.next();
            Long entityid = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            resultIds.add(entityid);
        }
        
        return resultIds;
        
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setUpdateInProgress(Long setId) {
        OAISet oaiSet = find(setId);
        if (oaiSet == null) {
            return;
        }
        em.refresh(oaiSet);
        oaiSet.setUpdateInProgress(true);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setDeleteInProgress(Long setId) {
        OAISet oaiSet = find(setId);
        
        if (oaiSet == null) {
            return;
        }
        em.refresh(oaiSet);
        oaiSet.setDeleteInProgress(true);
    }
    
    public void save(OAISet oaiSet) {
        em.merge(oaiSet);
    }
    
}
