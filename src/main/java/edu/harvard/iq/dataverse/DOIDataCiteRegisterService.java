/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean.GlobalIdMetadataTemplate;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.xml.transform.Source;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteRegisterService {

    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    DOIDataCiteServiceBean doiDataCiteServiceBean;
    
        
    //A singleton since it, and the httpClient in it can be reused.
    private DataCiteRESTfullClient client=null;
    
    private DataCiteRESTfullClient getClient() throws IOException {
        if (client == null) {
            client = new DataCiteRESTfullClient(System.getProperty("doi.baseurlstring"), System.getProperty("doi.username"), System.getProperty("doi.password"));
        }
        return client;
    }

    public String createIdentifierLocal(String identifier, Map<String, String> metadata, DvObject dvObject) {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
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
        retString = "success to reserved " + identifier;

        return retString;
    }

    public String registerIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
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
            try {
                DataCiteRESTfullClient client = getClient();
                retString = client.postMetadata(xmlMetadata);
                client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                DataCiteRESTfullClient client = getClient();
                retString = client.postMetadata(xmlMetadata);
                client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return retString;
    }
    
    public String reRegisterIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
        String numericIdentifier = identifier.substring(identifier.indexOf(":") + 1);
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        String target = metadata.get("_target");
        DataCiteRESTfullClient client = getClient();
        String currentMetadata = client.getMetadata(numericIdentifier);
        Diff myDiff = DiffBuilder.compare(xmlMetadata)
                .withTest(currentMetadata).ignoreWhitespace().checkForSimilar()
                .build();

        if (myDiff.hasDifferences()) {
            for(Difference d : myDiff.getDifferences()) {
            
              logger.fine(d.toString());
            }
            retString = "metadata:\\r" + client.postMetadata(xmlMetadata) + "\\r";
        }
        if (!target.equals(client.getUrl(numericIdentifier))) {
            logger.info("Updating target URL to " +  target);
            client.postUrl(numericIdentifier, target);
            retString = retString + "url:\\r" + target;

        }

        return retString;
    }

    public String deactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) {
        String retString = "";

            String metadataString = getMetadataForDeactivateIdentifier(identifier, metadata, dvObject);
            retString = client.postMetadata(metadataString);
            retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));

        return retString;
    }
    
        public static String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

        Dataset dataset = null;

        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else {
            dataset = (Dataset) dvObject.getOwner();
        }

        DataCiteMetadataTemplate metadataTemplate = new DataCiteMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Util.getListFromStr(metadata.get("datacite.creator")));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            String description = dataset.getLatestVersion().getDescriptionPlainText();
            if (description.isEmpty() || description.equals(DatasetField.NA_VALUE)) {
                description = AbstractGlobalIdServiceBean.UNAVAILABLE;
            }
            metadataTemplate.setDescription(description);
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            //Note: File metadata is not escaped like dataset metadata is, so adding an xml escape here.
            //This could/should be removed if the datafile methods add escaping
            String fileDescription = StringEscapeUtils.escapeXml(df.getDescription());
            metadataTemplate.setDescription(fileDescription == null ? AbstractGlobalIdServiceBean.UNAVAILABLE : fileDescription);
            String datasetPid = df.getOwner().getGlobalId().asString();
            metadataTemplate.setDatasetIdentifier(datasetPid);
        } else {
            metadataTemplate.setDatasetIdentifier("");
        }

        metadataTemplate.setContacts(dataset.getLatestVersion().getDatasetContacts());
        metadataTemplate.setProducers(dataset.getLatestVersion().getDatasetProducers());
        String title = dvObject.getCurrentName();
        if(dvObject.isInstanceofDataFile()) {
            //Note file title is not currently escaped the way the dataset title is, so adding it here.
            title = StringEscapeUtils.escapeXml(title);
        }
        
        if (title.isEmpty() || title.equals(DatasetField.NA_VALUE)) {
            title = AbstractGlobalIdServiceBean.UNAVAILABLE;
        }
        
        metadataTemplate.setTitle(title);
        //QDR use institution name
        String producerString = BundleUtil.getStringFromBundle("institution.name");
        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = AbstractGlobalIdServiceBean.UNAVAILABLE;
        }
        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public static String getMetadataForDeactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) {

        DataCiteMetadataTemplate metadataTemplate = new DataCiteMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Util.getListFromStr(metadata.get("datacite.creator")));

        metadataTemplate.setDescription(AbstractGlobalIdServiceBean.UNAVAILABLE);

        String title =metadata.get("datacite.title");
        
        System.out.print("Map metadata title: "+ metadata.get("datacite.title"));
        
        metadataTemplate.setAuthors(null);
        
        metadataTemplate.setTitle(title);
        String producerString = AbstractGlobalIdServiceBean.UNAVAILABLE;

        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public String modifyIdentifier(String identifier, HashMap<String, String> metadata, DvObject dvObject) throws IOException {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        logger.fine("XML to send to DataCite: " + xmlMetadata);

        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
        if (status.equals("reserved")) {
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
            retString = "success to reserved " + identifier;
        } else if (status.equals("public")) {
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
                    DataCiteRESTfullClient client = getClient();
                    retString = client.postMetadata(xmlMetadata);
                    client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);

                } catch (UnsupportedEncodingException ex) {
                    logger.log(Level.SEVERE, null, ex);

                } catch (RuntimeException rte) {
                    logger.log(Level.SEVERE, "Error creating DOI at DataCite: {0}", rte.getMessage());
                    logger.log(Level.SEVERE, "Exception", rte);

                }
            }
        } else if (status.equals("unavailable")) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            try {
                DataCiteRESTfullClient client = getClient();
                if (rc != null) {
                    rc.setStatus("unavailable");
                    retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
                }
            } catch (IOException io) {

            }
        }
        return retString;
    }

    public boolean testDOIExists(String identifier) {
        boolean doiExists;
        try {
            DataCiteRESTfullClient client = getClient();
            doiExists = client.testDOIExists(identifier.substring(identifier.indexOf(":") + 1));
        } catch (Exception e) {
            logger.log(Level.INFO, identifier, e);
            return false;
        }
        return doiExists;
    }

    public HashMap<String, String> getMetadata(String identifier) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        try {
            DataCiteRESTfullClient client = getClient();
            String xmlMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":") + 1));
            DOIDataCiteServiceBean.GlobalIdMetadataTemplate template = doiDataCiteServiceBean.new GlobalIdMetadataTemplate(xmlMetadata);
            metadata.put("datacite.creator", Util.getStrFromList(template.getCreators()));
            metadata.put("datacite.title", template.getTitle());
            metadata.put("datacite.publisher", template.getPublisher());
            metadata.put("datacite.publicationyear", template.getPublisherYear());
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc != null) {
                metadata.put("_status", rc.getStatus());
            } else {
                metadata.put("_status", "public");
            }
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }

    public DOIDataCiteRegisterCache findByDOI(String doi) {
        TypedQuery<DOIDataCiteRegisterCache> query = em.createNamedQuery("DOIDataCiteRegisterCache.findByDoi",
                DOIDataCiteRegisterCache.class);
        query.setParameter("doi", doi);
        List<DOIDataCiteRegisterCache> rc = query.getResultList();
        if (rc.size() == 1) {
            return rc.get(0);
        }
        return null;
    }

    public void deleteIdentifier(String identifier) {
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        if (rc != null) {
            em.remove(rc);
        }
    }

}

