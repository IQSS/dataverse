/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteServiceBean extends AbstractIdServiceBean {

    private static final Logger logger = Logger.getLogger(DOIDataCiteServiceBean.class.getCanonicalName());

    @EJB
    DOIDataCiteRegisterService doiDataCiteRegisterService;

    public DOIDataCiteServiceBean() {
    }

    @Override
    public boolean registerWhenPublished() {
        return true;
    }

    @Override
    public boolean alreadyExists(Dataset dataset) {
        logger.log(Level.FINE,"alreadyExists");
        boolean alreadyExists;
        String identifier = getIdentifierFromDataset(dataset);
        try{
            alreadyExists = doiDataCiteRegisterService.testDOIExists(identifier); 
        } catch (Exception e){
            logger.log(Level.WARNING, "alreadyExists failed");
            return false;
        }
        return  alreadyExists;
    }

    @Override
    public String createIdentifier(Dataset dataset) throws Exception {
        logger.log(Level.FINE,"createIdentifier");
        String identifier = getIdentifierFromDataset(dataset);
        Map<String, String> metadata = getMetadataFromStudyForCreateIndicator(dataset);
        metadata.put("_status", "reserved");
        try {
            String retString = doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
            logger.log(Level.FINE, "create DOI identifier retString : {0}", retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier not created: create failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(Dataset dataset) {
        logger.log(Level.FINE,"getIdentifierMetadata");
        String identifier = getIdentifierFromDataset(dataset);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            return metadata;
        }
        return metadata;
    }

    /**
     * Looks up the metadata for a Global Identifier
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param separator the string that separates authority from local identifier part
     * @param identifier the local identifier part
     * @return a Map of metadata. It is empty when the lookup failed, e.g. when
     * the identifier does not exist.
     */
    @Override
    public Map<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier) {
        logger.log(Level.FINE,"lookupMetadataFromIdentifier");
        String identifierOut = getIdentifierForLookup(protocol, authority, separator, identifier);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifierOut);
        } catch (Exception e) {
            logger.log(Level.WARNING, "None existing so we can use this identifier");
            logger.log(Level.WARNING, "identifier: {0}", identifierOut);
            return metadata;
        }
        return metadata;
    }


    /**
     * Modifies the EZID metadata for a Dataset
     * @param dataset the Dataset whose metadata needs to be modified
     * @param metadata the new metadata for the Dataset
     * @return the Dataset identifier, or null if the modification failed
     * @throws java.lang.Exception
     */
    @Override
    public String modifyIdentifier(Dataset dataset, Map<String, String> metadata) throws Exception {
        logger.log(Level.FINE,"modifyIdentifier");
        String identifier = getIdentifierFromDataset(dataset);
        try {
            doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed");
            logger.log(Level.WARNING, "String " + e.toString());
            logger.log(Level.WARNING, "localized message " + e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause " + e.getCause());
            logger.log(Level.WARNING, "message " + e.getMessage());
            throw e;
        }
        return identifier;
    }
    
    public void deleteRecordFromCache(Dataset datasetIn){
        logger.log(Level.FINE,"deleteRecordFromCache");
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap doiMetadata = new HashMap();
        try {
            doiMetadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "get matadata failed cannot delete");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
        }

        String idStatus = (String) doiMetadata.get("_status");

        if (idStatus == null || idStatus.equals("reserved")) {
            logger.log(Level.WARNING, "Delete status is reserved..");
            try {
                doiDataCiteRegisterService.deleteIdentifier(identifier);
            } catch (Exception e) {
                logger.log(Level.WARNING, "delete failed");
                logger.log(Level.WARNING, "String {0}", e.toString());
                logger.log(Level.WARNING, "localized message {0}",  e.getLocalizedMessage());
                logger.log(Level.WARNING, "cause", e.getCause());
                logger.log(Level.WARNING, "message {0}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void deleteIdentifier(Dataset datasetIn) throws Exception {
        logger.log(Level.FINE,"deleteIdentifier");
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap<String, String> doiMetadata = new HashMap<>();
        try {
            doiMetadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "get matadata failed cannot delete");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause ", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
        }

        String idStatus = doiMetadata.get("_status");

        if (idStatus != null && idStatus.equals("reserved")) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                doiDataCiteRegisterService.deleteIdentifier(identifier);
            } catch (Exception e) {
                logger.log(Level.WARNING, "delete failed");
                logger.log(Level.WARNING, "String {0}", e.toString());
                logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
                logger.log(Level.WARNING, "cause", e.getCause());
                logger.log(Level.WARNING, "message {0}", e.getMessage());
            }
            return;
        }
        if (idStatus != null && idStatus.equals("public")) {
            //if public then it has been released set to unavailable and reset target to n2t url
            updateIdentifierStatus(datasetIn, "unavailable");
        }
    }

    @Override
    protected Map<String, String> getUpdateMetadataFromDataset(Dataset datasetIn) {
        logger.log(Level.FINE,"getUpdateMetadataFromDataset");
        Map<String, String> metadata = super.getUpdateMetadataFromDataset(datasetIn);
        metadata.put("datacite.publicationyear", generateYear(datasetIn));
        return metadata;
    }

    @Override
    public Map<String, String> getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        logger.log(Level.FINE,"getMetadataFromStudyForCreateIndicator");
        Map<String, String> metadata = super.getMetadataFromStudyForCreateIndicator(datasetIn);
        metadata.put("datacite.resourcetype", "Dataset");
        return metadata;
    }

    @Override
    public boolean publicizeIdentifier(Dataset dataset) {
        logger.log(Level.FINE,"publicizeIdentifier");
        return updateIdentifierStatus(dataset, "public");
    }

    private boolean updateIdentifierStatus(Dataset dataset, String statusIn) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        String identifier = getIdentifierFromDataset(dataset);
        Map<String, String> metadata = getUpdateMetadataFromDataset(dataset);
        metadata.put("_status", statusIn);
        metadata.put("_target", getTargetUrl(dataset));
        try {
            doiDataCiteRegisterService.createIdentifier(identifier, metadata, dataset);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getProviderInformation(){
        ArrayList <String> providerInfo = new ArrayList<>();
        String providerName = "DataCite";
        String providerLink = "http://status.datacite.org";
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }
}
