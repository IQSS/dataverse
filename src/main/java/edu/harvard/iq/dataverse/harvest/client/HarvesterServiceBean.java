/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import static java.net.HttpURLConnection.HTTP_OK;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.xml.sax.SAXException;

import io.gdcc.xoai.model.oaipmh.results.record.Header;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    public static final String HARVEST_RESULT_SUCCESS="success";
    public static final String HARVEST_RESULT_FAILED="failed";
    public static final String DATAVERSE_PROPRIETARY_METADATA_FORMAT="dataverse_json";
    public static final String DATAVERSE_PROPRIETARY_METADATA_API="/api/datasets/export?exporter="+DATAVERSE_PROPRIETARY_METADATA_FORMAT+"&persistentId=";

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
     * @param harvestingClient  the harvesting client object
     * @param hdLogger          custom logger (specific to this harvesting run)
     * @param harvestErrorOccurred  have we encountered any errors during harvest?
     * @param failedIdentifiers     Study Identifiers for failed "GetRecord" requests
     */
    private List<Long> harvestOAI(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, PrintWriter importCleanupLog, MutableBoolean harvestErrorOccurred, List<String> failedIdentifiers, List<String> deletedIdentifiers, List<Long> harvestedDatasetIdsThisBatch)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {

        logBeginOaiHarvest(hdLogger, harvestingClient);
        
        List<Long> harvestedDatasetIds = new ArrayList<Long>();
        OaiHandler oaiHandler;
        HttpClient httpClient = null; 

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

        if (DATAVERSE_PROPRIETARY_METADATA_FORMAT.equals(oaiHandler.getMetadataPrefix())) {
            // If we are harvesting native Dataverse json, we'll also need this 
            // jdk http client to make direct calls to the remote Dataverse API:
            httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        }
        
        try {
            for (Iterator<Header> idIter = oaiHandler.runListIdentifiers(); idIter.hasNext();) {

                Header h = idIter.next();
                String identifier = h.getIdentifier();
                Date dateStamp = Date.from(h.getDatestamp());
                
                hdLogger.info("processing identifier: " + identifier + ", date: " + dateStamp);
                
                if (h.isDeleted()) {
                    hdLogger.info("Deleting harvesting dataset for " + identifier + ", per ListIdentifiers.");

                    deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, hdLogger);
                    continue;
                }

                MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);

                // Retrieve and process this record with a separate GetRecord call:
                
                Long datasetId = processRecord(dataverseRequest, hdLogger, importCleanupLog, oaiHandler, identifier, getRecordErrorOccurred, deletedIdentifiers, dateStamp, httpClient);
                
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
            }
        } catch (OaiHandlerException e) {
            throw new IOException("Failed to run ListIdentifiers: " + e.getMessage());
        }

        logCompletedOaiHarvest(hdLogger, harvestingClient);

        return harvestedDatasetIds;

    }    
    
    private Long processRecord(DataverseRequest dataverseRequest, Logger hdLogger, PrintWriter importCleanupLog, OaiHandler oaiHandler, String identifier, MutableBoolean recordErrorOccurred, List<String> deletedIdentifiers, Date dateStamp, HttpClient httpClient) {
        String errMessage = null;
        Dataset harvestedDataset = null;
        logGetRecord(hdLogger, oaiHandler, identifier);
        File tempFile = null;
        
        try {
            boolean deleted = false;
            
            if (DATAVERSE_PROPRIETARY_METADATA_FORMAT.equals(oaiHandler.getMetadataPrefix())) {
                // Make direct call to obtain the proprietary Dataverse metadata
                // in JSON from the remote Dataverse server:
                String metadataApiUrl = oaiHandler.getProprietaryDataverseMetadataURL(identifier);
                logger.info("calling "+metadataApiUrl);
                tempFile = retrieveProprietaryDataverseMetadata(httpClient, metadataApiUrl);
                
            } else {
                FastGetRecord record = oaiHandler.runGetRecord(identifier);
                errMessage = record.getErrorMessage();
                deleted = record.isDeleted();
                tempFile = record.getMetadataFile();
            }

            if (errMessage != null) {
                hdLogger.log(Level.SEVERE, "Error calling GetRecord - " + errMessage);
                
            } else if (deleted) {
                hdLogger.info("Deleting harvesting dataset for "+identifier+", per GetRecord.");
                
                deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, hdLogger); 
            } else {
                hdLogger.info("Successfully retrieved GetRecord response.");

                PrintWriter cleanupLog;
                harvestedDataset = importService.doImportHarvestedDataset(dataverseRequest, 
                        oaiHandler.getHarvestingClient(),
                        identifier,
                        oaiHandler.getMetadataPrefix(), 
                        tempFile,
                        dateStamp,
                        importCleanupLog);
                
                hdLogger.fine("Harvest Successful for identifier " + identifier);
                hdLogger.fine("Size of this record: " + tempFile.length());
            }
        } catch (Throwable e) {
            logGetRecordException(hdLogger, oaiHandler, identifier, e);
            errMessage = "Caught exception while executing GetRecord on "+identifier;
                
        } finally {
            if (tempFile != null) {
                // temporary - let's not delete the temp metadata file if anything went wrong, for now:
                if (errMessage == null) {
                    try{tempFile.delete();}catch(Throwable t){};
                }
            }
        }

        // If we got an Error from the OAI server or an exception happened during import, then
        // set recordErrorOccurred to true (if recordErrorOccurred is being used)
        // otherwise throw an exception (if recordErrorOccurred is not used, i.e null)
        
        if (errMessage != null) {
            if (recordErrorOccurred != null) {
                recordErrorOccurred.setValue(true);
            } else {
                throw new EJBException(errMessage);
            }
        }

        return harvestedDataset != null ? harvestedDataset.getId() : null;
    }
    
    File retrieveProprietaryDataverseMetadata (HttpClient client, String remoteApiUrl) throws IOException {
        
        if (client == null) {
            throw new IOException("Null Http Client, cannot make a call to obtain native metadata.");
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteApiUrl))
                .GET()
                .header("User-Agent", "DataverseHarvester/6.0")
                .build();
        
        HttpResponse<InputStream> response;

        try {            
             response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Failed to connect to the remote dataverse server to obtain native metadata");
        }
            
        int responseCode = response.statusCode();
            
        if (responseCode == HTTP_OK) {
            File tempMetadataFile = File.createTempFile("meta", ".tmp");

            try (InputStream inputStream = response.body();
                    FileOutputStream outputStream = new FileOutputStream(tempMetadataFile);) {
                inputStream.transferTo(outputStream);
                return tempMetadataFile;
            }
        }
            
        throw new IOException("Failed to download native metadata from the remote dataverse server.");
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
        
}
