/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
//import java.net.URLEncoder;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.bean.ManagedBean;
import javax.inject.Named;
//import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.mutable.MutableLong;
import org.xml.sax.SAXException;

import com.lyncode.xoai.model.oaipmh.Header;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Leonid Andreev
 */
@Stateless(name = "harvesterService")
@Named
@ManagedBean
public class HarvesterServiceBean {
    @PersistenceContext(unitName="VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @Resource
    javax.ejb.TimerService timerService;
    @EJB
    DataverseTimerServiceBean dataverseTimerService;
    @EJB
    HarvestingClientServiceBean harvestingClientService;
    @EJB
    ImportServiceBean importService;
    @EJB
    EjbDataverseEngine engineService;
    @EJB
    IndexServiceBean indexService;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean");
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    public static final String HARVEST_RESULT_SUCCESS="success";
    public static final String HARVEST_RESULT_FAILED="failed";
    private static final Long  INDEXING_CONTENT_BATCH_SIZE = 10000000L;


    public HarvesterServiceBean() {

    }
    
    /**
     * Called to run an "On Demand" harvest.  
     */
    @Asynchronous
    public void doAsyncHarvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient) {
        
        try {
            doHarvest(dataverseRequest, harvestingClient.getId());
        } catch (Exception e) {
            logger.info("Caught exception running an asynchronous harvest (dataverse \""+harvestingClient.getName()+"\")");
        }
    }

    public void createScheduledHarvestTimers() {
        logger.log(Level.INFO, "HarvesterService: going to (re)create Scheduled harvest timers.");
        dataverseTimerService.removeHarvestTimers();

        List configuredClients = harvestingClientService.getAllHarvestingClients();
        for (Iterator it = configuredClients.iterator(); it.hasNext();) {
            HarvestingClient harvestingConfig = (HarvestingClient) it.next();
            if (harvestingConfig.isScheduled()) {
                dataverseTimerService.createHarvestTimer(harvestingConfig);
            }
        }
    }
  
    public List<HarvestTimerInfo> getHarvestTimers() {
        ArrayList <HarvestTimerInfo>timers = new ArrayList<>();
        
        for (Iterator it = timerService.getTimers().iterator(); it.hasNext();) {
            Timer timer = (Timer) it.next();
            if (timer.getInfo() instanceof HarvestTimerInfo) {
                HarvestTimerInfo info = (HarvestTimerInfo) timer.getInfo();
                timers.add(info);
            }
        }    
        return timers;
    }

