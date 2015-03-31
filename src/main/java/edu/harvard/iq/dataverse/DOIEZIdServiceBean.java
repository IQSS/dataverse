/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;


import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
    EZIDService ezidService;
    EZIDServiceRequest ezidServiceRequest;    
    String baseURLString =  "https://ezid.cdlib.org";  
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.index.DOIEZIdServiceBean");
    
    // get username and password from system properties
    private String DOISHOULDER = "";
    private String USERNAME = "";
    private String PASSWORD = "";    
    
    public DOIEZIdServiceBean(){
        baseURLString = System.getProperty("doi.baseurlstring");
        ezidService = new EZIDService (baseURLString);    
        USERNAME  = System.getProperty("doi.username");
        PASSWORD  = System.getProperty("doi.password");
        logger.log(Level.INFO, "baseURLString " + baseURLString);
        try {
           ezidService.login(USERNAME, PASSWORD);  
        } catch(Exception e){
            logger.log(Level.INFO, "login failed ");
            logger.log(Level.INFO, "String " + e.toString() );
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());           
        }
    }    
    
    public String createIdentifier(Dataset dataset) {
        String retString = "";
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = getMetadataFromStudyForCreateIndicator(dataset);
        metadata.put("_status", "reserved");;       
        try {
            retString = ezidService.createIdentifier(identifier, metadata);
            logger.log(Level.INFO, "create DOI identifier retString : " + retString);
        } catch (EZIDException e) {
            logger.log(Level.INFO, "Identifier not created: create failed");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            return "Identifier not created "  +  e.getLocalizedMessage();
        }
        return retString;
    }
    
   
    public HashMap getIdentifierMetadata(Dataset dataset){
        String identifier = getIdentifierFromDataset(dataset);        
        HashMap metadata = new HashMap();
       try {
              metadata = ezidService.getMetadata(identifier);
            }  catch (EZIDException e){                
            logger.log(Level.INFO, "getIdentifierMetadata failed");
            logger.log(Level.INFO, "String " + e.toString() );
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());    
            return metadata;
        }         
       return metadata;
    }
    
    public HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier){
        String identifierOut = getIdentifierForLookup( protocol,  authority, separator, identifier);        
        HashMap metadata = new HashMap();
       try {
              metadata = ezidService.getMetadata(identifierOut);
            }  catch (EZIDException e){                
            logger.log(Level.INFO, "None existing so we can use this identifier");
            logger.log(Level.INFO, "identifier: " + identifierOut );  
            return metadata;
        }         
       return metadata;
    }
    
    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        return protocol + ":" + authority + separator  + identifier;
    }
    
    
    public void modifyIdentifier(Dataset dataset, HashMap metadata ){
        String identifier = getIdentifierFromDataset(dataset);
       try {
               ezidService.setMetadata(identifier, metadata);
            }  catch (EZIDException e){                
            logger.log(Level.INFO, "modifyMetadata failed");
            logger.log(Level.INFO, "String " + e.toString() );
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());    
        }                
    }
    
    public void deleteIdentifier(Dataset datasetIn) {
        String identifier = getIdentifierFromDataset(datasetIn);
        HashMap doiMetadata = new HashMap();
        try {
            doiMetadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
            logger.log(Level.INFO, "get matadata failed cannot delete");
            logger.log(Level.INFO, "String " + e.toString());
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());
            return;
        }

        String idStatus = (String) doiMetadata.get("_status");
        
        if (idStatus.equals("reserved")) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                ezidService.deleteIdentifier(identifier);
            } catch (EZIDException e) {
                logger.log(Level.INFO, "delete failed");
                logger.log(Level.INFO, "String " + e.toString());
                logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
                logger.log(Level.INFO, "cause " + e.getCause());
                logger.log(Level.INFO, "message " + e.getMessage());
            }
            return;
        }
        if (idStatus.equals("public")) { 
            //if public then it has been released set to unavaialble and reset target to n2t url
            updateIdentifierStatus(datasetIn, "unavailable | withdrawn by author");
            HashMap metadata = new HashMap();
            metadata.put("_target", "http://ezid.cdlib.org/id/" + datasetIn.getProtocol() + ":" + datasetIn.getAuthority() 
              + datasetIn.getDoiSeparator()      + datasetIn.getIdentifier());
            modifyIdentifier(datasetIn, metadata);
        }
    }
    
    private HashMap getUpdateMetadataFromDataset(Dataset datasetIn){
        HashMap<String, String> metadata = new HashMap<String, String>();
        
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
    
    private HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        
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
	metadata.put("datacite.publicationyear", generateYear());
	metadata.put("datacite.resourcetype", "Dataset");
        String inetAddress = getSiteUrl();
        String targetUrl = "";     
        DOISHOULDER = "doi:" + datasetIn.getAuthority();
        
        if (inetAddress.equals("localhost")){                    
           targetUrl ="http://localhost:8080" + "/dataset.xhtml?persistentId=" + DOISHOULDER 
                    + datasetIn.getDoiSeparator()       + datasetIn.getIdentifier();
        } else{
           targetUrl = inetAddress + "/dataset.xhtml?persistentId=" + DOISHOULDER 
                + datasetIn.getDoiSeparator()     + datasetIn.getIdentifier();
        }            
        metadata.put("_target", targetUrl);
        return metadata;
    }
    
    public String getSiteUrl() {
        String hostUrl = System.getProperty("dataverse.siteUrl");
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty("dataverse.fqdn");
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        hostUrl = "https://" + hostName;
        return hostUrl;
    }
   
    private String getIdentifierFromDataset(Dataset dataset){       
        return dataset.getGlobalId();
    }
    

    public void publicizeIdentifier(Dataset studyIn) {
        updateIdentifierStatus(studyIn, "public");
    }
    
    private void updateIdentifierStatus(Dataset dataset, String statusIn){
        String identifier = getIdentifierFromDataset(dataset);
        HashMap metadata = getUpdateMetadataFromDataset(dataset);
        metadata.put("_status", statusIn);
       try {
               ezidService.setMetadata(identifier, metadata);
            }  catch (EZIDException e){                
            logger.log(Level.INFO, "modifyMetadata failed");
            logger.log(Level.INFO, "String " + e.toString() );
            logger.log(Level.INFO, "localized message " + e.getLocalizedMessage());
            logger.log(Level.INFO, "cause " + e.getCause());
            logger.log(Level.INFO, "message " + e.getMessage());    
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