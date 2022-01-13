package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResourceCreator;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractGlobalIdServiceBean implements GlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(AbstractGlobalIdServiceBean.class.getCanonicalName());
    private static final String UNAVAILABLE = ":unav";

    @EJB
    private DataverseDao dataverseDao;
    @Inject
    protected SettingsServiceBean settingsService;
    @EJB
    private EjbDataverseEngine commandEngine;
    @EJB
    private DatasetDao datasetDao;
    @EJB
    private DataFileServiceBean datafileService;
    @EJB
    protected SystemConfig systemConfig;

    // -------------------- LOGIC --------------------

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String identifier) {
        logger.log(Level.FINE, "getIdentifierForLookup");
        return String.format("%s:%s/%s", protocol, authority, identifier);
    }

    @Override
    public Map<String, String> getMetadataForCreateIndicator(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getMetadataForCreateIndicator(DvObject)");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        metadata.put("_target", getTargetUrl(dvObjectIn));
        return metadata;
    }

    @Override
    public String getIdentifier(DvObject dvObject) {
        return dvObject.getGlobalId().asString();
    }

    @Override
    public DvObject generateIdentifier(DvObject dvObject) {
        String protocol = dvObject.getProtocol() == null
                ? settingsService.getValueForKey(SettingsServiceBean.Key.Protocol)
                : dvObject.getProtocol();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, commandEngine.getContext());
        dvObject.setIdentifier(dvObject.isInstanceofDataset()
                ? datasetDao.generateDatasetIdentifier((Dataset) dvObject)
                : datafileService.generateDataFileIdentifier((DataFile) dvObject, idServiceBean));
        if (dvObject.getProtocol() == null) {
            dvObject.setProtocol(protocol);
        }
        if (dvObject.getAuthority() == null) {
            dvObject.setAuthority(settingsService.getValueForKey(SettingsServiceBean.Key.Authority));
        }
        return dvObject;
    }

    protected Map<String, String> getUpdateMetadata(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getUpdateMetadataFromDataset");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        return metadata;
    }

    protected Map<String, String> addBasicMetadata(DvObject dvObjectIn, Map<String, String> metadata) {

        String authorString = dvObjectIn.getAuthorString();

        if (StringUtils.isEmpty(authorString)) {
            authorString = UNAVAILABLE;
        }

        String producerString = dataverseDao.findRootDataverse().getName();

        if (StringUtils.isEmpty(producerString)) {
            producerString = UNAVAILABLE;
        }

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", dvObjectIn.getDisplayName());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        return metadata;
    }

    protected String getTargetUrl(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getTargetUrl");
        return systemConfig.getDataverseSiteUrl() + dvObjectIn.getTargetUrl() + dvObjectIn.getGlobalId().asString();
    }

    protected String generateYear(DvObject dvObjectIn) {
        return dvObjectIn.getYearPublishedCreated();
    }

    public Map<String, String> getMetadataForTargetURL(DvObject dvObject) {
        logger.log(Level.FINE, "getMetadataForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        return metadata;
    }

    public String getMetadataFromDvObject(String identifier, DvObject dvObject) {
        String xmlMetadata;
        try {
            XmlMapper mapper = new XmlMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            DataCiteResource resource = new DataCiteResourceCreator()
                    .create(identifier, dvObject.getYearPublishedCreated(), dvObject);

            // Remove unused data:
            resource.getCreators().forEach(c -> c.setNameIdentifier(null));
            resource.setFundingReferences(Collections.emptyList());

            xmlMetadata = mapper.writeValueAsString(resource);
        } catch (JsonProcessingException jpe) {
            logger.log(Level.WARNING, "Error while creating XML", jpe);
            throw new RuntimeException(jpe);
        }
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }
}
