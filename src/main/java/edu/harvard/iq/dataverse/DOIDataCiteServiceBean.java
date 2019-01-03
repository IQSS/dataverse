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
public class DOIDataCiteServiceBean extends AbstractGlobalIdServiceBean {

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
    public boolean alreadyExists(DvObject dvObject) {
        if(dvObject==null) {
            logger.severe("Null DvObject sent to alreadyExists().");
            return false;
        }
        return alreadyExists(dvObject.getGlobalId());
    }

    @Override
    public boolean alreadyExists(GlobalId pid) {
        logger.log(Level.FINE,"alreadyExists");
        if(pid==null) {
            logger.severe("No identifier sent.");
            return false;
        }
        boolean alreadyExists;
        String identifier = pid.asString();
        try{
            alreadyExists = doiDataCiteRegisterService.testDOIExists(identifier); 
        } catch (Exception e){
            logger.log(Level.WARNING, "alreadyExists failed");
            return false;
        }
        return  alreadyExists;
    }
    

    @Override
    public String createIdentifier(DvObject dvObject) throws Exception {
        logger.log(Level.FINE,"createIdentifier");
        if(dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty() ){
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getMetadataForCreateIndicator(dvObject);
        metadata.put("_status", "reserved");
        try {
            String retString = doiDataCiteRegisterService.createIdentifierLocal(identifier, metadata, dvObject);
            logger.log(Level.FINE, "create DOI identifier retString : " + retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier not created: create failed", e);
            throw e;
        }
    }

    @Override
    public HashMap getIdentifierMetadata(DvObject dvObject) {
        logger.log(Level.FINE,"getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed", e);
        }
        return metadata;
    }
    

    /**
     * Looks up the metadata for a Global Identifier
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param identifier the local identifier part
     * @return a Map of metadata. It is empty when the lookup failed, e.g. when
     * the identifier does not exist.
     */
    @Override
    public HashMap<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier) {
        logger.log(Level.FINE,"lookupMetadataFromIdentifier");
        String identifierOut = getIdentifierForLookup(protocol, authority, identifier);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifierOut);
        } catch (Exception e) {
            logger.log(Level.WARNING, "None existing so we can use this identifier");
            logger.log(Level.WARNING, "identifier: {0}", identifierOut);
        }
        return metadata;
    }


    /**
     * Modifies the DOI metadata for a Dataset
     * @param dvObject the dvObject whose metadata needs to be modified
     * @return the Dataset identifier, or null if the modification failed
     * @throws java.lang.Exception
     */
    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        logger.log(Level.FINE,"modifyIdentifier");
        String identifier = getIdentifier(dvObject);
        try {
           HashMap<String, String> metadata = doiDataCiteRegisterService.getMetadata(identifier);
            doiDataCiteRegisterService.modifyIdentifier(identifier, metadata, dvObject);
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed", e);
            throw e;
        }
        return identifier;
    }
    
    public void deleteRecordFromCache(Dataset datasetIn){
        logger.log(Level.FINE,"deleteRecordFromCache");
        String identifier = getIdentifier(datasetIn);
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
    public void deleteIdentifier(DvObject dvObject) throws Exception {
        logger.log(Level.FINE,"deleteIdentifier");
        String identifier = getIdentifier(dvObject);
        HashMap<String, String> doiMetadata = new HashMap<>();
        try {
            doiMetadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "deleteIdentifier: get matadata failed. " + e.getMessage(), e);
        }

        String idStatus = doiMetadata.get("_status");

        if ( idStatus != null ) {
            switch ( idStatus ) {
                case "reserved":
                    logger.log(Level.INFO, "Delete status is reserved..");
                    try {
                        doiDataCiteRegisterService.deleteIdentifier(identifier);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "delete failed: " + e.getMessage(), e);
                    }
                    break;
                       
                case "public":
                    //if public then it has been released set to unavailable and reset target to n2t url
                    updateIdentifierStatus(dvObject, "unavailable");
                    break;
            }
            return;
        }
        if (idStatus != null && idStatus.equals("public")) {
            //if public then it has been released set to unavailable and reset target to n2t url
            doiDataCiteRegisterService.deactivateIdentifier(identifier, doiMetadata, dvObject);
        }
    }

    private boolean updateIdentifierStatus(DvObject dvObject, String statusIn) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        metadata.put("_status", statusIn);
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            doiDataCiteRegisterService.registerIdentifier(identifier, metadata, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        if(dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty() ){
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        metadata.put("_status", "public");
        metadata.put("datacite.publicationyear", generateYear(dvObject));
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            doiDataCiteRegisterService.registerIdentifier(identifier, metadata, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
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
