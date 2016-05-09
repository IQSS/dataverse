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
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.net.URLEncoder;
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
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.xml.sax.SAXException;

import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.serviceprovider.ServiceProvider;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.client.HttpOAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

/**
 *
 * @author Leonid Andreev
 */
@Stateless(name = "harvesterService")
@Named
@ManagedBean
public class HarvesterServiceBean {
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

    /*
     This method is implemented in the DataverseTimerServiceBean; 
    TODO: make sure that implementation does everything we need. 
    -- L.A. 4.4, May 08 2016.
    private void createHarvestTimer(Dataverse harvestingDataverse) {
        HarvestingClient harvestingDataverseConfig = harvestingDataverse.getHarvestingClientConfig();
        
        if (harvestingDataverseConfig == null) {
            logger.info("ERROR: No Harvesting Configuration found for dataverse id="+harvestingDataverse.getId());
            return;
        }
        
        if (harvestingDataverseConfig.isScheduled()) {
            long intervalDuration = 0;
            Calendar initExpiration = Calendar.getInstance();
            initExpiration.set(Calendar.MINUTE, 0);
            initExpiration.set(Calendar.SECOND, 0);
            if (harvestingDataverseConfig.getSchedulePeriod().equals(harvestingDataverseConfig.SCHEDULE_PERIOD_DAILY)) {
                intervalDuration = 1000 * 60 * 60 * 24;
                initExpiration.set(Calendar.HOUR_OF_DAY, harvestingDataverseConfig.getScheduleHourOfDay());

            } else if (harvestingDataverseConfig.getSchedulePeriod().equals(harvestingDataverseConfig.SCHEDULE_PERIOD_WEEKLY)) {
                intervalDuration = 1000 * 60 * 60 * 24 * 7;
                initExpiration.set(Calendar.HOUR_OF_DAY, harvestingDataverseConfig.getScheduleHourOfDay());
                initExpiration.set(Calendar.DAY_OF_WEEK, harvestingDataverseConfig.getScheduleDayOfWeek());

            } else {
                logger.log(Level.WARNING, "Could not set timer for dataverse id, " + harvestingDataverse.getId() + ", unknown schedule period: " + harvestingDataverseConfig.getSchedulePeriod());
                return;
            }
            Date initExpirationDate = initExpiration.getTime();
            Date currTime = new Date();
            if (initExpirationDate.before(currTime)) {
                initExpirationDate.setTime(initExpiration.getTimeInMillis() + intervalDuration);
            }
            logger.log(Level.INFO, "Setting timer for dataverse " + harvestingDataverse.getName() + ", initial expiration: " + initExpirationDate);
            dataverseTimerService.createTimer(initExpirationDate, intervalDuration, new HarvestTimerInfo(harvestingDataverse.getId(), harvestingDataverse.getName(), harvestingDataverseConfig.getSchedulePeriod(), harvestingDataverseConfig.getScheduleHourOfDay(), harvestingDataverseConfig.getScheduleDayOfWeek()));
        }
    }
    */

