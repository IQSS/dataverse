/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteServiceBean {
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.DOIDataCiteServiceBean");


    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    DOIDataCiteRegisterService doiDataCiteRegisterService;
    @EJB
    SystemConfig systemConfig;
    
    private String DOISHOULDER = "";
    
    public DOIDataCiteServiceBean() {
        
    }
    
    public boolean alreadyExists (Dataset dataset){   
        boolean alreadyExists;
        String identifier = getIdentifierFromDataset(dataset);
        try{
            alreadyExists = doiDataCiteRegisterService.testDOIExists(identifier); 
        } catch (Exception e){
            logger.log(Level.INFO, "alreadyExists failed");
            return false;
        }
        return  alreadyExists;
    }

    public String createIdentifier(Dataset dataset) throws Exception {
        String retString = "";
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = getMetadataFromStudyForCreateIndicator(dataset);
        metadata.put("_status", "reserved");       
        try {
            retString = doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
        } catch (Exception e) {
            logger.log(Level.INFO, "Identifier not created: create failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            throw e;
//            return "Identifier not created " + e.getLocalizedMessage();
        }
        return retString;
    }
    
    public HashMap getIdentifierMetadata(Dataset dataset) {
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = new HashMap();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.INFO, "getIdentifierMetadata failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            return metadata;
        }
        return metadata;
    }

    public HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier) {
        String identifierOut = getIdentifierForLookup(protocol, authority, separator, identifier);
        HashMap metadata = new HashMap();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifierOut);
        } catch (Exception e) {
            logger.log(Level.INFO, "None existing so we can use this identifier");
            logger.log(Level.INFO, "identifier: {0}", identifierOut);
            return metadata;
        }
        return metadata;
    }

    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        return protocol + ":" + authority + separator + identifier;
    }

    public String modifyIdentifier(Dataset dataset, HashMap metadata) throws Exception {
        String identifier = getIdentifierFromDataset(dataset);
        try {
            doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
        } catch (Exception e) {
            logger.log(Level.INFO, "modifyMetadata failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            throw e;
        }
        return identifier;
    }
    
    public void deleteRecordFromCache(Dataset datasetIn){
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap doiMetadata = new HashMap();
        try {
            doiMetadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.INFO, "get matadata failed cannot delete");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
        }

        String idStatus = (String) doiMetadata.get("_status");

        if (idStatus == null || idStatus.equals("reserved")) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                doiDataCiteRegisterService.deleteIdentifier(identifier);
            } catch (Exception e) {
                logger.log(Level.INFO, "delete failed");
                logger.log(Level.INFO, "String " + e.toString());
                logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
                logger.log(Level.INFO, "cause " + e.getCause());
                logger.log(Level.INFO, "message " + e.getMessage());
                throw new RuntimeException(e);
            }
            return;
        }
        
    }
    

    public void deleteIdentifier(Dataset datasetIn) throws Exception {
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap doiMetadata = new HashMap();
        try {
            doiMetadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.INFO, "get matadata failed cannot delete");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
        }

        String idStatus = (String) doiMetadata.get("_status");

        if (idStatus != null && idStatus.equals("reserved")) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                doiDataCiteRegisterService.deleteIdentifier(identifier);
            } catch (Exception e) {
                logger.log(Level.INFO, "delete failed");
                logger.log(Level.INFO, "String " + e.toString());
                logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
                logger.log(Level.INFO, "cause " + e.getCause());
                logger.log(Level.INFO, "message " + e.getMessage());
            }
            return;
        }
        if (idStatus != null && idStatus.equals("public")) {
            //if public then it has been released set to unavailable and reset target to n2t url
            updateIdentifierStatus(datasetIn, "unavailable");
        }
    }

    private HashMap getUpdateMetadataFromDataset(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<String, String>();

        String authorString = datasetIn.getLatestVersion().getAuthorsStr();

        if (authorString.isEmpty()) {
            authorString = ":unav";
        }

        String producerString = dataverseService.findRootDataverse().getName() + " Dataverse";

        if (producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", datasetIn.getLatestVersion().getTitle());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", generateYear());

        return metadata;
    }

    private HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<String, String>();

        String authorString = datasetIn.getLatestVersion().getAuthorsStr();

        if (authorString.isEmpty()) {
            authorString = ":unav";
        }

        String producerString = dataverseService.findRootDataverse().getName() + " Dataverse";

        if (producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", datasetIn.getLatestVersion().getTitle());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", generateYear());
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }
    
    public HashMap getMetadataFromDatasetForTargetURL(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<>();           
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }
    
    private String getTargetUrl(Dataset datasetIn){
           return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalId();      
    }

    private String getIdentifierFromDataset(Dataset dataset) {
        return dataset.getGlobalId();
    }

    public void publicizeIdentifier(Dataset dataset) throws Exception {
        updateIdentifierStatus(dataset, "public");
    }

    private void updateIdentifierStatus(Dataset dataset, String statusIn) throws Exception  {
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = getUpdateMetadataFromDataset(dataset);
        metadata.put("_target", getTargetUrl(dataset));
        metadata.put("_status", statusIn);
        try {
            doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
        } catch (Exception e) {
            logger.log(Level.INFO, "modifyMetadata failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            throw  e;
        }
    }

    public static String generateYear() {
        StringBuilder guid = new StringBuilder();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));

        return guid.toString();
    }

    public static String generateTimeString() {
        StringBuilder guid = new StringBuilder();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));
        guid.append(calendar.get(Calendar.DAY_OF_YEAR));
        guid.append(calendar.get(Calendar.HOUR_OF_DAY));
        guid.append(calendar.get(Calendar.MINUTE));
        guid.append(calendar.get(Calendar.SECOND));
        guid.append(calendar.get(Calendar.MILLISECOND));
        double random = Math.random();
        guid.append(random);

        return guid.toString();
    }
}