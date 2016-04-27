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
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean");
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    public static final String HARVEST_RESULT_SUCCESS="success";
    public static final String HARVEST_RESULT_FAILED="failed";

   
    private long processedSizeThisBatch = 0;
    private List<Long> harvestedDatasetIdsThisBatch = null;
    public HarvesterServiceBean() {

    }
    
    /**
     * Called to run an "On Demand" harvest.  
     */
    @Asynchronous
    public void doAsyncHarvest(Dataverse harvestingDataverse) {
        
        try {
            doHarvest(harvestingDataverse.getId());
        } catch (Exception e) {
            logger.info("Caught exception running an asynchronous harvest (dataverse \""+harvestingDataverse.getAlias()+"\")");
        }
    }

    public void createScheduledHarvestTimers() {
        logger.log(Level.INFO, "HarvesterService: going to (re)create Scheduled harvest timers.");
        dataverseTimerService.removeHarvestTimers();

        List dataverses = dataverseService.getAllHarvestedDataverses();
        for (Iterator it = dataverses.iterator(); it.hasNext();) {
            Dataverse dataverse = (Dataverse) it.next();
            HarvestingClient harvestingConfig = dataverse.getHarvestingClientConfig();
            if (harvestingConfig == null) {
                logger.warning("ERROR: no harvesting config found for dataverse id="+dataverse.getId());
            } else if (harvestingConfig.isScheduled()) {
                createHarvestTimer(dataverse);
            }
        }
    }

    public void removeHarvestTimer(Dataverse dataverse) {
        dataverseTimerService.removeHarvestTimer(dataverse);
    }

    public void updateHarvestTimer(Dataverse harvestedDataverse) {
        removeHarvestTimer(harvestedDataverse);
        createHarvestTimer(harvestedDataverse);
    }
    
    public List<HarvestTimerInfo> getHarvestTimers() {
        ArrayList timers = new ArrayList<HarvestTimerInfo>();
        // Clear dataverse timer, if one exists 
        for (Iterator it = timerService.getTimers().iterator(); it.hasNext();) {
            Timer timer = (Timer) it.next();
            if (timer.getInfo() instanceof HarvestTimerInfo) {
                HarvestTimerInfo info = (HarvestTimerInfo) timer.getInfo();
                timers.add(info);
            }
        }    
        return timers;
    }

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

    /**
     * Run a harvest for an individual harvesting Dataverse
     * @param dataverseId
     */
    public void doHarvest(Long dataverseId) throws IOException {
        Dataverse harvestingDataverse = dataverseService.find(dataverseId);
        
        if (harvestingDataverse == null) {
            throw new IOException("No such Dataverse: id="+dataverseId);
        }
        
        HarvestingClient harvestingClientConfig = harvestingDataverse.getHarvestingClientConfig();
        
        if (harvestingClientConfig == null) {
            throw new IOException("Could not find Harvesting Config for Dataverse id="+dataverseId);
        }
        
        MutableBoolean harvestErrorOccurred = new MutableBoolean(false);
        String logTimestamp = logFormatter.format(new Date());
        Logger hdLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean." + harvestingDataverse.getAlias() + logTimestamp);
        String logFileName = /* TODO: !!!! FileUtil.getImportFileDir() +*/ File.separator + "harvest_" + harvestingDataverse.getAlias() + logTimestamp + ".log";
        FileHandler fileHandler = new FileHandler(logFileName);
        hdLogger.addHandler(fileHandler);
        List<Long> harvestedDatasetIds = null;

    	this.processedSizeThisBatch = 0;
        this.harvestedDatasetIdsThisBatch = new ArrayList<Long>();

        List<String> failedIdentifiers = new ArrayList<String>();
        Date harvestStartTime = new Date();
        
        try {
            boolean harvestingNow = harvestingClientConfig.isHarvestingNow();

            if (harvestingNow) {
                harvestErrorOccurred.setValue(true);
                hdLogger.log(Level.SEVERE, "Cannot begin harvesting, Dataverse " + harvestingDataverse.getName() + " is currently being harvested.");

            } else {
                harvestingClientService.resetHarvestInProgress(harvestingDataverse.getId());
                String until = null;  // If we don't set until date, we will get all the changes since the last harvest.
                String from = null;
                // TODO: should it be last *non-empty* time? -- L.A. 4.4
                Date lastSuccessfulHarvestTime = harvestingClientConfig.getLastSuccessfulHarvestTime();
                if (lastSuccessfulHarvestTime != null) {
                    from = formatter.format(lastSuccessfulHarvestTime);
                }
                harvestingClientService.setHarvestInProgress(harvestingDataverse.getId(), harvestStartTime);

                hdLogger.log(Level.INFO, "BEGIN HARVEST..., oaiUrl=" + harvestingClientConfig.getArchiveUrl() + ",set=" + harvestingClientConfig.getHarvestingSet() + ", metadataPrefix=" + harvestingClientConfig.getMetadataPrefix() + ", from=" + from + ", until=" + until);

                if (harvestingClientConfig.isOai()) {
                    harvestedDatasetIds = harvestOAI(harvestingDataverse, hdLogger, from, until, harvestErrorOccurred, failedIdentifiers);

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
     * @param dataverse  the dataverse to harvest into
     * @param from       get updated studies from this beginning date
     * @param until      get updated studies until this end date
     * @param harvestErrorOccurred  have we encountered any errors during harvest?
     * @param failedIdentifiers     Study Identifiers for failed "GetRecord" requests
     */
    private List<Long> harvestOAI(Dataverse dataverse, Logger hdLogger, String from, String until, MutableBoolean harvestErrorOccurred, List<String> failedIdentifiers)
            throws IOException, ParserConfigurationException,SAXException, TransformerException {
   
        List<Long> harvestedDatasetIds = new ArrayList<Long>();
   
        /*
            ResumptionTokenType resumptionToken = null;

            do {
                //resumptionToken = harvesterService.harvestFromIdentifiers(hdLogger, resumptionToken, dataverse, from, until, harvestedDatasetIds, failedIdentifiers, harvestErrorOccurred
                resumptionToken = harvestFromIdentifiers(hdLogger, resumptionToken, dataverse, from, until, harvestedDatasetIds, failedIdentifiers, harvestErrorOccurred);
            } while (resumptionToken != null && !resumptionToken.equals(""));

            hdLogger.log(Level.INFO, "COMPLETED HARVEST, oaiUrl=" + dataverse.getServerUrl() + ",set=" + dataverse.getHarvestingSet() + ", metadataPrefix=" + dataverse.getHarvestFormatType().getMetadataPrefix() + ", from=" + from + ", until=" + until);
           
        */
        return harvestedDatasetIds;
     
    }

    /*
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResumptionTokenType harvestFromIdentifiers(Logger hdLogger, ResumptionTokenType resumptionToken, HarvestingDataverse dataverse, String from, String until, List<Long> harvestedDatasetIds, List<String> failedIdentifiers, MutableBoolean harvestErrorOccurred)
            throws java.io.IOException, ParserConfigurationException, SAXException, TransformerException, JAXBException {
        String encodedSet = dataverse.getHarvestingSet() == null ? null : URLEncoder.encode(dataverse.getHarvestingSet(), "UTF-8");
        ListIdentifiers listIdentifiers = null;

        if (resumptionToken == null) {
            listIdentifiers = new ListIdentifiers(dataverse.getServerUrl(),
                    from,
                    until,
                    encodedSet,
                    URLEncoder.encode(dataverse.getHarvestFormatType().getMetadataPrefix(), "UTF-8"));
        } else {
            hdLogger.log(Level.INFO, "harvestFromIdentifiers(), resumptionToken=" + resumptionToken.getValue());
            listIdentifiers = new ListIdentifiers(dataverse.getServerUrl(), resumptionToken.getValue());
        }
        
        Document doc = listIdentifiers.getDocument();

        //       JAXBContext jc = JAXBContext.newInstance("edu.harvard.hmdc.vdcnet.jaxb.oai");
        //       Unmarshaller unmarshaller = jc.createUnmarshaller();
        JAXBElement unmarshalObj = (JAXBElement) unmarshaller.unmarshal(doc);
        OAIPMHtype oaiObj = (OAIPMHtype) unmarshalObj.getValue();

        if (oaiObj.getError() != null && oaiObj.getError().size() > 0) {
            if (oaiObj.getError().get(0).getCode().equals(OAIPMHerrorcodeType.NO_RECORDS_MATCH)) {
                 hdLogger.info("ListIdentifiers returned NO_RECORDS_MATCH - no studies found to be harvested.");
            } else {
                handleOAIError(hdLogger, oaiObj, "calling listIdentifiers, oaiServer= " + dataverse.getServerUrl() + ",from=" + from + ",until=" + until + ",encodedSet=" + encodedSet + ",format=" + dataverse.getHarvestFormatType().getMetadataPrefix());
                throw new EJBException("Received OAI Error response calling ListIdentifiers");
            }
        } else {
            ListIdentifiersType listIdentifiersType = oaiObj.getListIdentifiers();
            if (listIdentifiersType != null) {
                resumptionToken = listIdentifiersType.getResumptionToken();
                for (Iterator it = listIdentifiersType.getHeader().iterator(); it.hasNext();) {
                    HeaderType header = (HeaderType) it.next();
                    MutableBoolean getRecordErrorOccurred = new MutableBoolean(false);
                    Long studyId = getRecord(hdLogger, dataverse, header.getIdentifier(), dataverse.getHarvestFormatType().getMetadataPrefix(), getRecordErrorOccurred);
                    if (studyId != null) {
                        harvestedDatasetIds.add(studyId);
                    }
                    if (getRecordErrorOccurred.booleanValue()==true) {
                        failedIdentifiers.add(header.getIdentifier());
                    }
                    
                }

            }
        }
        String logMsg = "Returning from harvestFromIdentifiers";

        if (resumptionToken == null) {
            logMsg += " resumptionToken is null";
        } else if (!StringUtil.isEmpty(resumptionToken.getValue())) {
            logMsg += " resumptionToken is " + resumptionToken.getValue();
        } else {
            // Some OAIServers return an empty resumptionToken element when all
            // the identifiers have been sent, so need to check  for this, and 
            // treat it as if resumptiontoken is null.
            logMsg += " resumptionToken is empty, setting return value to null.";
            resumptionToken = null;
        }
        hdLogger.info(logMsg);
        return resumptionToken;
    }
    */

    /*
    private void handleOAIError(Logger hdLogger, OAIPMHtype oaiObj, String message) {
        for (Iterator it = oaiObj.getError().iterator(); it.hasNext();) {
            OAIPMHerrorType error = (OAIPMHerrorType) it.next();
            message += ", error code: " + error.getCode();
            message += ", error value: " + error.getValue();
            hdLogger.log(Level.SEVERE, message);

        }
    }
    */

    /*
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Long getRecord(HarvestingDataverse dataverse, String identifier, String metadataPrefix) {
        return getRecord(logger, dataverse, identifier, metadataPrefix, null);
    }
    */
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Long getRecord(Logger hdLogger, Dataverse dataverse, String identifier, String metadataPrefix, MutableBoolean recordErrorOccurred) {
        String errMessage = null;

        HarvestingClient harvestingConfig = dataverse.getHarvestingClientConfig();
        
        if (harvestingConfig == null) {
            errMessage = "Could not find Harvesting Config for Dataverse id="+dataverse.getId();
            hdLogger.log(Level.SEVERE, errMessage);
            return null;
        }
        
        Dataset harvestedDataset = null;
        String oaiUrl = harvestingConfig.getHarvestingUrl();
        try {
            hdLogger.log(Level.INFO, "Calling GetRecord: oaiUrl =" + oaiUrl + "?verb=GetRecord&identifier=" + identifier + "&metadataPrefix=" + metadataPrefix);

            FastGetRecord record = new FastGetRecord(oaiUrl, identifier, metadataPrefix);
            errMessage = record.getErrorMessage();
            //errMessage=null;

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
                hdLogger.log(Level.INFO, "Successfully retreived GetRecord response.");


                harvestedDataset = null; // TODO: !!! import
                hdLogger.log(Level.INFO, "Harvest Successful for identifier " + identifier);

        		this.processedSizeThisBatch += record.getMetadataFile().length();
                if ( this.harvestedDatasetIdsThisBatch == null ) {
                    this.harvestedDatasetIdsThisBatch = new ArrayList<Long>();
                }
                this.harvestedDatasetIdsThisBatch.add(harvestedDataset.getId());

                // reindexing in batches? - this is from DVN 3; 
                // we may not need it anymore. 
                if ( this.processedSizeThisBatch > 10000000 ) {

                    hdLogger.log(Level.INFO, "REACHED CONTENT BATCH SIZE LIMIT; calling index ("+this.harvestedDatasetIdsThisBatch.size()+" studies in the batch).");
                    //indexService.updateIndexList(this.harvestedDatasetIdsThisBatch);
                    hdLogger.log(Level.INFO, "REINDEX DONE.");


                    this.processedSizeThisBatch = 0;
                    this.harvestedDatasetIdsThisBatch = null;
                }
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
    
    
    /*
    public List<String> getMetadataFormats(String oaiUrl) {
        JAXBElement unmarshalObj;
        try {

            Document doc = new ListMetadataFormats(oaiUrl).getDocument();
            JAXBContext jc = JAXBContext.newInstance("edu.harvard.hmdc.vdcnet.jaxb.oai");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshalObj = (JAXBElement) unmarshaller.unmarshal(doc);
        } catch (TransformerException ex) {
            throw new EJBException(ex);
        } catch (ParserConfigurationException ex) {
            throw new EJBException(ex);
        } catch (JAXBException ex) {
            throw new EJBException(ex);
        } catch (SAXException ex) {
            throw new EJBException(ex);
        } catch (IOException ex) {
            throw new EJBException(ex);
        }

        OAIPMHtype OAIObj = (OAIPMHtype) unmarshalObj.getValue();
       if (OAIObj.getError()!=null && OAIObj.getError().size()>0) {
            List<OAIPMHerrorType> errList = OAIObj.getError();
            String errMessage="";
            for (OAIPMHerrorType error : OAIObj.getError()){
                 errMessage += error.getCode()+ " " +error.getValue(); 
            }
            throw new EJBException(errMessage);
        }
        ListMetadataFormatsType listMetadataFormats = OAIObj.getListMetadataFormats();
        List<String> formats = null;
        if (listMetadataFormats != null) {
            formats = new ArrayList<String>();
            for (Iterator it = listMetadataFormats.getMetadataFormat().iterator(); it.hasNext();) {
                //  Object elem = it.next();
                MetadataFormatType elem = (MetadataFormatType) it.next();
                formats.add(elem.getMetadataPrefix());
            }
        }
        return formats;
    }
    */

    /**
     *
     *  SetDetailBean returned rather than the ListSetsType because we get strange errors when trying
     *  to refer to JAXB generated classes in both Web and EJB tiers.
     */
    /*
    public List<SetDetailBean> getSets(String oaiUrl) {
        JAXBElement unmarshalObj = null;

        try {
            ListSets listSets = new ListSets(oaiUrl);
            int nodeListLength = listSets.getErrors().getLength();
            if (nodeListLength==1) {
                 System.out.println("err Node: "+ listSets.getErrors().item(0));
            }
           
            
            Document doc = new ListSets(oaiUrl).getDocument();
            JAXBContext jc = JAXBContext.newInstance("edu.harvard.hmdc.vdcnet.jaxb.oai");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshalObj = (JAXBElement) unmarshaller.unmarshal(doc);
        } catch (ParserConfigurationException ex) {
            throw new EJBException(ex);
        } catch (SAXException ex) {
            throw new EJBException(ex);
        } catch (TransformerException ex) {
            throw new EJBException(ex);
        } catch (IOException ex) {
            throw new EJBException(ex);
        } catch (JAXBException ex) {
            throw new EJBException(ex);
        }
        List<SetDetailBean> sets = null;
        Object value = unmarshalObj.getValue();

        Package valPackage = value.getClass().getPackage();
        if (value instanceof edu.harvard.hmdc.vdcnet.jaxb.oai.OAIPMHtype) {
            OAIPMHtype OAIObj = (OAIPMHtype) value;
            if (OAIObj.getError()!=null && OAIObj.getError().size()>0 ) {
                List<OAIPMHerrorType> errList = OAIObj.getError();
                String errMessage="";
                for (OAIPMHerrorType error : OAIObj.getError()){
                     // NO_SET_HIERARCHY is not an error from the perspective of the DVN,
                     // it just means that the OAI server doesn't support sets.
                     if (!error.getCode().equals(OAIPMHerrorcodeType.NO_SET_HIERARCHY)) {
                        errMessage += error.getCode()+ " " +error.getValue(); 
                     }
                }
                if (errMessage!="")  {
                     throw new EJBException(errMessage);
                }
               
            }
         
            ListSetsType listSetsType = OAIObj.getListSets();
            if (listSetsType != null) {
                sets = new ArrayList<SetDetailBean>();
                for (Iterator it = listSetsType.getSet().iterator(); it.hasNext();) {
                    SetType elem = (SetType) it.next();
                    SetDetailBean setDetail = new SetDetailBean();
                    setDetail.setName(elem.getSetName());
                    setDetail.setSpec(elem.getSetSpec());
                    sets.add(setDetail);
                }
            }
        }
        return sets;
    }
    */
    
    
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
        Most likely not needed any more: 
    public List<HarvestFormatType> findAllHarvestFormatTypes() {
        String queryStr = "SELECT f FROM HarvestFormatType f";
        Query query = em.createQuery(queryStr);
        return query.getResultList();
    }    
    
    public HarvestFormatType findHarvestFormatTypeByMetadataPrefix(String metadataPrefix) {
        String queryStr = "SELECT f FROM HarvestFormatType f WHERE f.metadataPrefix = '" + metadataPrefix + "'";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        HarvestFormatType hft = null;
        if (resultList.size() > 1) {
            throw new EJBException("More than one HarvestFormatType found with metadata Prefix= '" + metadataPrefix + "'");
        }
        if (resultList.size() == 1) {
            hft = (HarvestFormatType) resultList.get(0);
        }
        return hft;
    }
*/
    
    
}
