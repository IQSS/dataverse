package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.Stateless;

/**
 *
 * @author skraffmiller
 */
@Stateless
public class DOIEZIdServiceBean extends DOIServiceBean {
    
    private static final Logger logger = Logger.getLogger(DOIEZIdServiceBean.class.getCanonicalName());
    
    EZIDService ezidService;
    
    // This has a sane default in microprofile-config.properties
    private final String baseUrl = JvmSettings.EZID_API_URL.lookup();
    
    public DOIEZIdServiceBean() {
        // Creating the service doesn't do any harm, just initializing some object data here.
        // Makes sure we don't run into NPEs from the other methods, but will obviously fail if the
        // login below does not work.
        this.ezidService = new EZIDService(this.baseUrl);
        
        try {
            // These have (obviously) no default, but still are optional to make the provider optional
            String username = JvmSettings.EZID_USERNAME.lookupOptional().orElse(null);
            String password = JvmSettings.EZID_PASSWORD.lookupOptional().orElse(null);
            
            if (username != null ^ password != null) {
                logger.log(Level.WARNING, "You must give both username and password. Will not try to login.");
            }
            
            if (username != null && password != null) {
                this.ezidService.login(username, password);
                this.configured = true;
            }
        } catch (EZIDException e) {
            // We only do the warnings here, but the object still needs to be created.
            // The EJB stateless thing expects this to go through, and it is requested on any
            // global id parsing.
            logger.log(Level.WARNING, "Login failed to {0}", this.baseUrl);
            logger.log(Level.WARNING, "Exception String: {0}", e.toString());
            logger.log(Level.WARNING, "Localized message: {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "Cause:", e.getCause());
            logger.log(Level.WARNING, "Message {0}", e.getMessage());
        // TODO: is this antipattern really necessary?
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Other Error on ezidService.login(USERNAME, PASSWORD) - not EZIDException ", e.getMessage());
        }
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) throws Exception {
        logger.log(Level.FINE,"alreadyRegistered");
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
            logger.log(Level.WARNING, "alreadyRegistered failed");
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
        return List.of("EZID", this.baseUrl);
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

    @Override
    protected String getProviderKeyName() {
        return "EZID";
    }

}

