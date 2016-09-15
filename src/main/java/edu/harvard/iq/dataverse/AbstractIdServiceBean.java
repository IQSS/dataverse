package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

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
    public HashMap<String, String> getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        logger.log(Level.FINE,"getMetadataFromStudyForCreateIndicator");
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
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }

    protected HashMap<String, String> getUpdateMetadataFromDataset(Dataset datasetIn) {
        logger.log(Level.FINE,"getUpdateMetadataFromDataset");
        HashMap<String, String> metadata = new HashMap<>();

        String authorString = datasetIn.getLatestVersion().getAuthorsStr();

        if (authorString.isEmpty()) {
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

    @Override
    public HashMap<String, String> getMetadataFromDatasetForTargetURL(Dataset datasetIn) {
        logger.log(Level.FINE,"getMetadataFromDatasetForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }

    protected String getTargetUrl(Dataset datasetIn) {
        logger.log(Level.FINE,"getTargetUrl");
        return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalId();
    }

    @Override
    public String getIdentifierFromDataset(Dataset dataset) {
        logger.log(Level.FINE,"getIdentifierFromDataset");
        return dataset.getGlobalId();
    }

    @Override
    public String generateYear()
    {
        StringBuilder guid = new StringBuilder();

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

    @Override
    public String generateTimeString()
    {
        StringBuilder guid = new StringBuilder();

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