class DataCiteMetadataTemplate {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.DataCiteMetadataTemplate");
    private static String template;

    static {
        try (InputStream in = DataCiteMetadataTemplate.class.getResourceAsStream("datacite_metadata_template.xml")) {
            template = Util.readAndClose(in, "utf-8");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "datacite metadata template load error");
            logger.log(Level.SEVERE, "String " + e.toString());
            logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
            logger.log(Level.SEVERE, "cause " + e.getCause());
            logger.log(Level.SEVERE, "message " + e.getMessage());
        }
    }

    private String xmlMetadata;
    private String identifier;
    private String datasetIdentifier;
    private List<String> datafileIdentifiers;
    private List<String> creators;
    private String title;
    private String publisher;
    private String publisherYear;
    private List<DatasetAuthor> authors;
    private String description;
    private List<String[]> contacts;
    private List<String[]> producers;

    public List<String[]> getProducers() {
        return producers;
    }

    public void setProducers(List<String[]> producers) {
        this.producers = producers;
    }

    public List<String[]> getContacts() {
        return contacts;
    }

    public void setContacts(List<String[]> contacts) {
        this.contacts = contacts;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DatasetAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<DatasetAuthor> authors) {
        this.authors = authors;
    }

    public DataCiteMetadataTemplate() {
    }

    public List<String> getDatafileIdentifiers() {
        return datafileIdentifiers;
    }

    public void setDatafileIdentifiers(List<String> datafileIdentifiers) {
        this.datafileIdentifiers = datafileIdentifiers;
    }

    public DataCiteMetadataTemplate(String xmlMetaData) {
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
        if (authors!= null && !authors.isEmpty()) {
            for (DatasetAuthor author : authors) {
                creatorsElement.append("<creator><creatorName>");
                creatorsElement.append(author.getName().getDisplayValue());
                creatorsElement.append("</creatorName>");

                if (author.getIdType() != null && author.getIdValue() != null && !author.getIdType().isEmpty() && !author.getIdValue().isEmpty() && author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {

                    if (author.getIdType().equals("ORCID")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"https://orcid.org/\" nameIdentifierScheme=\"ORCID\">" + author.getIdValue() + "</nameIdentifier>");
                    }
                    if (author.getIdType().equals("ISNI")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"http://isni.org/isni/\" nameIdentifierScheme=\"ISNI\">" + author.getIdValue() + "</nameIdentifier>");
                    }
                    if (author.getIdType().equals("LCNA")) {
                        creatorsElement.append("<nameIdentifier schemeURI=\"http://id.loc.gov/authorities/names/\" nameIdentifierScheme=\"LCNA\">" + author.getIdValue() + "</nameIdentifier>");
                    }
                }
                if (author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
                    creatorsElement.append("<affiliation>" + author.getAffiliation().getDisplayValue() + "</affiliation>");
                }
                creatorsElement.append("</creator>");
            }

        } else {
            creatorsElement.append("<creator><creatorName>").append(AbstractGlobalIdServiceBean.UNAVAILABLE).append("</creatorName></creator>");
        }

        xmlMetadata = xmlMetadata.replace("${creators}", creatorsElement.toString());

        StringBuilder contributorsElement = new StringBuilder();
        if (this.getContacts() != null) {
            for (String[] contact : this.getContacts()) {
                if (!contact[0].isEmpty()) {
                    contributorsElement.append("<contributor contributorType=\"ContactPerson\"><contributorName>" + contact[0] + "</contributorName>");
                    if (!contact[1].isEmpty()) {
                        contributorsElement.append("<affiliation>" + contact[1] + "</affiliation>");
                    }
                    contributorsElement.append("</contributor>");
                }
            }
        }

        if (this.getProducers() != null) {
            for (String[] producer : this.getProducers()) {
                contributorsElement.append("<contributor contributorType=\"Producer\"><contributorName>" + producer[0] + "</contributorName>");
                if (!producer[1].isEmpty()) {
                    contributorsElement.append("<affiliation>" + producer[1] + "</affiliation>");
                }
                contributorsElement.append("</contributor>");
            }
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
            List<DatasetRelPublication> relatedPublications = dataset.getLatestVersionForCopy().getRelatedPublications();
            if (!relatedPublications.isEmpty()) {
                for (DatasetRelPublication relatedPub : relatedPublications) {
                    String pubIdType = relatedPub.getIdType();
                    String identifier = relatedPub.getIdNumber();
                    /*
                     * Note - with identifier and url fields, it's not clear that there's a single
                     * way those two fields are used for all identifier types In QDR, at this time,
                     * doi and isbn types always have the raw number in the identifier field,
                     * whereas there are examples where URLs are in the identifier or url fields.
                     * The code here addresses those practices and is not generic.
                     */
                    if(pubIdType!=null) {
                    switch (pubIdType) {
                    case "doi":
                        if (identifier != null && identifier.length() != 0) {
                            appendIdentifier(sb, "DOI", "IsSupplementTo", "doi:" + identifier);
                        }
                        break;
                    case "isbn":
                        if (identifier != null && identifier.length() != 0) {
                            appendIdentifier(sb, "ISBN", "IsSupplementTo", "ISBN:" + identifier);
                        }
                        break;
                    case "url":
                        if (identifier != null && identifier.length() != 0) {
                            appendIdentifier(sb, "URL", "IsSupplementTo", identifier);
                        } else {
                            String pubUrl = relatedPub.getUrl();
                            if (pubUrl != null && pubUrl.length() > 0) {
                                appendIdentifier(sb, "URL", "IsSupplementTo", pubUrl);
                            }
                        }
                        break;
                    default:
                        if (identifier != null && identifier.length() != 0) {
                            if (pubIdType.equalsIgnoreCase("arXiv")) {
                                pubIdType = "arXiv";
                            } else if (!pubIdType.equals("bibcode") && !pubIdType.equals("Handle")) {
                                pubIdType = pubIdType.toUpperCase();
                            }
                            // For all others, do a generic attempt to match the identifier type to the
                            // datacite schema and send the raw identifier as the value
                            appendIdentifier(sb, pubIdType, "IsSupplementTo", identifier);
                        }
                        break;
                    }
                    
                } else {
                    logger.info(relatedPub.getIdNumber() + relatedPub.getUrl() + relatedPub.getTitle());
                }
                }
            }
            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    if (!dataFile.getGlobalId().asString().isEmpty()) {
                        appendIdentifier(sb, "DOI", "HasPart", dataFile.getGlobalId().toString());
                    }
                }

                if (!sb.toString().isEmpty()) {
                    sb.append("</relatedIdentifiers>");
                }
            }
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            appendIdentifier(sb, "DOI", "IsPartOf", df.getOwner().getGlobalId().toString());
            if (sb.length() != 0) {
                sb.append("</relatedIdentifiers>");
            }
        }
        return sb.toString();
    }

    private void appendIdentifier(StringBuilder sb, String idType, String relationType, String identifier) {
        if (sb.toString().isEmpty()) {
            sb.append("<relatedIdentifiers>");
        }
        sb.append("<relatedIdentifier relatedIdentifierType=\"" + idType + "\" relationType=\"" + relationType + "\">" + identifier + "</relatedIdentifier>");
    }

    public void generateFileIdentifiers(DvObject dvObject) {

        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;

            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    datafileIdentifiers.add(dataFile.getIdentifier());
                    int x = xmlMetadata.indexOf("</relatedIdentifiers>") - 1;
                    xmlMetadata = xmlMetadata.replace("{relatedIdentifier}", dataFile.getIdentifier());
                    xmlMetadata = xmlMetadata.substring(0, x) + "<relatedIdentifier relatedIdentifierType=\"hasPart\" "
                            + "relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>" + template.substring(x, template.length() - 1);

                }

            } else {
                xmlMetadata = xmlMetadata.replace("<relatedIdentifier relatedIdentifierType=\"hasPart\" relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>", "");
            }
        }
    }

    public static String getTemplate() {
        return template;
    }

    public static void setTemplate(String template) {
        DataCiteMetadataTemplate.template = template;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setDatasetIdentifier(String datasetIdentifier) {
        this.datasetIdentifier = datasetIdentifier;
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
}

class Util {

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Fail to close InputStream");
            }
        }
    }

    public static String readAndClose(InputStream inStream, String encoding) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        String data;
        try {
            int cnt;
            while ((cnt = inStream.read(buf)) >= 0) {
                outStream.write(buf, 0, cnt);
            }
            data = outStream.toString(encoding);
        } catch (IOException ioe) {
            throw new RuntimeException("IOException");
        } finally {
            close(inStream);
        }
        return data;
    }

    public static List<String> getListFromStr(String str) {
        return Arrays.asList(str.split("; "));
//        List<String> authors = new ArrayList();
//        int preIdx = 0;
//        for(int i=0;i<str.length();i++){
//            if(str.charAt(i)==';'){
//                authors.add(str.substring(preIdx,i).trim());
//                preIdx = i+1;
//            }
//        }
//        return authors;
    }

    public static String getStrFromList(List<String> authors) {
        StringBuilder str = new StringBuilder();
        for (String author : authors) {
            if (str.length() > 0) {
                str.append("; ");
            }
            str.append(author);
        }
        return str.toString();
    }
    
}
