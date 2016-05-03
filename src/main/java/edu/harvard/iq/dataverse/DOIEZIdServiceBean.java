/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;


import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.EZIDServiceRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author skraffmiller
 */
@Stateless
public class DOIEZIdServiceBean  {
    @EJB
    DataverseServiceBean dataverseService;
    @EJB 
    SettingsServiceBean settingsService;
    @EJB
    SystemConfig systemConfig;
    EZIDService ezidService;
    EZIDServiceRequest ezidServiceRequest;
    String baseURLString =  "https://ezid.cdlib.org";
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.index.DOIEZIdServiceBean");
    
    // get username and password from system properties
    private String DOISHOULDER = "";
    private String USERNAME = "";
    private String PASSWORD = "";
    
    public DOIEZIdServiceBean() {
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
    
    public String createIdentifier(Dataset dataset) {
        String retString = "";
        String identifier = getIdentifierFromDataset(dataset);
        HashMap<String, String> metadata = getMetadataFromStudyForCreateIndicator(dataset);
        metadata.put("_status", "reserved");
        try {
            retString = ezidService.createIdentifier(identifier, metadata);
            logger.log(Level.FINE, "create DOI identifier retString : " + retString);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "Identifier not created: create failed");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
            logger.log(Level.WARNING, "message {0}", e.getMessage());
            return "Identifier not created "  +  e.getLocalizedMessage();
        }
        return retString;
    }
    
   
    public HashMap<String, String> getIdentifierMetadata(Dataset dataset) {
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
    public HashMap<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier) {
        String identifierOut = getIdentifierForLookup(protocol, authority, separator, identifier);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = ezidService.getMetadata(identifierOut);
        }  catch (EZIDException e) {
            logger.log(Level.FINE, "None existing so we can use this identifier");
            logger.log(Level.FINE, "identifier: {0}", identifierOut);
            return metadata;
        }
        return metadata;
    }

    /**
     * Concatenate the parts that make up a Global Identifier.
     * @param protocol the identifier system, e.g. "doi"
     * @param authority the namespace that the authority manages in the identifier system
     * @param separator the string that separates authority from local identifier part
     * @param identifier the local identifier part
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        return protocol + ":" + authority + separator  + identifier;
    }

    /**
     * Modifies the EZID metadata for a Dataset
     * @param dataset the Dataset whose metadata needs to be modified
     * @param metadata the new metadata for the Dataset
     * @return the Dataset identifier, or null if the modification failed
     */
    public String modifyIdentifier(Dataset dataset, HashMap<String, String> metadata) {
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
        } 
        return null;
    }
    
    public void deleteIdentifier(Dataset datasetIn) {
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap<String, String> doiMetadata = new HashMap<>();
        try {
            doiMetadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "get matadata failed cannot delete");
            logger.log(Level.WARNING, "String {0}", e.toString());
            logger.log(Level.WARNING, "localized message {0}", e.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", e.getCause());
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
            modifyIdentifier(datasetIn, metadata);
        }
    }
    
    private HashMap<String, String> getUpdateMetadataFromDataset(Dataset datasetIn){
        HashMap<String, String> metadata = new HashMap<>();
        
        String authorString = datasetIn.getLatestVersion().getAuthorsStr();
        
        if(authorString.isEmpty()) {
            authorString = ":unav";
        }
        
        String producerString = dataverseService.findRootDataverse().getName() + " Dataverse";

        if(producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadata.put("datacite.creator", authorString);
	metadata.put("datacite.title", datasetIn.getLatestVersion().getTitle());
	metadata.put("datacite.publisher", producerString);

        return metadata;
        
    }
    
    public HashMap<String, String> getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<>();
        
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
	metadata.put("datacite.resourcetype", "Dataset");
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }
    
    public HashMap<String, String> getMetadataFromDatasetForTargetURL(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<>();         
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }
    
    private String getTargetUrl(Dataset datasetIn) {
        return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalId();
    }
       
    private String getIdentifierFromDataset(Dataset dataset) {
        return dataset.getGlobalId();
    }


    public boolean publicizeIdentifier(Dataset dataset) {
        return updateIdentifierStatus(dataset, "public");
    }
    
    private boolean updateIdentifierStatus(Dataset dataset, String statusIn) {
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

    public static String generateYear()
    {
        StringBuffer guid = new StringBuffer();

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

    public static String generateTimeString()
    {
        StringBuffer guid = new StringBuffer();

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
