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
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;
import jakarta.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.solr.client.solrj.SolrServerException;
import org.xml.sax.SAXException;

import io.gdcc.xoai.model.oaipmh.results.Record;
import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import static edu.harvard.iq.dataverse.harvest.client.FastGetRecord.XML_XMLNS_XSI_ATTRIBUTE_TAG;
import static edu.harvard.iq.dataverse.harvest.client.FastGetRecord.XML_XMLNS_XSI_ATTRIBUTE;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    jakarta.ejb.TimerService timerService;
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
     * Run a harvest for an individual harvesting client
     * @param dataverseRequest
     * @param harvestingClientId
     * @throws IOException
     */
    public void doHarvest(DataverseRequest dataverseRequest, Long harvestingClientId) throws IOException {
        HarvestingClient harvestingClientConfig = harvestingClientService.find(harvestingClientId);
        
        if (harvestingClientConfig == null) {
            throw new IOException("No such harvesting client: id="+harvestingClientId);
        }
                
        String logTimestamp = logFormatter.format(new Date());
        Logger hdLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean." + harvestingClientConfig.getName() + logTimestamp);
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "harvest_" + harvestingClientConfig.getName() + "_" + logTimestamp + ".log";
        FileHandler fileHandler = new FileHandler(logFileName);
        hdLogger.setUseParentHandlers(false);
        hdLogger.addHandler(fileHandler);
        
        PrintWriter importCleanupLog = new PrintWriter(new FileWriter(System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs/harvest_cleanup_" + harvestingClientConfig.getName() + "_" + logTimestamp + ".txt"));
        
        
        List<Long> harvestedDatasetIds = new ArrayList<>();
        List<String> failedIdentifiers = new ArrayList<>();
        List<String> deletedIdentifiers = new ArrayList<>();
        
        Date harvestStartTime = new Date();
        
        try {
            if (harvestingClientConfig.isHarvestingNow()) {
                hdLogger.log(Level.SEVERE, String.format("Cannot start harvest, client %s is already harvesting.", harvestingClientConfig.getName()));

            } else {
                harvestingClientService.resetHarvestInProgress(harvestingClientId);
                harvestingClientService.setHarvestInProgress(harvestingClientId, harvestStartTime);

               
                if (harvestingClientConfig.isOai()) {
                    harvestOAI(dataverseRequest, harvestingClientConfig, hdLogger, importCleanupLog, failedIdentifiers, deletedIdentifiers, harvestedDatasetIds);

                } else {
                    throw new IOException("Unsupported harvest type");
                }

                if (failedIdentifiers.isEmpty()) {
                    harvestingClientService.setHarvestCompleted(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());
                    hdLogger.log(Level.INFO, String.format("\"COMPLETED HARVEST, server=%s, metadataPrefix=%s", harvestingClientConfig.getArchiveUrl(), harvestingClientConfig.getMetadataPrefix()));
                } else {
                    harvestingClientService.setHarvestCompletedWithFailures(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());
                    hdLogger.log(Level.INFO, String.format("\"COMPLETED HARVEST WITH FAILURES, server=%s, metadataPrefix=%s", harvestingClientConfig.getArchiveUrl(), harvestingClientConfig.getMetadataPrefix()));
                }

                hdLogger.log(Level.INFO, String.format("Datasets created/updated: %s, datasets deleted: %s, datasets failed: %s", harvestedDatasetIds.size(), deletedIdentifiers.size(), failedIdentifiers.size()));

                // Reindex dataverse to update datasetCount
                try {
                    indexService.indexDataverse(harvestingClientConfig.getDataverse());
                } catch (IOException | SolrServerException e) {
                    hdLogger.log(Level.SEVERE, "Dataverse indexing failed. You can kickoff a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + harvestingClientConfig.getDataverse().getId().toString());
                }
            }
        } catch (StopHarvestException she) {
            hdLogger.log(Level.INFO, "HARVEST INTERRUPTED BY EXTERNAL REQUEST");
            harvestingClientService.setPartiallyCompleted(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());
        } catch (Throwable e) {
            // Any other exception should be treated as a complete failure
            String message = "Exception processing harvest, server= " + harvestingClientConfig.getHarvestingUrl() + ",format=" + harvestingClientConfig.getMetadataPrefix() + " " + e.getClass().getName() + " " + e.getMessage();
            hdLogger.log(Level.SEVERE, message);
            logException(e, hdLogger);
            hdLogger.log(Level.INFO, "HARVEST NOT COMPLETED DUE TO UNEXPECTED ERROR.");

            harvestingClientService.setHarvestFailure(harvestingClientId, new Date(), harvestedDatasetIds.size(), failedIdentifiers.size(), deletedIdentifiers.size());

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
    private void harvestOAI(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, PrintWriter importCleanupLog, List<String> failedIdentifiers, List<String> deletedIdentifiers, List<Long> harvestedDatasetIds)
            throws IOException, ParserConfigurationException, SAXException, TransformerException, StopHarvestException {

        logBeginOaiHarvest(hdLogger, harvestingClient);
        
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

        // We will use this jdk http client to make direct calls to the remote 
        // OAI (or remote Dataverse API) to obtain the metadata records 
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        
        try {
            if (harvestingClient.isUseListRecords()) {
                harvestOAIviaListRecords(oaiHandler, dataverseRequest, harvestingClient, httpClient, failedIdentifiers, deletedIdentifiers, harvestedDatasetIds, hdLogger, importCleanupLog);
            } else {
                // The default behavior is to use ListIdentifiers:
                harvestOAIviaListIdentifiers(oaiHandler, dataverseRequest, harvestingClient, httpClient, failedIdentifiers, deletedIdentifiers, harvestedDatasetIds, hdLogger, importCleanupLog);
            }
        } catch (OaiHandlerException e) {
            throw new IOException("Failed to run ListIdentifiers: " + e.getMessage());
        }

        logCompletedOaiHarvest(hdLogger, harvestingClient);

    }  
    
    private void harvestOAIviaListIdentifiers(OaiHandler oaiHandler, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, HttpClient httpClient, List<String> failedIdentifiers, List<String> deletedIdentifiers, List<Long> harvestedDatasetIds, Logger harvesterLogger, PrintWriter importCleanupLog) throws OaiHandlerException, StopHarvestException {
        for (Iterator<Header> idIter = oaiHandler.runListIdentifiers(); idIter.hasNext();) {
            // Before each iteration, check if this harvesting job needs to be aborted:
            if (checkIfStoppingJob(harvestingClient)) {
                throw new StopHarvestException("Harvesting stopped by external request");
            }

            Header h = idIter.next();
            String identifier = h.getIdentifier();
            Date dateStamp = Date.from(h.getDatestamp());

            harvesterLogger.info("ListIdentifiers; processing identifier: " + identifier + ", date: " + dateStamp);

            if (h.isDeleted()) {
                harvesterLogger.info("ListIdentifiers; deleting harvesting dataset for " + identifier);

                deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, harvesterLogger);
                continue;
            }

            MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);

            // Retrieve and process this record with a separate GetRecord call:
            Long datasetId = processRecord(dataverseRequest, harvesterLogger, importCleanupLog, oaiHandler, identifier, getRecordErrorOccurred, deletedIdentifiers, dateStamp, httpClient);

            if (datasetId != null) {
                harvestedDatasetIds.add(datasetId);
            }

            if (getRecordErrorOccurred.booleanValue() == true) {
                failedIdentifiers.add(identifier);
                //can be uncommented out for testing failure handling:
                //throw new IOException("Exception occured, stopping harvest");
            }
        }
    }
    
    private void harvestOAIviaListRecords(OaiHandler oaiHandler, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, HttpClient httpClient, List<String> failedIdentifiers, List<String> deletedIdentifiers, List<Long> harvestedDatasetIds, Logger harvesterLogger, PrintWriter importCleanupLog) throws OaiHandlerException, StopHarvestException {
        for (Iterator<Record> idIter = oaiHandler.runListRecords(); idIter.hasNext();) {
            // Before each iteration, check if this harvesting job needs to be aborted:
            if (checkIfStoppingJob(harvestingClient)) {
                throw new StopHarvestException("Harvesting stopped by external request");
            }

            Record oaiRecord = idIter.next();
                        
            Header h = oaiRecord.getHeader();
            String identifier = h.getIdentifier();
            Date dateStamp = Date.from(h.getDatestamp());

            harvesterLogger.info("ListRecords; processing identifier : " + identifier + ", date: " + dateStamp);

            if (h.isDeleted()) {
                harvesterLogger.info("ListRecords; Deleting harvested dataset for " + identifier);

                deleteHarvestedDatasetIfExists(identifier, oaiHandler.getHarvestingClient().getDataverse(), dataverseRequest, deletedIdentifiers, harvesterLogger);
                continue;
            }

            MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);
            
            Metadata oaiMetadata = oaiRecord.getMetadata();
            String metadataString = oaiMetadata.getMetadataAsString();

            Long datasetId = null; 
            
            if (metadataString != null) {
                Dataset harvestedDataset = null;
                
                // Some xml header sanitation: 
                if (!metadataString.matches("^<[^>]*" + XML_XMLNS_XSI_ATTRIBUTE_TAG + ".*")) {
                    metadataString = metadataString.replaceFirst(">", XML_XMLNS_XSI_ATTRIBUTE);
                }

                try {
                    harvestedDataset = importService.doImportHarvestedDataset(dataverseRequest,
                            oaiHandler.getHarvestingClient(),
                            identifier,
                            oaiHandler.getMetadataPrefix(),
                            metadataString,
                            dateStamp,
                            importCleanupLog);

                    harvesterLogger.fine("Harvest Successful for identifier " + identifier);
                    harvesterLogger.fine("Size of this record: " + metadataString.length());
                } catch (Throwable e) {
                    logGetRecordException(harvesterLogger, oaiHandler, identifier, e);
                }
                if (harvestedDataset != null) {
                    datasetId = harvestedDataset.getId();
                }
            } else {
                // Instead of giving up here, let's try to retrieve and process 
                // this record with a separate GetRecord call:
                datasetId = processRecord(dataverseRequest, harvesterLogger, importCleanupLog, oaiHandler, identifier, getRecordErrorOccurred, deletedIdentifiers, dateStamp, httpClient);
            }

            if (datasetId != null) {
                harvestedDatasetIds.add(datasetId);
            }

            if (getRecordErrorOccurred.booleanValue() == true) {
                failedIdentifiers.add(identifier);
                //can be uncommented out for testing failure handling:
                //throw new IOException("Exception occured, stopping harvest");
            }
        }
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
                logger.fine("calling "+metadataApiUrl);
                tempFile = retrieveProprietaryDataverseMetadata(httpClient, metadataApiUrl);
                
            } else {
                FastGetRecord record = oaiHandler.runGetRecord(identifier, httpClient);
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
                .header("User-Agent", "XOAI Service Provider v5 (Dataverse)")
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
    
    private boolean checkIfStoppingJob(HarvestingClient harvestingClient) {
        Long pid = ProcessHandle.current().pid();
        String stopFileName = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "stopharvest_" + harvestingClient.getName() + "." + pid; 
        Path stopFilePath = Paths.get(stopFileName);
        
        if (Files.exists(stopFilePath)) {
            // Now that we know that the file is there, let's (try to) delete it,
            // so that the harvest can be re-run. 
            try {
                Files.delete(stopFilePath);
            } catch (IOException ioex) {
                // No need to treat this is a big deal (could be a permission, etc.)
                logger.warning("Failed to delete the flag file "+stopFileName + "; check permissions and delete manually.");
            }
            return true;
        }
        
        return false;                 
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