    /**
     * Run a harvest for an individual harvesting Dataverse
     * @param dataverseId
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
        String logFileName = "../logs" + File.separator + "harvest_" + harvestingClientConfig.getName() + logTimestamp + ".log";
        FileHandler fileHandler = new FileHandler(logFileName);
        hdLogger.addHandler(fileHandler);
        List<Long> harvestedDatasetIds = null;

        List<Long> harvestedDatasetIdsThisBatch = new ArrayList<Long>();

        List<String> failedIdentifiers = new ArrayList<String>();
        Date harvestStartTime = new Date();
        
        try {
            boolean harvestingNow = harvestingClientConfig.isHarvestingNow();

            if (harvestingNow) {
                harvestErrorOccurred.setValue(true);
                hdLogger.log(Level.SEVERE, "Cannot begin harvesting, Dataverse " + harvestingDataverse.getName() + " is currently being harvested.");

            } else {
                harvestingClientService.resetHarvestInProgress(harvestingDataverse.getId());
                harvestingClientService.setHarvestInProgress(harvestingDataverse.getId(), harvestStartTime);

               
                if (harvestingClientConfig.isOai()) {
                    harvestedDatasetIds = harvestOAI(dataverseRequest, harvestingClientConfig, hdLogger, harvestErrorOccurred, failedIdentifiers, harvestedDatasetIdsThisBatch);

                } else {
                    throw new IOException("Unsupported harvest type");
                }
                harvestingClientService.setHarvestSuccess(harvestingDataverse.getId(), new Date(), harvestedDatasetIds.size(), failedIdentifiers.size());
                hdLogger.log(Level.INFO, "COMPLETED HARVEST, server=" + harvestingClientConfig.getArchiveUrl() + ", metadataPrefix=" + harvestingClientConfig.getMetadataPrefix());
                hdLogger.log(Level.INFO, "Datasets created/updated: " + harvestedDatasetIds.size() + ", datasets deleted: [TODO:], datasets failed: " + failedIdentifiers.size());

                // now index all the datasets we have harvested - created, modified or deleted:
                /* (TODO: !!!)
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
            String message = "Exception processing harvest, server= " + harvestingClientConfig.getArchiveUrl() + ",format=" + harvestingClientConfig.getMetadataPrefix() + " " + e.getClass().getName() + " " + e.getMessage();
            hdLogger.log(Level.SEVERE, message);
            logException(e, hdLogger);
            hdLogger.log(Level.INFO, "HARVEST NOT COMPLETED DUE TO UNEXPECTED ERROR.");
            // TODO: 
            // even though this harvesting run failed, we may have had successfully 
            // processed some number of datasets, by the time the exception was thrown. 
            // We should record that number too. And the number of the datasets that
            // had failed, that we may have counted.  -- L.A. 4.4
            harvestingClientService.setHarvestFailure(harvestingDataverse.getId(), new Date());

        } finally {
            harvestingClientService.resetHarvestInProgress(harvestingDataverse.getId());
            fileHandler.close();
            hdLogger.removeHandler(fileHandler);
        }
    }

    /**
     * 
     * @param harvestingClient  the harvesting client object
     * @param hdLogger          custom logger (specific to this harvesting run)
     * @param harvestErrorOccurred  have we encountered any errors during harvest?
     * @param failedIdentifiers     Study Identifiers for failed "GetRecord" requests
     */
    private List<Long> harvestOAI(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, MutableBoolean harvestErrorOccurred, List<String> failedIdentifiers, List<Long> harvestedDatasetIdsThisBatch)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {

        List<Long> harvestedDatasetIds = new ArrayList<Long>();
        Long processedSizeThisBatch = 0L;


        String baseOaiUrl = harvestingClient.getHarvestingUrl();
        String metadataPrefix = harvestingClient.getMetadataPrefix();
        Date fromDate = harvestingClient.getLastNonEmptyHarvestTime();

        String set = harvestingClient.getHarvestingSet() == null ? null : URLEncoder.encode(harvestingClient.getHarvestingSet(), "UTF-8");

        hdLogger.log(Level.INFO, "BEGIN HARVEST..., oaiUrl=" + baseOaiUrl + ",set=" + set + ", metadataPrefix=" + metadataPrefix + ", from=" + fromDate);
        
        ListIdentifiersParameters parameters = buildParams(metadataPrefix, set, fromDate);
        ServiceProvider serviceProvider = getServiceProvider(baseOaiUrl, Granularity.Second);

        try {
            for (Iterator<Header> idIter = serviceProvider.listIdentifiers(parameters); idIter.hasNext();) {

                Header h = idIter.next();
                String identifier = h.getIdentifier();
                hdLogger.fine("identifier: " + identifier);

                // Retrieve and process this record with a separate GetRecord call:
                MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);
                Long datasetId = getRecord(dataverseRequest, hdLogger, harvestingClient, identifier, metadataPrefix, getRecordErrorOccurred, processedSizeThisBatch);
                if (datasetId != null) {
                    harvestedDatasetIds.add(datasetId);
                }
                if (getRecordErrorOccurred.booleanValue() == true) {
                    failedIdentifiers.add(identifier);
                }
                
                if ( harvestedDatasetIdsThisBatch == null ) {
                    harvestedDatasetIdsThisBatch = new ArrayList<Long>();
                }
                harvestedDatasetIdsThisBatch.add(datasetId);

                // reindexing in batches? - this is from DVN 3; 
                // we may not need it anymore. 
                if ( processedSizeThisBatch > INDEXING_CONTENT_BATCH_SIZE ) {

                    hdLogger.log(Level.INFO, "REACHED CONTENT BATCH SIZE LIMIT; calling index ("+ harvestedDatasetIdsThisBatch.size()+" datasets in the batch).");
                    //indexService.updateIndexList(this.harvestedDatasetIdsThisBatch);
                    hdLogger.log(Level.INFO, "REINDEX DONE.");


                    processedSizeThisBatch = 0L;
                    harvestedDatasetIdsThisBatch = null;
                }

            }
        } catch (BadArgumentException e) {
            throw new IOException("Incorrectly formatted OAI parameter", e);
        }

