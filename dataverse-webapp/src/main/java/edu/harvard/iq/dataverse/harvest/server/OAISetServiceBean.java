package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecordRepository;
import edu.harvard.iq.dataverse.persistence.harvest.OAISet;
import edu.harvard.iq.dataverse.persistence.harvest.OAISetRepository;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * @author Leonid Andreev
 * dedicated service for managing OAI sets,
 * for the Harvesting server.
 */

@Stateless
public class OAISetServiceBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean");
    
    private static final String LOG_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss";

    @Inject
    private OAISetRepository oaiSetRepository;

    @Inject
    private OAIRecordRepository oaiRecordRepository;

    @Inject
    private OAIRecordServiceBean oaiRecordService;

    @Inject
    private DatasetRepository datasetRepository;
    
    @Inject
    private SolrClient solrServer;
    
    @Inject
    private SolrQuerySanitizer querySanitizer;


    public boolean specExists(String spec) {
        return oaiSetRepository.findBySpecName(spec).isPresent();
    }

    public OAISet findBySpec(String spec) {
        return oaiSetRepository.findBySpecName(spec).orElse(null);
    }

    // Find the default, "no name" set:
    public OAISet findDefaultSet() {
        return oaiSetRepository.findBySpecName(OAISet.DEFAULT_SET_SPEC_NAME).orElse(null);
    }

    public List<OAISet> findAll() {
        return oaiSetRepository.findAll();
    }

    public List<OAISet> findAllNamedSets() {
        return oaiSetRepository.findAllBySpecNameNot(OAISet.DEFAULT_SET_SPEC_NAME);
    }

    @Asynchronous
    public void remove(Long setId) {
        oaiSetRepository.findById(setId).ifPresent(oaiSet -> {

            oaiRecordRepository.deleteBySetName(oaiSet.getSpec());
            oaiSetRepository.delete(oaiSet);
        });
    }

    @Asynchronous
    public void exportOaiSetAsync(OAISet oaiSet) {
        exportOaiSet(oaiSet);
    }

    private void exportOaiSet(OAISet oaiSet) {
        exportOaiSet(oaiSet, logger);
    }

    private void exportOaiSet(OAISet oaiSet, Logger exportLogger) {
        OAISet managedSet = oaiSetRepository.getById(oaiSet.getId());

        String query = managedSet.getDefinition();

        List<Long> datasetIds;
        try {
            if (!oaiSet.isDefaultSet()) {
                datasetIds = expandSetQuery(query);
                exportLogger.info("set query expanded to " + datasetIds.size() + " datasets.");
            } else {
                // The default set includes all the local, published datasets. 
                // findIdsByNullHarvestedFrom() will
                // include the unpublished drafts and deaccessioned ones.
                // Those will be filtered out further down the line. 
                datasetIds = datasetRepository.findIdsByNullHarvestedFrom();
            }
        } catch (OaiSetException ose) {
            throw new RuntimeException("Unable to retrieve dataset ids", ose);
        }

        // We still DO want to update the set, when the search query does not 
        // find any datasets! - This way if there are records already in the set
        // they will be properly marked as "deleted"! -- L.A. 4.5
        //if (datasetIds != null && !datasetIds.isEmpty()) {
        exportLogger.info("Calling OAI Record Service to re-export " + datasetIds.size() + " datasets.");
        oaiRecordService.updateOaiRecords(managedSet.getSpec(), datasetIds, exportLogger);
        //}
        managedSet.setUpdateInProgress(false);

    }

    public void exportAllSets() {
        String logTimestamp = new SimpleDateFormat(LOG_DATE_FORMAT).format(new Date());
        Logger exportLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.OAISetServiceBean." + "UpdateAllSets." + logTimestamp);
        String logFileName = "../logs" + File.separator + "oaiSetsUpdate_" + logTimestamp + ".log";
        FileHandler fileHandler = null;
        boolean fileHandlerSuceeded = false;
        try {
            fileHandler = new FileHandler(logFileName);
            exportLogger.setUseParentHandlers(false);
            fileHandlerSuceeded = true;
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DatasetDao.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (fileHandlerSuceeded) {
            exportLogger.addHandler(fileHandler);
        } else {
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
        return resultIds.size();
    }

    public List<Long> expandSetQuery(String query) throws OaiSetException {
        // We do not allow "keyword" queries (like "king") - we require
        // that they search on specific fields, for ex., "authorName:king":
        if (query == null || query.indexOf(':') == -1) {
            throw new OaiSetException("Invalid search query.");
        }
        SolrQuery solrQuery = new SolrQuery();
        String sanitizedQuery = querySanitizer.sanitizeQuery(query);
        sanitizedQuery = sanitizedQuery.replaceAll(" [Aa][Nn][Dd] ", " AND ");
        sanitizedQuery = sanitizedQuery.replaceAll(" [Oo][Rr] ", " OR ");

        solrQuery.setQuery(sanitizedQuery);

        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + SearchObjectType.DATASETS.getSolrValue());
        solrQuery.addFilterQuery(SearchFields.IS_HARVESTED + ":" + false);
        solrQuery.addFilterQuery(SearchFields.PUBLICATION_STATUS + ":" + SearchPublicationStatus.PUBLISHED.getSolrValue());

        solrQuery.setRows(Integer.MAX_VALUE);


        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (RemoteSolrException ex) {
            String messageFromSolr = ex.getMessage();
            String error = StringUtils.replace(messageFromSolr, "org.apache.solr.search.SyntaxError: ", "Search Syntax Error: ", 1);
            logger.fine(error);
            throw new OaiSetException(error, ex);
        } catch (SolrServerException | IOException ex) {
            logger.warning("Internal Dataverse Search Engine Error");
            throw new OaiSetException("Internal Dataverse Search Engine Error", ex);
        }

        return queryResponse.getResults().stream()
                .map(solrDocument -> (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID))
                .collect(toList());

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setUpdateInProgress(Long setId) {
        oaiSetRepository.findById(setId).ifPresent(oaiSet -> {

            oaiSet.setUpdateInProgress(true);
            oaiSetRepository.save(oaiSet);
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setDeleteInProgress(Long setId) {
        oaiSetRepository.findById(setId).ifPresent(oaiSet -> {

            oaiSet.setDeleteInProgress(true);
            oaiSetRepository.save(oaiSet);
        });
    }

    public void save(OAISet oaiSet) {
        oaiSetRepository.save(oaiSet);
    }

}