    /**
     * Run a harvest for an individual harvesting Dataverse
     * @param dataverseRequest
     * @param harvestingClientId
     * @throws IOException
     */
    public void doHarvest(DataverseRequest dataverseRequest, Long harvestingClientId) throws IOException {
        HarvestingClient harvestingClientConfig = harvestingClientService.find(harvestingClientId);
        
        if (harvestingClientConfig == null) {
            throw new IOException("No such harvesting client: id="+harvestingClientId);
        }
        
        Dataverse harvestingDataverse = harvestingClientConfig.getDataverse();
        
        MutableBoolean harvestErrorOccurred = new MutableBoolean(false);
        String logTimestamp = logFormatter.format(new Date());
        Logger hdLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean." + harvestingDataverse.getAlias() + logTimestamp);
        String logFileName = "../logs" + File.separator + "harvest_" + harvestingClientConfig.getName() + "_" + logTimestamp + ".log";
        FileHandler fileHandler = new FileHandler(logFileName);
        hdLogger.setUseParentHandlers(false);
        hdLogger.addHandler(fileHandler);
        
        PrintWriter importCleanupLog = new PrintWriter(new FileWriter( "../logs/harvest_cleanup_" + harvestingClientConfig.getName() + "_" + logTimestamp+".txt"));
        
        
        List<Long> harvestedDatasetIds = null;

        List<Long> harvestedDatasetIdsThisBatch = new ArrayList<Long>();

        List<String> failedIdentifiers = new ArrayList<String>();
        List<String> deletedIdentifiers = new ArrayList<String>();
        
        Date harvestStartTime = new Date();
        
        try {
            boolean harvestingNow = harvestingClientConfig.isHarvestingNow();

            if (harvestingNow) {
                harvestErrorOccurred.setValue(true);
                hdLogger.log(Level.SEVERE, "Cannot begin harvesting, Dataverse " + harvestingDataverse.getName() + " is currently being harvested.");

            } else {
                harvestingClientService.resetHarvestInProgress(harvestingClientId);
                harvestingClientService.setHarvestInProgress(harvestingClientId, harvestStartTime);

               
                if (harvestingClientConfig.isOai()) {
                    harvestedDatasetIds = harvestOAI(dataverseRequest, harvestingClientConfig, hdLogger, importCleanupLog, harvestErrorOccurred, failedIdentifiers, deletedIdentifiers, harvestedDatasetIdsThisBatch);

                } else {
                    throw new IOException("Unsupported harvest type");
                }
               harvestingClientService.setHarvestSuccess(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());
               hdLogger.log(Level.INFO, "COMPLETED HARVEST, server=" + harvestingClientConfig.getArchiveUrl() + ", metadataPrefix=" + harvestingClientConfig.getMetadataPrefix());
               hdLogger.log(Level.INFO, "Datasets created/updated: " + harvestedDatasetIds.size() + ", datasets deleted: " + deletedIdentifiers.size() + ", datasets failed: " + failedIdentifiers.size());

                // now index all the datasets we have harvested - created, modified or deleted:
                /* (TODO: may not be needed at all. In Dataverse4, we may be able to get away with the normal 
                    reindexing after every import. See the rest of the comments about batch indexing throughout 
                    this service bean)
                    if (this.processedSizeThisBatch > 0) {
                        hdLogger.log(Level.INFO, "POST HARVEST, reindexing the remaining studies.");
                        if (this.harvestedDatasetIdsThisBatch != null) {
                            hdLogger.log(Level.INFO, this.harvestedDatasetIdsThisBatch.size()+" studies in the batch");
                        }
                        hdLogger.log(Level.INFO, this.processedSizeThisBatch + " bytes of content");
                        indexService.updateIndexList(this.harvestedDatasetIdsThisBatch);
                        hdLogger.log(Level.INFO, "POST HARVEST, calls to index finished.");
                    } else {
                        hdLogger.log(Level.INFO, "(All harvested content already reindexed)");
                    }
                 */
            }
            //mailService.sendHarvestNotification(...getSystemEmail(), harvestingDataverse.getName(), logFileName, logTimestamp, harvestErrorOccurred.booleanValue(), harvestedDatasetIds.size(), failedIdentifiers);
        } catch (Throwable e) {
            harvestErrorOccurred.setValue(true);
            String message = "Exception processing harvest, server= " + harvestingClientConfig.getHarvestingUrl() + ",format=" + harvestingClientConfig.getMetadataPrefix() + " " + e.getClass().getName() + " " + e.getMessage();
            hdLogger.log(Level.SEVERE, message);
            logException(e, hdLogger);
            hdLogger.log(Level.INFO, "HARVEST NOT COMPLETED DUE TO UNEXPECTED ERROR.");
            // TODO: 
            // even though this harvesting run failed, we may have had successfully 
            // processed some number of datasets, by the time the exception was thrown. 
            // We should record that number too. And the number of the datasets that
            // had failed, that we may have counted.  -- L.A. 4.4
            harvestingClientService.setHarvestFailure(harvestingClientId, new Date());

        } finally {
            harvestingClientService.resetHarvestInProgress(harvestingClientId);
            fileHandler.close();
            hdLogger.removeHandler(fileHandler);
            importCleanupLog.close();
        }
    }

    /**
     * 
     * @param harvestingClient  the harvesting client object
     * @param hdLogger          custom logger (specific to this harvesting run)
     * @param harvestErrorOccurred  have we encountered any errors during harvest?
     * @param failedIdentifiers     Study Identifiers for failed "GetRecord" requests
     */
    private List<Long> harvestOAI(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, PrintWriter importCleanupLog, MutableBoolean harvestErrorOccurred, List<String> failedIdentifiers, List<String> deletedIdentifiers, List<Long> harvestedDatasetIdsThisBatch)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {

        logBeginOaiHarvest(hdLogger, harvestingClient);
        
        List<Long> harvestedDatasetIds = new ArrayList<Long>();
        MutableLong processedSizeThisBatch = new MutableLong(0L);
        OaiHandler oaiHandler;

        try {
            oaiHandler = new OaiHandler(harvestingClient);
        } catch (OaiHandlerException ohe) {
            String errorMessage = "Failed to create OaiHandler for harvesting client "
                    +harvestingClient.getName()
                    +"; "
                    +ohe.getMessage();
            hdLogger.log(Level.SEVERE, errorMessage);
            throw new IOException(errorMessage);
        }
                
        try {
            for (Iterator<Header> idIter = oaiHandler.runListIdentifiers(); idIter.hasNext();) {

                Header h = idIter.next();
                String identifier = h.getIdentifier();
                
                hdLogger.info("processing identifier: " + identifier);

                MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);

                // Retrieve and process this record with a separate GetRecord call:
                Long datasetId = processRecord(dataverseRequest, hdLogger, importCleanupLog, oaiHandler, identifier, getRecordErrorOccurred, processedSizeThisBatch, deletedIdentifiers);
                
                hdLogger.info("Total content processed in this batch so far: "+processedSizeThisBatch);
                if (datasetId != null) {
                    harvestedDatasetIds.add(datasetId);
                    
                    if ( harvestedDatasetIdsThisBatch == null ) {
                        harvestedDatasetIdsThisBatch = new ArrayList<Long>();
                    }
                    harvestedDatasetIdsThisBatch.add(datasetId);
                    
                }
                
                if (getRecordErrorOccurred.booleanValue() == true) {
                    failedIdentifiers.add(identifier);
                    harvestErrorOccurred.setValue(true);
                    //temporary:
                    //throw new IOException("Exception occured, stopping harvest");
                }
                
                // reindexing in batches? - this is from DVN 3; 
                // we may not need it anymore. 
                if ( processedSizeThisBatch.longValue() > INDEXING_CONTENT_BATCH_SIZE ) {

                    hdLogger.log(Level.INFO, "REACHED CONTENT BATCH SIZE LIMIT; calling index ("+ harvestedDatasetIdsThisBatch.size()+" datasets in the batch).");
                    //indexService.updateIndexList(this.harvestedDatasetIdsThisBatch);
                    hdLogger.log(Level.INFO, "REINDEX DONE.");


                    processedSizeThisBatch.setValue(0L);
                    harvestedDatasetIdsThisBatch = null;
                }

            }
        } catch (OaiHandlerException e) {
            throw new IOException("Failed to run ListIdentifiers: " + e.getMessage());
        }

