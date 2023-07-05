package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;


/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteServiceBean extends DOIServiceBean {

    private static final Logger logger = Logger.getLogger(DOIDataCiteServiceBean.class.getCanonicalName());
    
    private static final String PUBLIC = "public";
    private static final String FINDABLE = "findable";
    private static final String RESERVED = "reserved";
    private static final String DRAFT = "draft";

    @EJB
    DOIDataCiteRegisterService doiDataCiteRegisterService;

    @Override
    public boolean registerWhenPublished() {
        return false;
    }



    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) {
        logger.log(Level.FINE,"alreadyRegistered");
        if(pid==null || pid.asString().isEmpty()) {
            logger.fine("No identifier sent.");
            return false;
        }
        boolean alreadyRegistered;
        String identifier = pid.asString();
        try{
            alreadyRegistered = doiDataCiteRegisterService.testDOIExists(identifier); 
        } catch (Exception e){
            logger.log(Level.WARNING, "alreadyRegistered failed");
            return false;
        }
        return  alreadyRegistered;
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
            String retString = doiDataCiteRegisterService.reserveIdentifier(identifier, metadata, dvObject);
            logger.log(Level.FINE, "create DOI identifier retString : " + retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier not created: create failed", e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.log(Level.FINE,"getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed", e);
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

    /*
     * Deletes a DOI if it is in DRAFT/RESERVED state or removes metadata and changes it from PUBLIC/FINDABLE to REGISTERED.
     */
    @Override
    public void deleteIdentifier(DvObject dvObject) throws IOException, HttpException {
        logger.log(Level.FINE,"deleteIdentifier");
        String identifier = getIdentifier(dvObject);
        //ToDo - PidUtils currently has a DataCite API call that would get the status at DataCite for this identifier - that could be more accurate than assuming based on whether the dvObject has been published
        String idStatus = DRAFT;
        if(dvObject.isReleased()) {
        	idStatus = PUBLIC;
        } 
        if ( idStatus != null ) {
            switch ( idStatus ) {
                case RESERVED:
                case DRAFT:    
                    logger.log(Level.INFO, "Delete status is reserved..");
                  	//service only removes the identifier from the cache (since it was written before DOIs could be registered in draft state)
                    doiDataCiteRegisterService.deleteIdentifier(identifier);
                    //So we call the deleteDraftIdentifier method below until things are refactored
                    deleteDraftIdentifier(dvObject);
                    break;

                case PUBLIC:
                case FINDABLE:
                    //if public then it has been released set to unavailable and reset target to n2t url
                    Map<String, String> metadata = addDOIMetadataForDestroyedDataset(dvObject);
                    metadata.put("_status", "registered");
                    metadata.put("_target", getTargetUrl(dvObject));                   
                    doiDataCiteRegisterService.deactivateIdentifier(identifier, metadata, dvObject);
                    break;
            }
        }
    }
        
    /**
     * Deletes DOI from the DataCite side, if possible. Only "draft" DOIs can be
     * deleted.
     */
    private void deleteDraftIdentifier(DvObject dvObject) throws IOException {
    	
    	//ToDo - incorporate into DataCiteRESTfulClient
        String baseUrl = JvmSettings.DATACITE_REST_API_URL.lookup();
        String username = JvmSettings.DATACITE_USERNAME.lookup();
        String password = JvmSettings.DATACITE_PASSWORD.lookup();
        GlobalId doi = dvObject.getGlobalId();
        /**
         * Deletes the DOI from DataCite if it can. Returns 204 if PID was deleted
         * (only possible for "draft" DOIs), 405 (method not allowed) if the DOI
         * wasn't deleted (because it's in "findable" state, for example, 404 if the
         * DOI wasn't found, and possibly other status codes such as 500 if DataCite
         * is down.
         */

            URL url = new URL(baseUrl + "/dois/" + doi.getAuthority() + "/" + doi.getIdentifier());
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
            int status = connection.getResponseCode();
            if(status!=HttpStatus.SC_NO_CONTENT) {
            	logger.warning("Incorrect Response Status from DataCite: " + status + " : " + connection.getResponseMessage());
            	throw new HttpException("Status: " + status);
            }
            logger.fine("deleteDoi status for " + doi.asString() + ": " + status);
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        if(dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty() ){
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        metadata.put("_status", PUBLIC);
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
        return List.of("DataCite", "https://status.datacite.org");
    }



    @Override
    protected String getProviderKeyName() {
        return "DataCite";
    }
}
