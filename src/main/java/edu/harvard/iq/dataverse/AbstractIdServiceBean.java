package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.IdServiceBean.logger;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.text.SimpleDateFormat;

import javax.ejb.EJB;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractIdServiceBean implements IdServiceBean {

    private static final Logger logger = Logger.getLogger(AbstractIdServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    SystemConfig systemConfig;

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        logger.log(Level.FINE,"getIdentifierForLookup");
        return protocol + ":" + authority + separator + identifier;
    }

    
    @Override
    public HashMap<String, String> getMetadataForCreateIndicator(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getMetadataForCreateIndicator(DvObject)");
        HashMap<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        metadata.put("_target", getTargetUrl(dvObjectIn));
        return metadata;
    }

    protected HashMap<String, String> getUpdateMetadata(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getUpdateMetadataFromDataset");
        HashMap<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        return metadata;
    }
    
    protected HashMap<String, String> addBasicMetadata(DvObject dvObjectIn, HashMap<String, String> metadata) {

        Dataset dataset;
        if (dvObjectIn.isInstanceofDataset()) {
            dataset = (Dataset) dvObjectIn;
        } else {
            dataset = (Dataset) dvObjectIn.getOwner();
        }

        String authorString = dataset.getLatestVersion().getAuthorsStr();

        if (authorString.isEmpty()) {
            authorString = ":unav";
        }

        String producerString = dataverseService.findRootDataverse().getName();

        if (producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", dvObjectIn.getDisplayName());
        metadata.put("datacite.publisher", producerString);
        return metadata;
    }   

    protected String getTargetUrl(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getTargetUrl");
        if (dvObjectIn.isInstanceofDataset()){
              return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + dvObjectIn.getGlobalId();
        }       
        if (dvObjectIn.isInstanceofDataFile()){
              return systemConfig.getDataverseSiteUrl() + DataFile.TARGET_URL + dvObjectIn.getGlobalId();
        }       
        return null;
    }
    
    @Override
    public String getIdentifier(DvObject dvObject)
    {
        return dvObject.getGlobalId();
    }
    
    protected String generateYear (DvObject dvObjectIn){
        Dataset dataset;
        if(dvObjectIn.isInstanceofDataset()){
            dataset = (Dataset) dvObjectIn;
        } else {
            dataset = (Dataset) dvObjectIn.getOwner();
        }
        if (dataset.isReleased()) {
            return dataset.getPublicationDateFormattedYYYYMMDD().substring(0, 4);
        }
        return new SimpleDateFormat("yyyy").format(dataset.getCreateDate()); 
    }
    
     @Override
    public HashMap getIdentifierMetadata(DvObject dvObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public HashMap<String, String> getMetadataForTargetURL(DvObject dvObject) {
        logger.log(Level.FINE,"getMetadataForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        return metadata;
    }

}
