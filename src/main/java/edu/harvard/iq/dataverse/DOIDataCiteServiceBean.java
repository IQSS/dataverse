/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteServiceBean implements IdServiceBean {
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.DOIDataCiteServiceBean");


    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    DOIDataCiteRegisterService doiDataCiteRegisterService;
    @EJB
    SystemConfig systemConfig;
    
    public DOIDataCiteServiceBean() {
        
    }

    @Override
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

    @Override
    public String createIdentifier(Dataset dataset) throws Exception {
        String retString;
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

    @Override
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

    @Override
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

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        return protocol + ":" + authority + separator + identifier;
    }

    @Override
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
        }
    }


    @Override
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
        metadata.put("datacite.publicationyear", IdServiceBean.generateYear());

        return metadata;
    }

    @Override
    public HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
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
        metadata.put("datacite.publicationyear", IdServiceBean.generateYear());
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }

    @Override
    public HashMap getMetadataFromDatasetForTargetURL(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<>();           
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }
    
    private String getTargetUrl(Dataset datasetIn){
           return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalId();      
    }

    @Override
    public String getIdentifierFromDataset(Dataset dataset) {
        return dataset.getGlobalId();
    }

    @Override
    public boolean publicizeIdentifier(Dataset dataset) {
        return updateIdentifierStatus(dataset, "public");
    }

    private boolean updateIdentifierStatus(Dataset dataset, String statusIn) {
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = getUpdateMetadataFromDataset(dataset);
        metadata.put("_target", getTargetUrl(dataset));
        metadata.put("_status", statusIn);
        try {
            doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
            return true;
        } catch (Exception e) {
            logger.log(Level.INFO, "modifyMetadata failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            return false;
        }
    }
}