        hdLogger.log(Level.INFO, "COMPLETED HARVEST, oaiUrl=" + baseOaiUrl + ",set=" + set + ", metadataPrefix=" + metadataPrefix + ", from=" + fromDate);

        return harvestedDatasetIds;

    }
    
    private ServiceProvider getServiceProvider(String baseOaiUrl, Granularity oaiGranularity) {
        Context context = new Context();

        context.withBaseUrl(baseOaiUrl);
        context.withGranularity(oaiGranularity);
        context.withOAIClient(new HttpOAIClient(baseOaiUrl));

        ServiceProvider serviceProvider = new ServiceProvider(context);
        return serviceProvider;
    }
    
    /**
     * Creates an XOAI parameters object for the ListIdentifiers call
     *
     * @param metadataPrefix
     * @param set
     * @param from
     * @return ListIdentifiersParameters
     */
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

    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Long getRecord(DataverseRequest dataverseRequest, Logger hdLogger, HarvestingClient harvestingClient, String identifier, String metadataPrefix, MutableBoolean recordErrorOccurred, Long processedSizeThisBatch) {
        String errMessage = null;
        Dataset harvestedDataset = null;
        String oaiUrl = harvestingClient.getHarvestingUrl();
        Dataverse parentDataverse = harvestingClient.getDataverse();
        
        try {
            hdLogger.log(Level.INFO, "Calling GetRecord: oaiUrl =" + oaiUrl + "?verb=GetRecord&identifier=" + identifier + "&metadataPrefix=" + metadataPrefix);

            FastGetRecord record = new FastGetRecord(oaiUrl, identifier, metadataPrefix);
            errMessage = record.getErrorMessage();

            if (errMessage != null) {
                hdLogger.log(Level.SEVERE, "Error calling GetRecord - " + errMessage);
            } else if (record.isDeleted()) {
                hdLogger.log(Level.INFO, "Received 'deleted' status from OAI Server.");
                Dataset dataset = null; //TODO: !!! datasetService.getDatasetByHarvestInfo(dataverse, identifier);
                if (dataset != null) {
                    hdLogger.log(Level.INFO, "Deleting study " + dataset.getGlobalId());
                    // TODO: !!! datasetService.deleteDataset(dataset.getId());
                } else {
                    hdLogger.log(Level.INFO, "No study found for this record, skipping delete. ");
                }

            } else {
                hdLogger.log(Level.INFO, "Successfully retrieved GetRecord response.");

                harvestedDataset = importService.doImportHarvestedDataset(dataverseRequest, parentDataverse, metadataPrefix, record.getMetadataFile(), null);
                
                hdLogger.log(Level.INFO, "Harvest Successful for identifier " + identifier);
                
                processedSizeThisBatch += record.getMetadataFile().length();
            }
        } catch (Throwable e) {
            errMessage = "Exception processing getRecord(), oaiUrl=" + oaiUrl + ",identifier=" + identifier + " " + e.getClass().getName() + " " + e.getMessage();
            hdLogger.log(Level.SEVERE, errMessage);
            logException(e, hdLogger);
                
        }

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
