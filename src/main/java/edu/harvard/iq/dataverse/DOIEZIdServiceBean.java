/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author skraffmiller
 */
@Stateless
public class DOIEZIdServiceBean extends AbstractIdServiceBean {

    private static final Logger logger = Logger.getLogger(DOIEZIdServiceBean.class.getCanonicalName());

    EZIDService ezidService;

    String baseURLString =  "https://ezid.cdlib.org";
    // get username and password from system properties
    private String USERNAME = "";
    private String PASSWORD = "";

    public DOIEZIdServiceBean() {
        logger.log(Level.FINE,"Constructor");
        baseURLString = System.getProperty("doi.baseurlstring");
        ezidService = new EZIDService (baseURLString);
        USERNAME  = System.getProperty("doi.username");
        PASSWORD  = System.getProperty("doi.password");
        logger.log(Level.FINE, "Using baseURLString {0}", baseURLString);
        try {
            ezidService.login(USERNAME, PASSWORD);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "login failed ");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
        } catch(Exception e){
            System.out.print("Other Error on ezidService.login(USERNAME, PASSWORD) - not EZIDException ");
        }
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyExists(Dataset dataset) throws Exception {
        logger.log(Level.FINE,"alreadyExists");
        try {
            HashMap<String, String> result = ezidService.getMetadata(getIdentifierFromDataset(dataset));            
            return result != null && !result.isEmpty();
            // TODO just check for HTTP status code 200/404, sadly the status code is swept under the carpet
        } catch (EZIDException e ){
            //No such identifier is treated as an exception
            //but if that is the case then we want to just return false
            if (e.getLocalizedMessage().contains("no such identifier")){
                return false;
            }
            logger.log(Level.WARNING, "alreadyExists failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String createIdentifier(Dataset dataset) throws Exception {
        logger.log(Level.FINE,"createIdentifier");
        String identifier = getIdentifierFromDataset(dataset);
        HashMap<String, String> metadata = getMetadataFromStudyForCreateIndicator(dataset);
        metadata.put("datacite.resourcetype", "Dataset");
        metadata.put("_status", "reserved");
        try {
            String retString = ezidService.createIdentifier(identifier, metadata);
            logger.log(Level.FINE, "create DOI identifier retString : " + retString);
            return retString;
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "Identifier not created: create failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            throw e;
        }
    }


    @Override
    public HashMap<String, String> getIdentifierMetadata(Dataset dataset) {
        logger.log(Level.FINE,"getIdentifierMetadata");
        String identifier = getIdentifierFromDataset(dataset);
        HashMap<String, String> metadata = new HashMap<>();
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
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param separator the string that separates authority from local identifier part
     * @param identifier the local identifier part
     * @return a Map of metadata. It is empty when the lookup failed, e.g. when
     * the identifier does not exist.
     */
    @Override
    public HashMap<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier) {
        logger.log(Level.FINE,"lookupMetadataFromIdentifier");
        String identifierOut = getIdentifierForLookup(protocol, authority, separator, identifier);
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
     * @param dataset the Dataset whose metadata needs to be modified
     * @param metadata the new metadata for the Dataset
     * @return the Dataset identifier, or null if the modification failed
     */
    @Override
    public String modifyIdentifier(Dataset dataset, HashMap<String, String> metadata) throws Exception {
        logger.log(Level.FINE,"modifyIdentifier");
        String identifier = getIdentifierFromDataset(dataset);
        try {
            ezidService.setMetadata(identifier, metadata);
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
    public void deleteIdentifier(Dataset datasetIn) {
        logger.log(Level.FINE,"deleteIdentifier");
        String identifier = getIdentifierFromDataset(datasetIn);
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
            updateIdentifierStatus(datasetIn, "unavailable | withdrawn by author");
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("_target", "http://ezid.cdlib.org/id/" + datasetIn.getProtocol() + ":" + datasetIn.getAuthority() 
              + datasetIn.getDoiSeparator()      + datasetIn.getIdentifier());
            try {
                modifyIdentifier(datasetIn, metadata);
            } catch (Exception e) {
                // TODO already logged, how to react here?
            }
        }
    }

    @Override
    public boolean publicizeIdentifier(Dataset dataset) {
        logger.log(Level.FINE,"publicizeIdentifier");
        return updateIdentifierStatus(dataset, "public");
    }

    private boolean updateIdentifierStatus(Dataset dataset, String statusIn) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        String identifier = getIdentifierFromDataset(dataset);
        HashMap<String, String> metadata = getUpdateMetadataFromDataset(dataset);
        metadata.put("_status", statusIn);
        metadata.put("_target", getTargetUrl(dataset));
        try {
            ezidService.setMetadata(identifier, metadata);
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
}

