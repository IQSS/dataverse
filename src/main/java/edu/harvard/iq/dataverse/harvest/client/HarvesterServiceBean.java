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
import javax.inject.Named;
//import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableLong;
import org.xml.sax.SAXException;

import com.lyncode.xoai.model.oaipmh.Header;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
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
                ClientHarvestRun currentRun = harvestingClientService.setHarvestInProgress(harvestingClientId, harvestStartTime);
                
                if (harvestingClientConfig.isOai()) {
                    harvestedDatasetIds = harvestOAI(dataverseRequest, harvestingClientConfig, currentRun.getId(), hdLogger, importCleanupLog, harvestErrorOccurred, failedIdentifiers, deletedIdentifiers);

                } else {
                    throw new IOException("Unsupported harvest type");
                }
                
                harvestingClientService.setHarvestSuccess(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());
                hdLogger.log(Level.INFO, "COMPLETED HARVEST, server=" + harvestingClientConfig.getArchiveUrl() + ", metadataPrefix=" + harvestingClientConfig.getMetadataPrefix());
                hdLogger.log(Level.INFO, "Datasets created/updated: " + harvestedDatasetIds.size() + ", datasets deleted: " + deletedIdentifiers.size() + ", datasets failed: " + failedIdentifiers.size());

            }
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
     * @param dataverseRequest  DataverseRequest object that will be used for imports
     * @param harvestingClient  the harvesting client object
     * @param thisJobId         the numeric id of this ongoing harvesting job
     * @param hdLogger          custom logger (specific to this harvesting run)
     * @param importCleanupLog  PrintWriter for the Cleanup Log
     * @param harvestErrorOccurred  have we encountered any errors during harvest?
     * @param failedIdentifiers     Identifiers that we failed to harvest
     * @param deletedIdentifiers    Identifiers that the Server tagged as "deleted"
     * @return List of database ids of the successfully imported datasets 
     */
    private List<Long> harvestOAI(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Long thisJobId, Logger hdLogger, PrintWriter importCleanupLog, MutableBoolean harvestErrorOccurred, List<String> failedIdentifiers, List<String> deletedIdentifiers)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {

        logBeginOaiHarvest(hdLogger, harvestingClient);
        
        List<Long> harvestedDatasetIds = new ArrayList<>();
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

                // Before each iteration, check if this harvesting job needs to be aborted:
                if (false) { // (harvestingClientService.checkIfStoppingJob(thisJobId) {
                    
                }
                Header h = idIter.next();
                String identifier = h.getIdentifier();
                Date dateStamp = h.getDatestamp();
                
                hdLogger.info("processing identifier: " + identifier + ", date: " + dateStamp);
                
                if (h.isDeleted()) {
                    hdLogger.info("Deleting harvesting dataset for " + identifier + ", per ListIdentifiers.");

                    deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, hdLogger);
                    continue;
                }

                MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);

                // Retrieve and process this record with a separate GetRecord call:
                Long datasetId = processRecord(dataverseRequest, hdLogger, importCleanupLog, oaiHandler, identifier, getRecordErrorOccurred, deletedIdentifiers, dateStamp);
                
                if (datasetId != null) {
                    harvestedDatasetIds.add(datasetId);
                }
                
                if (getRecordErrorOccurred.booleanValue()) {
                    failedIdentifiers.add(identifier);
                    harvestErrorOccurred.setValue(true);
                    //temporary:
                    //throw new IOException("Exception occured, stopping harvest");
                }
            }
        } catch (OaiHandlerException e) {
            throw new IOException("Failed to run ListIdentifiers: " + e.getMessage());
        }

        logCompletedOaiHarvest(hdLogger, harvestingClient);

        return harvestedDatasetIds;

    }
    
    
    
    private Long processRecord(DataverseRequest dataverseRequest, Logger hdLogger, PrintWriter importCleanupLog, OaiHandler oaiHandler, String identifier, MutableBoolean recordErrorOccurred, List<String> deletedIdentifiers, Date dateStamp) {
        Dataset harvestedDataset = null;
        logGetRecord(hdLogger, oaiHandler, identifier);
        File tempFile = null;
        
        try {  
            FastGetRecord record = oaiHandler.runGetRecord(identifier);
            String errorMessage = record.getErrorMessage();

            if (errorMessage != null) {
                hdLogger.log(Level.SEVERE, "Error calling GetRecord - " + errorMessage);
                recordErrorOccurred.setValue(true);
            } else if (record.isDeleted()) {
                hdLogger.info("Deleting harvesting dataset for "+identifier+", per the OAI server's instructions.");
                
                deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, hdLogger);
            } else {
                hdLogger.info("Successfully retrieved GetRecord response.");

                tempFile = record.getMetadataFile();
                PrintWriter cleanupLog;
                harvestedDataset = importService.doImportHarvestedDataset(dataverseRequest, 
                        oaiHandler.getHarvestingClient(),
                        identifier,
                        oaiHandler.getMetadataPrefix(), 
                        record.getMetadataFile(),
                        dateStamp,
                        importCleanupLog);
                
                hdLogger.fine("Harvest Successful for identifier " + identifier);
                hdLogger.fine("Size of this record: " + record.getMetadataFile().length());
            }
        } catch (Throwable e) {
            logGetRecordException(hdLogger, oaiHandler, identifier, e);
            recordErrorOccurred.setValue(true);   
        } finally {
            if (tempFile != null) {
                try{tempFile.delete();}catch(Throwable t){};
            }
        }

        return harvestedDataset != null ? harvestedDataset.getId() : null;
    }
    
    private void deleteHarvestedDatasetIfExists(String persistentIdentifier, Dataverse harvestingDataverse, DataverseRequest dataverseRequest, List<String> deletedIdentifiers, Logger hdLogger) {
        Dataset dataset = datasetService.getDatasetByHarvestInfo(harvestingDataverse, persistentIdentifier);
        if (dataset != null) {
            datasetService.deleteHarvestedDataset(dataset, dataverseRequest, hdLogger);
            // TODO: 
            // check the status of that Delete - see if it actually succeeded
            deletedIdentifiers.add(persistentIdentifier);
            return;
        }
        hdLogger.info("No dataset found for " + persistentIdentifier + ", skipping delete. ");
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
                +", identifier="
                +identifier 
                +", "
                +e.getClass().getName() 
                //+" (exception message suppressed)";
                +", "
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