        logCompletedOaiHarvest(hdLogger, harvestingClient);

        return harvestedDatasetIds;

    }
    
    
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Long processRecord(DataverseRequest dataverseRequest, Logger hdLogger, PrintWriter importCleanupLog, OaiHandler oaiHandler, String identifier, MutableBoolean recordErrorOccurred, MutableLong processedSizeThisBatch, List<String> deletedIdentifiers) {
        String errMessage = null;
        Dataset harvestedDataset = null;
        logGetRecord(hdLogger, oaiHandler, identifier);
        File tempFile = null;
        
        try {  
            FastGetRecord record = oaiHandler.runGetRecord(identifier);
            errMessage = record.getErrorMessage();

            if (errMessage != null) {
                hdLogger.log(Level.SEVERE, "Error calling GetRecord - " + errMessage);
            } else if (record.isDeleted()) {
                hdLogger.info("Deleting harvesting dataset for "+identifier+", per the OAI server's instructions.");
                
                Dataset dataset = datasetService.getDatasetByHarvestInfo(oaiHandler.getHarvestingClient().getDataverse(), identifier);
                if (dataset != null) {
                    hdLogger.info("Deleting dataset " + dataset.getGlobalIdString());
                    deleteHarvestedDataset(dataset, dataverseRequest, hdLogger);
                    // TODO: 
                    // check the status of that Delete - see if it actually succeeded
                    deletedIdentifiers.add(identifier);
                } else {
                    hdLogger.info("No dataset found for "+identifier+", skipping delete. ");
                }

            } else {
                hdLogger.info("Successfully retrieved GetRecord response.");

                tempFile = record.getMetadataFile();
                PrintWriter cleanupLog;
                harvestedDataset = importService.doImportHarvestedDataset(dataverseRequest, 
                        oaiHandler.getHarvestingClient(),
                        identifier,
                        oaiHandler.getMetadataPrefix(), 
                        record.getMetadataFile(), 
                        importCleanupLog);
                
                hdLogger.fine("Harvest Successful for identifier " + identifier);
                hdLogger.fine("Size of this record: " + record.getMetadataFile().length());
                processedSizeThisBatch.add(record.getMetadataFile().length());
            }
        } catch (Throwable e) {
            logGetRecordException(hdLogger, oaiHandler, identifier, e);
            errMessage = "Caught exception while executing GetRecord on "+identifier;
            //logException(e, hdLogger);
                
        } finally {
            if (tempFile != null) {
                // temporary - let's not delete the temp metadata file if anything went wrong, for now:
                if (errMessage == null) {
                    try{tempFile.delete();}catch(Throwable t){};
                }
            }
        }

        // TODO: the message below is taken from DVN3; - figure out what it means...
        // 
        // If we got an Error from the OAI server or an exception happened during import, then
        // set recordErrorOccurred to true (if recordErrorOccurred is being used)
        // otherwise throw an exception (if recordErrorOccurred is not used, i.e null)
        
        if (errMessage != null) {
            if (recordErrorOccurred  != null) {
                recordErrorOccurred.setValue(true);
            } else {
                throw new EJBException(errMessage);
            }
        }

        return harvestedDataset != null ? harvestedDataset.getId() : null;
    }
    
    private void deleteHarvestedDataset(Dataset dataset, DataverseRequest request, Logger hdLogger) {
        // Purge all the SOLR documents associated with this client from the 
        // index server: 
        indexService.deleteHarvestedDocuments(dataset);
        
        try {
            // files from harvested datasets are removed unceremoniously, 
            // directly in the database. no need to bother calling the 
            // DeleteFileCommand on them.
            for (DataFile harvestedFile : dataset.getFiles()) {
                DataFile merged = em.merge(harvestedFile);
                em.remove(merged);
                harvestedFile = null; 
            }
            dataset.setFiles(null);
            Dataset merged = em.merge(dataset);
            engineService.submit(new DeleteDatasetCommand(request, merged));
        } catch (IllegalCommandException ex) {
            // TODO: log the result
        } catch (PermissionException ex) {
            // TODO: log the result
        } catch (CommandException ex) {
            // TODO: log the result                    
        }
                            
        // TODO: log the success result
    }
    
    
    private void logBeginOaiHarvest(Logger hdLogger, HarvestingClient harvestingClient) {
        hdLogger.log(Level.INFO, "BEGIN HARVEST, oaiUrl=" 
                +harvestingClient.getHarvestingUrl() 
                +",set=" 
                +harvestingClient.getHarvestingSet() 
                +", metadataPrefix=" 
                +harvestingClient.getMetadataPrefix()
                + harvestingClient.getLastNonEmptyHarvestTime() == null ? "" : "from=" + harvestingClient.getLastNonEmptyHarvestTime());
    }
    
    private void logCompletedOaiHarvest(Logger hdLogger, HarvestingClient harvestingClient) {
        hdLogger.log(Level.INFO, "COMPLETED HARVEST, oaiUrl=" 
                +harvestingClient.getHarvestingUrl() 
                +",set=" 
                +harvestingClient.getHarvestingSet() 
                +", metadataPrefix=" 
                +harvestingClient.getMetadataPrefix()
                + harvestingClient.getLastNonEmptyHarvestTime() == null ? "" : "from=" + harvestingClient.getLastNonEmptyHarvestTime());
    }
    
    public void logGetRecord(Logger hdLogger, OaiHandler oaiHandler, String identifier) {
        hdLogger.log(Level.FINE, "Calling GetRecord: oaiUrl =" 
                +oaiHandler.getBaseOaiUrl()
                +"?verb=GetRecord&identifier=" 
                +identifier 
                +"&metadataPrefix=" + oaiHandler.getMetadataPrefix());
    }
    
    public void logGetRecordException(Logger hdLogger, OaiHandler oaiHandler, String identifier, Throwable e) {
        String errMessage = "Exception processing getRecord(), oaiUrl=" 
                +oaiHandler.getBaseOaiUrl() 
                +",identifier=" 
                +identifier 
                +" " 
                +e.getClass().getName() 
                //+" (exception message suppressed)";
                +" " 
                +e.getMessage();
        
            hdLogger.log(Level.SEVERE, errMessage);
            
            // temporary:
            e.printStackTrace();
    }
    
    
    // TODO: I doubt we need a full stacktrace in the harvest log - ??
    // -- L.A. 4.5 May 2016
    private void logException(Throwable e, Logger logger) {

        boolean cause = false;
        String fullMessage = "";
        do {
            String message = e.getClass().getName() + " " + e.getMessage();
            if (cause) {
                message = "\nCaused By Exception.................... " + e.getClass().getName() + " " + e.getMessage();
            }
            StackTraceElement[] ste = e.getStackTrace();
            message += "\nStackTrace: \n";
            for (int m = 0; m < ste.length; m++) {
                message += ste[m].toString() + "\n";
            }
            fullMessage += message;
            cause = true;
        } while ((e = e.getCause()) != null);
        logger.severe(fullMessage);
    }
    
    /*
     some dead code below: 
     this functionality has been moved into OaiHandler. 
    TODO: test that harvesting is still working and remove.
     
    private ServiceProvider getServiceProvider(String baseOaiUrl, Granularity oaiGranularity) {
        Context context = new Context();

        context.withBaseUrl(baseOaiUrl);
        context.withGranularity(oaiGranularity);
        context.withOAIClient(new HttpOAIClient(baseOaiUrl));

        ServiceProvider serviceProvider = new ServiceProvider(context);
        return serviceProvider;
    }
    */
    
    /**
     * Creates an XOAI parameters object for the ListIdentifiers call
     *
     * @param metadataPrefix
     * @param set
     * @param from
     * @return ListIdentifiersParameters
     */
    /*
    private ListIdentifiersParameters buildParams(String metadataPrefix, String set, Date from) {
        ListIdentifiersParameters mip = ListIdentifiersParameters.request();
        mip.withMetadataPrefix(metadataPrefix);

        if (from != null) {
            mip.withFrom(from);
        }

        if (set != null) {
            mip.withSetSpec(set);
        }
        return mip;
    }
    */
    
    
}
