package edu.harvard.iq.dataverse;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.EZIDServiceRequest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;

/**
 *
 * @author skraffmiller
 */
@Stateless
public class DOIEZIdServiceBean extends AbstractGlobalIdServiceBean {

    EZIDService ezidService;
    EZIDServiceRequest ezidServiceRequest;
    String baseURLString = "https://ezid.cdlib.org";
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.index.DOIEZIdServiceBean");

    // get username and password from system properties
    private String USERNAME = "";
    private String PASSWORD = "";

    public DOIEZIdServiceBean() {
        logger.log(Level.FINE,"Constructor");
        baseURLString = System.getProperty("doi.baseurlstring");
        ezidService = new EZIDService(baseURLString);
        USERNAME = System.getProperty("doi.username");
        PASSWORD = System.getProperty("doi.password");
        logger.log(Level.FINE, "Using baseURLString {0}", baseURLString);
        try {
            ezidService.login(USERNAME, PASSWORD);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "login failed ");
            logger.log(Level.WARNING, "Exception String: {0}", e.toString());
            logger.log(Level.WARNING, "localized message: {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause: ", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Other Error on ezidService.login(USERNAME, PASSWORD) - not EZIDException ", e.getMessage());
        }
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyExists(DvObject dvObject) throws Exception {
        if(dvObject==null) {
            logger.severe("Null DvObject sent to alreadyExists().");
            return false;
        }
        return alreadyExists(dvObject.getGlobalId());
    }
    
    @Override
    public boolean alreadyExists(GlobalId pid) throws Exception {
        logger.log(Level.FINE,"alreadyExists");
        try {
            HashMap<String, String> result = ezidService.getMetadata(pid.asString());            
            return result != null && !result.isEmpty();
            // TODO just check for HTTP status code 200/404, sadly the status code is swept under the carpet
        } catch (EZIDException e ){
            //No such identifier is treated as an exception
            //but if that is the case then we want to just return false
            if(pid.getIdentifier() == null){
                return false;
            }
            if (e.getLocalizedMessage().contains("no such identifier")){
                return false;
            }
            logger.log(Level.WARNING, "alreadyExists failed");
            logger.log(Level.WARNING, "getIdentifier(dvObject) {0}", pid.asString());
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.log(Level.FINE,"getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
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
     *
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the
     * identifier system
     * identifier part
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
            metadata = ezidService.getMetadata(identifierOut);
        } catch (EZIDException e) {
            logger.log(Level.FINE, "None existing so we can use this identifier");
            logger.log(Level.FINE, "identifier: {0}", identifierOut);
            return metadata;
        }
        return metadata;
    }

    /**
     * Modifies the EZID metadata for a Dataset
     *
     * @param dvObject the Dataset whose metadata needs to be modified
     * @return the Dataset identifier, or null if the modification failed
     * @throws java.lang.Exception
     */
    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        String identifier = getIdentifier(dvObject);
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            ezidService.setMetadata(identifier,metadata);
            return identifier;
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "modifyMetadata failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            throw e;
        }
    }
        
    @Override
    public void deleteIdentifier(DvObject dvObject) throws Exception {
                logger.log(Level.FINE,"deleteIdentifier");
        String identifier = getIdentifier(dvObject);
        HashMap<String, String> doiMetadata;
        try {
            doiMetadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "get matadata failed cannot delete");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause ", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            return;
        }

        String idStatus = doiMetadata.get("_status");

        if (idStatus.equals("reserved")) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                ezidService.deleteIdentifier(identifier);
            } catch (EZIDException e) {
                logger.log(Level.WARNING, "delete failed");
                logger.log(Level.WARNING, "String {0}", e.toString());
                logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
                logger.log(Level.WARNING, "cause", e.getCause());
                logger.log(Level.WARNING, "message {0}", e.getMessage());
            }
            return;
        }
        if (idStatus.equals("public")) {
            //if public then it has been released set to unavaialble and reset target to n2t url
            updateIdentifierStatus(dvObject, "unavailable | withdrawn by author");
            Map<String, String> metadata = new HashMap<>();
            metadata.put("_target", "http://ezid.cdlib.org/id/" + dvObject.getProtocol() + ":" + dvObject.getAuthority()
                    + "/" + dvObject.getIdentifier());
            try {
                modifyIdentifierTargetURL(dvObject);
                if (dvObject instanceof Dataset) {
                    Dataset dataset = (Dataset) dvObject;
                    for (DataFile df : dataset.getFiles()) {
                        metadata = new HashMap<>();
                        metadata.put("_target", "http://ezid.cdlib.org/id/" + df.getProtocol() + ":" + df.getAuthority()
                                + "/" + df.getIdentifier());
                                        modifyIdentifierTargetURL(df);
                    }
                }

            } catch (Exception e) {
                // TODO already logged, how to react here?
            }
        }
    }
    
    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE,"publicizeIdentifier - dvObject");
        if(!dvObject.isIdentifierRegistered()){
            try {
                createIdentifier(dvObject);
            } catch (Throwable e){
                return false; 
            }
        }
        return updateIdentifierStatus(dvObject, "public");
    }

    private boolean updateIdentifierStatus(DvObject dvObject, String statusIn) {
        logger.log(Level.FINE, "updateIdentifierStatus");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        String objMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        Map<String, String> dcMetadata;
        dcMetadata = new HashMap<>();
        dcMetadata.put("datacite", objMetadata);
        dcMetadata.put("_status", statusIn);
        dcMetadata.put("_target", getTargetUrl(dvObject));

        try {
            // ezID API requires HashMap, not just any map.            
            ezidService.setMetadata(identifier, asHashMap(dcMetadata));
            return true;

        } catch (EZIDException e) {
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
        String providerName = "EZID";
        String providerLink = baseURLString;
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.log(Level.FINE, "createIdentifier");
        if(dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty() ){
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getMetadataForCreateIndicator(dvObject);
        String objMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        Map<String, String> dcMetadata;
        dcMetadata = new HashMap<>();
        dcMetadata.put("datacite", objMetadata);
        dcMetadata.put("datacite.resourcetype", "Dataset");
        dcMetadata.put("_status", "reserved");

        try {
            String retString = ezidService.createIdentifier(identifier, asHashMap(dcMetadata));
            logger.log(Level.FINE, "create DOI identifier retString : {0}", retString);
            return retString;
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "Identifier not created: create failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            logger.log(Level.WARNING, "identifier: ", identifier);
            throw e;
        }
    }
    
     /**
     * Returns a HashMap with the same values as {@code map}. This can be either
     * {@code map} itself, or a new instance with the same values.
     * 
     * This is needed as some of the internal APIs here require HashMap, but we
     * don't want the external APIs to use an implementation class.
     * @param <T>
     * @param map
     * @return A HashMap with the same values as {@code map}
     */
    private <T> HashMap<T,T> asHashMap(Map<T,T> map) {
        return (map instanceof HashMap) ? (HashMap)map : new HashMap<>(map);
    }

}

