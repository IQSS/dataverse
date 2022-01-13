package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Identifier;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResourceCreator;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.cache.DOIDataCiteRegisterCache;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Description;

/**
 * @author luopc
 */
@Stateless
public class DOIDataCiteRegisterService {

    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DOIDataCiteServiceBean doiDataCiteServiceBean;

    @Inject
    private SettingsServiceBean settingsService;


    //A singleton since it, and the httpClient in it can be reused.
    private DataCiteMdsApiClient client = null;

    // -------------------- LOGIC --------------------

    /**
     * This "reserveIdentifier" method is heavily based on the
     * "registerIdentifier" method below, but this one doesn't
     * register a URL, which causes the "state" of DOI to transition from
     * "draft" to "findable". Here are some DataCite docs on the matter:
     * <p>
     * "DOIs can exist in three states: draft, registered, and findable. DOIs
     * are in the draft state when metadata have been registered, and will
     * transition to the findable state when registering a URL." --
     * https://support.datacite.org/docs/mds-api-guide#doi-states
     */
    public String reserveIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        DOIDataCiteRegisterCache rc = Optional.ofNullable(findByDOI(identifier))
                .orElseGet(DOIDataCiteRegisterCache::new);
        String target = Optional.ofNullable(metadata.get("_target"))
                .orElse("");
        rc.setDoi(identifier);
        rc.setXml(xmlMetadata);
        rc.setStatus("reserved");
        if (target.trim().length() > 0) {
            rc.setUrl(target);
        }
        em.merge(rc);
        return reserveDOI(xmlMetadata);
    }

    public void registerIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        String target = metadata.get("_target");
        if (rc != null) {
            rc.setDoi(identifier);
            rc.setXml(xmlMetadata);
            rc.setStatus("public");
            if (target == null || target.trim().length() == 0) {
                target = rc.getUrl();
            } else {
                rc.setUrl(target);
            }
        }
        registerDOI(identifier.split(":")[1], target, xmlMetadata);
    }

    public void deactivateIdentifier(String identifier) {
        String metadataString = getMetadataForDeactivateId(identifier);
        client.postMetadata(metadataString);
        client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
    }

    public String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {
        DataCiteResourceCreator dtoCreator = new DataCiteResourceCreator();
        DataCiteResource resource = dtoCreator.create(identifier, metadata.get("datacite.publicationyear"), dvObject);
        return convertToXml(resource);
    }

    public String getMetadataForDeactivateId(String identifier) {
        DataCiteResource resource = new DataCiteResource();
        resource.setIdentifier(new Identifier(identifier.substring(identifier.indexOf(":") + 1)));
        resource.setDescriptions(Collections.singletonList(new Description(":unav")));
        resource.setTitles(Collections.singletonList("This item has been removed from publication"));
        resource.setPublisher(":unav");
        resource.setPublicationYear("9999");
        return convertToXml(resource);
    }

    public void modifyIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        logger.fine("XML to send to DataCite: " + xmlMetadata);

        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        if ("reserved".equals(status)) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc == null) {
                rc = new DOIDataCiteRegisterCache();
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
                em.persist(rc);
            } else {
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
            }
        } else if ("public".equals(status)) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc != null) {
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("public");
                if (target == null || target.trim().length() == 0) {
                    target = rc.getUrl();
                } else {
                    rc.setUrl(target);
                }
                try {
                    DataCiteMdsApiClient client = getClient();
                    client.postMetadata(xmlMetadata);
                    client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
                } catch (UnsupportedEncodingException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (RuntimeException rte) {
                    logger.log(Level.SEVERE, "Error creating DOI at DataCite: {0}", rte.getMessage());
                    logger.log(Level.SEVERE, "Exception", rte);
                }
            }
        } else if ("unavailable".equals(status)) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            try {
                DataCiteMdsApiClient client = getClient();
                if (rc != null) {
                    rc.setStatus("unavailable");
                    client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Exception encountered: ", ioe);
            }
        }
    }

    public boolean testDOIExists(String identifier) {
        try {
            DataCiteMdsApiClient client = getClient();
            return client.testDOIExists(identifier.substring(identifier.indexOf(":") + 1));
        } catch (Exception e) {
            logger.log(Level.INFO, identifier, e);
            return false;
        }
    }

    public Map<String, String> getMetadata(String identifier) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        try {
            DataCiteMdsApiClient client = getClient();
            String xmlMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":") + 1));
            Document document = Jsoup.parseBodyFragment(xmlMetadata);
            metadata.put("datacite.creator", extractAll(document, "creatorName"));
            metadata.put("datacite.title", extractFirst(document, "title"));
            metadata.put("datacite.publisher", extractFirst(document, "publisher"));
            metadata.put("datacite.publicationyear", extractFirst(document, "publicationYear"));
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc != null) {
                metadata.put("_status", rc.getStatus());
            }
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }

    private String extractFirst(Document document, String element) {
        Elements elements = document.select(element);
        return elements.isEmpty() ? null : elements.get(0).html();
    }

    private String extractAll(Document document, String element) {
        return document.select(element).stream()
                .map(Element::html)
                .collect(Collectors.joining("; "));
    }

    public DOIDataCiteRegisterCache findByDOI(String doi) {
        TypedQuery<DOIDataCiteRegisterCache> query =
                em.createNamedQuery("DOIDataCiteRegisterCache.findByDoi", DOIDataCiteRegisterCache.class);
        query.setParameter("doi", doi);
        List<DOIDataCiteRegisterCache> rc = query.getResultList();
        return rc.size() == 1 ? rc.get(0) : null;
    }

    public void deleteIdentifier(String identifier) {
        client.deleteDoi(identifier);
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        if (rc != null) {
            em.remove(rc);
        }
    }

    // -------------------- PRIVATE --------------------

    private DataCiteMdsApiClient getClient() throws IOException {
        if (client == null) {
            client = new DataCiteMdsApiClient(settingsService.getValueForKey(SettingsServiceBean.Key.DoiBaseUrlString),
                    settingsService.getValueForKey(SettingsServiceBean.Key.DoiUsername),
                    settingsService.getValueForKey(SettingsServiceBean.Key.DoiPassword));
        }
        return client;
    }

    private String convertToXml(DataCiteResource resource) {
        try {
            XmlMapper mapper = new XmlMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            return mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Exception while creating XML", e);
            return StringUtils.EMPTY;
        }
    }

    private String reserveDOI(String xmlMetadata) throws IOException {
        try {
            DataCiteMdsApiClient client = getClient();
            return client.postMetadata(xmlMetadata);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
            return StringUtils.EMPTY;
        }
    }

    private void registerDOI(String identifier, String target, String metadata) throws IOException {
        try {
            DataCiteMdsApiClient client = getClient();
            client.postMetadata(metadata);
            client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}