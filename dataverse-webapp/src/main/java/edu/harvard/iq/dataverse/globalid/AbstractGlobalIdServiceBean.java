package edu.harvard.iq.dataverse.globalid;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractGlobalIdServiceBean implements GlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(AbstractGlobalIdServiceBean.class.getCanonicalName());
    private static final String UNAVAILABLE = ":unav";

    @EJB
    DataverseDao dataverseDao;
    @Inject
    SettingsServiceBean settingsService;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DatasetDao datasetDao;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    SystemConfig systemConfig;

    // -------------------- LOGIC --------------------

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String identifier) {
        logger.log(Level.FINE, "getIdentifierForLookup");
        return protocol + ":" + authority + "/" + identifier;
    }

    @Override
    public Map<String, String> getMetadataForCreateIndicator(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getMetadataForCreateIndicator(DvObject)");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        metadata.put("_target", getTargetUrl(dvObjectIn));
        return metadata;
    }

    protected Map<String, String> getUpdateMetadata(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getUpdateMetadataFromDataset");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        return metadata;
    }

    protected Map<String, String> addBasicMetadata(DvObject dvObjectIn, Map<String, String> metadata) {

        String authorString = dvObjectIn.getAuthorString();

        if (authorString.isEmpty()) {
            authorString = UNAVAILABLE;
        }

        String producerString = dataverseDao.findRootDataverse().getName();

        if (producerString.isEmpty()) {
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

    @Override
    public String getIdentifier(DvObject dvObject) {
        return dvObject.getGlobalId().asString();
    }

    protected String getTargetUrl(Dataset datasetIn) {
        logger.log(Level.FINE, "getTargetUrl");
        return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalIdString();
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

    @Override
    public DvObject generateIdentifier(DvObject dvObject) {

        String protocol = dvObject.getProtocol() == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Protocol) : dvObject.getProtocol();
        String authority = dvObject.getAuthority() == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Authority) : dvObject.getAuthority();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, commandEngine.getContext());
        if (dvObject.isInstanceofDataset()) {
            dvObject.setIdentifier(datasetDao.generateDatasetIdentifier((Dataset) dvObject));
        } else {
            dvObject.setIdentifier(datafileService.generateDataFileIdentifier((DataFile) dvObject, idServiceBean));
        }
        if (dvObject.getProtocol() == null) {
            dvObject.setProtocol(protocol);
        }
        if (dvObject.getAuthority() == null) {
            dvObject.setAuthority(authority);
        }
        return dvObject;
    }

    public String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

        Dataset dataset = (Dataset) (dvObject instanceof Dataset ? dvObject : dvObject.getOwner());

        GlobalIdMetadataTemplate metadataTemplate = new GlobalIdMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(getListFromStr(metadata.get("datacite.creator")));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            metadataTemplate.setDescription(dataset.getLatestVersion().getDescriptionPlainText());
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            String fileDescription = df.getDescription();
            metadataTemplate.setDescription(fileDescription == null ? "" : fileDescription);
        }

        metadataTemplate.setContacts(dataset.getLatestVersion().getDatasetContacts());
        metadataTemplate.setProducers(dataset.getLatestVersion().getDatasetProducers());
        metadataTemplate.setTitle(dvObject.getDisplayName());
        String producerString = dataverseDao.findRootDataverse().getName();
        if (producerString.isEmpty()) {
            producerString = UNAVAILABLE;
        }
        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    private List<String> getListFromStr(String str) {
        return str != null
                ? Arrays.asList(str.split("; "))
                : Collections.emptyList();
    }

    // -------------------- INNER CLASSES --------------------

    // TODO should be refactored to use ResourceDTOCreator
    static class GlobalIdMetadataTemplate {

        private String template;

        private String xmlMetadata;
        private String identifier;
        private List<String> datafileIdentifiers;
        private List<String> creators;
        private String title;
        private String publisher;
        private String publisherYear;
        private List<DatasetAuthor> authors;
        private String description;
        private List<String[]> contacts;
        private List<String[]> producers;

        // -------------------- CONSTRUCTORS --------------------

        public GlobalIdMetadataTemplate() {
            try (InputStream in = GlobalIdMetadataTemplate.class.getResourceAsStream("datacite_metadata_template.xml")) {
                template = readAndClose(in);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "datacite metadata template load error");
                logger.log(Level.SEVERE, "String " + e.toString());
                logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
                logger.log(Level.SEVERE, "cause " + e.getCause());
                logger.log(Level.SEVERE, "message " + e.getMessage());
            }
        }

        // -------------------- GETTERS --------------------

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String templateIn) {
            template = templateIn;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public List<String> getCreators() {
            return creators;
        }

        public void setCreators(List<String> creators) {
            this.creators = creators;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getPublisherYear() {
            return publisherYear;
        }

        public void setPublisherYear(String publisherYear) {
            this.publisherYear = publisherYear;
        }

        public List<String[]> getProducers() {
            return producers;
        }

        public List<String[]> getContacts() {
            return contacts;
        }

        public String getDescription() {
            return description;
        }

        public List<DatasetAuthor> getAuthors() {
            return authors;
        }

        public List<String> getDatafileIdentifiers() {
            return datafileIdentifiers;
        }

        // -------------------- LOGIC --------------------

        public GlobalIdMetadataTemplate(String xmlMetaData) {
            this.xmlMetadata = xmlMetaData;
            Document doc = Jsoup.parseBodyFragment(xmlMetaData);
            Elements identifierElements = doc.select("identifier");
            if (identifierElements.size() > 0) {
                identifier = identifierElements.get(0).html();
            }
            Elements creatorElements = doc.select("creatorName");
            creators = new ArrayList<>();
            for (Element creatorElement : creatorElements) {
                creators.add(creatorElement.html());
            }
            Elements titleElements = doc.select("title");
            if (titleElements.size() > 0) {
                title = titleElements.get(0).html();
            }
            Elements publisherElements = doc.select("publisher");
            if (publisherElements.size() > 0) {
                publisher = publisherElements.get(0).html();
            }
            Elements publisherYearElements = doc.select("publicationYear");
            if (publisherYearElements.size() > 0) {
                publisherYear = publisherYearElements.get(0).html();
            }
        }

        public String generateXML(DvObject dvObject) {
            // Can't use "UNKNOWN" here because DataCite will respond with "[facet 'pattern'] the value 'unknown' is not accepted by the pattern '[\d]{4}'"
            String publisherYearFinal = "9999";
            // FIXME: Investigate why this.publisherYear is sometimes null now that pull request #4606 has been merged.
            if (this.publisherYear != null) {
                // Added to prevent a NullPointerException when trying to destroy datasets when using DataCite rather than EZID.
                publisherYearFinal = this.publisherYear;
            }
            xmlMetadata = template.replace("${identifier}", this.identifier.trim())
                    .replace("${title}", this.title)
                    .replace("${publisher}", this.publisher)
                    .replace("${publisherYear}", publisherYearFinal)
                    .replace("${description}", this.description);
            StringBuilder creatorsElement = new StringBuilder();
            for (DatasetAuthor author : authors) {
                creatorsElement.append("<creator><creatorName>")
                        .append(author.getName().getDisplayValue())
                        .append("</creatorName>");

                if (author.getIdType() != null && author.getIdValue() != null && !author.getIdType().isEmpty() && !author.getIdValue().isEmpty() && author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {

                    if (author.getIdType().equals("ORCID")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"https://orcid.org/\" nameIdentifierScheme=\"ORCID\">")
                                .append(author.getIdValue())
                                .append("</nameIdentifier>");
                    }
                    if (author.getIdType().equals("ISNI")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"http://isni.org/isni/\" nameIdentifierScheme=\"ISNI\">")
                                .append(author.getIdValue())
                                .append("</nameIdentifier>");
                    }
                    if (author.getIdType().equals("LCNA")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"http://id.loc.gov/authorities/names/\" nameIdentifierScheme=\"LCNA\">")
                                .append(author.getIdValue())
                                .append("</nameIdentifier>");
                    }
                }
                if (author.getAffiliation() != null && !author.getAffiliation().getValue().isEmpty()) {
                    creatorsElement.append("<affiliation>")
                            .append(author.getAffiliation().getValue())
                            .append("</affiliation>");
                }
                creatorsElement.append("</creator>");
            }
            xmlMetadata = xmlMetadata.replace("${creators}", creatorsElement.toString());

            StringBuilder contributorsElement = new StringBuilder();
            for (String[] contact : this.getContacts()) {
                if (!contact[0].isEmpty()) {
                    contributorsElement.append("<contributor contributorType=\"ContactPerson\"><contributorName>")
                            .append(contact[0])
                            .append("</contributorName>");
                    if (!contact[1].isEmpty()) {
                        contributorsElement.append("<affiliation>")
                                .append(contact[1])
                                .append("</affiliation>");
                    }
                    contributorsElement.append("</contributor>");
                }
            }
            for (String[] producer : this.getProducers()) {
                contributorsElement.append("<contributor contributorType=\"Producer\"><contributorName>")
                        .append(producer[0])
                        .append("</contributorName>");
                if (!producer[1].isEmpty()) {
                    contributorsElement.append("<affiliation>")
                            .append(producer[1])
                            .append("</affiliation>");
                }
                contributorsElement.append("</contributor>");
            }

            String relIdentifiers = generateRelatedIdentifiers(dvObject);

            xmlMetadata = xmlMetadata.replace("${relatedIdentifiers}", relIdentifiers);

            xmlMetadata = xmlMetadata.replace("{$contributors}", contributorsElement.toString());
            return xmlMetadata;
        }

        private String generateRelatedIdentifiers(DvObject dvObject) {

            StringBuilder sb = new StringBuilder();
            if (dvObject.isInstanceofDataset()) {
                Dataset dataset = (Dataset) dvObject;
                if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                    datafileIdentifiers = new ArrayList<>();
                    for (DataFile dataFile : dataset.getFiles()) {
                        if (!dataFile.getGlobalId().asString().isEmpty()) {
                            if (sb.toString().isEmpty()) {
                                sb.append("<relatedIdentifiers>");
                            }
                            sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"HasPart\">")
                                    .append(dataFile.getGlobalId())
                                    .append("</relatedIdentifier>");
                        }
                    }

                    if (!sb.toString().isEmpty()) {
                        sb.append("</relatedIdentifiers>");
                    }
                }
            } else if (dvObject.isInstanceofDataFile()) {
                DataFile df = (DataFile) dvObject;
                sb.append("<relatedIdentifiers>")
                        .append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"IsPartOf\"" + ">")
                        .append(df.getOwner().getGlobalId())
                        .append("</relatedIdentifier>")
                        .append("</relatedIdentifiers>");
            }
            return sb.toString();
        }

        // -------------------- PRIVATE --------------------

        private String readAndClose(InputStream inStream) {
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
                byte[] buf = new byte[128];
                int cnt;
                while ((cnt = inStream.read(buf)) >= 0) {
                    outStream.write(buf, 0, cnt);
                }
                return outStream.toString("utf-8");
            } catch (IOException ioe) {
                throw new RuntimeException("IOException");
            }
        }

        // -------------------- SETTERS --------------------

        public void setProducers(List<String[]> producers) {
            this.producers = producers;
        }

        public void setContacts(List<String[]> contacts) {
            this.contacts = contacts;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setAuthors(List<DatasetAuthor> authors) {
            this.authors = authors;
        }

        public void setDatafileIdentifiers(List<String> datafileIdentifiers) {
            this.datafileIdentifiers = datafileIdentifiers;
        }
    }
}
