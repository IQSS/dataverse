package edu.harvard.iq.dataverse.pidproviders.doi.crossref;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;
import jakarta.enterprise.inject.spi.CDI;
import org.apache.commons.text.StringEscapeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.SystemConfig.getDataverseSiteUrlStatic;

public class CrossRefDOIRegisterService implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(CrossRefDOIRegisterService.class.getCanonicalName());

    private final String url;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final String depositor;
    private final String depositorEmail;


    private static DataverseServiceBean dataverseService = null;

    private CrossRefRESTfullClient client = null;

    public CrossRefDOIRegisterService(String url, String apiUrl, String username, String password, String depositor, String depositorEmail) {
        this.url = url;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.depositor = depositor;
        this.depositorEmail = depositorEmail;
    }

    private CrossRefRESTfullClient getClient() {
        if (client == null) {
            client = new CrossRefRESTfullClient(url, apiUrl, username, password);
        }
        return client;
    }

    public boolean testDOIExists(String identifier) {
        boolean doiExists;
        try {
            CrossRefRESTfullClient client = getClient();
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
            CrossRefRESTfullClient client = getClient();
            String jsonMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":") + 1));
            Map<String, Object> mappedJson = new ObjectMapper().readValue(jsonMetadata, HashMap.class);
            logger.log(Level.FINE, jsonMetadata);
            metadata.put("_status", mappedJson.get("status").toString());
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }

    public String reserveIdentifier(String identifier, DvObject dvObject) throws IOException {
        logger.fine("Crossref reserveIdentifier");
        String xmlMetadata = getMetadataFromDvObject(identifier, dvObject);

        CrossRefRESTfullClient client = getClient();
        return client.postMetadata(xmlMetadata);
    }

    public void modifyIdentifier(String identifier, DvObject dvObject) throws IOException {
        logger.fine("Crossref modifyIdentifier");
        String xmlMetadata = getMetadataFromDvObject(identifier, dvObject);

        CrossRefRESTfullClient client = getClient();
        client.postMetadata(xmlMetadata);
    }

    public String getMetadataFromDvObject(String identifier, DvObject dvObject) {
        Dataset dataset;

        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else {
            dataset = (Dataset) dvObject.getOwner();
        }
        if (dataverseService == null) {
            dataverseService = CDI.current().select(DataverseServiceBean.class).get();
        }

        CrossRefMetadataTemplate metadataTemplate = new CrossRefMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        metadataTemplate.setDepositor(depositor);
        metadataTemplate.setDepositorEmail(depositorEmail);
        metadataTemplate.setInstitution(dataverseService.getRootDataverseName());

        String title = dvObject.getCurrentName();
        if (dvObject.isInstanceofDataFile()) {
            //Note file title is not currently escaped the way the dataset title is, so adding it here.
            title = StringEscapeUtils.escapeXml10(title);
        }

        if (title.isEmpty() || title.equals(DatasetField.NA_VALUE)) {
            title = AbstractPidProvider.UNAVAILABLE;
        }

        metadataTemplate.setTitle(title);

        return metadataTemplate.generateXML();
    }
}

class CrossRefMetadataTemplate {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.edu.harvard.iq.dataverse.CrossRefMetadataTemplate");
    private static String template;

    static {
        try (InputStream in = XmlMetadataTemplate.class.getResourceAsStream("crossref_metadata_template.xml")) {
            template = CrossRefFileUtil.readAndClose(in, "utf-8");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "crossref metadata template load error");
            logger.log(Level.SEVERE, "String " + e);
            logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
            logger.log(Level.SEVERE, "cause " + e.getCause());
            logger.log(Level.SEVERE, "message " + e.getMessage());
        }
    }

    private final String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    private String institution;
    private String depositor;
    private String depositorEmail;
    private String databaseTitle;
    private String identifier;
    private String title;
    private final String baseUrl = getDataverseSiteUrlStatic();
    private List<DatasetAuthor> authors;

    public List<DatasetAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<DatasetAuthor> authors) {
        this.authors = authors;
    }

    public CrossRefMetadataTemplate() {
    }

    public String generateXML() {
        String xmlMetadata = template.replace("${depositor}", depositor)
                .replace("${depositorEmail}", depositorEmail)
                .replace("${title}", title)
                .replace("${institution}", institution)
                .replace("${batchId}", identifier + " " + timestamp)
                .replace("${timestamp}", timestamp);

        StringBuilder datasetElement = new StringBuilder();
        datasetElement.append("<dataset dataset_type=\"collection\">");

        StringBuilder contributorsElement = new StringBuilder();
        if (authors != null && !authors.isEmpty()) {
            contributorsElement.append("<contributors>");
            for (DatasetAuthor author : authors) {
                contributorsElement.append("<person_name contributor_role=\"author\" sequence=\"first\"><given_name>");
                contributorsElement.append(author.getName().getDisplayValue());
                contributorsElement.append("</given_name><surname>");
                contributorsElement.append(author.getName().getDisplayValue());
                contributorsElement.append("</surname>");

                if (author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
                    contributorsElement.append("<affiliations><institution><institution_name>")
                            .append(author.getAffiliation().getDisplayValue())
                            .append("</institution_name></institution></affiliations>");
                }

                if (author.getIdType() != null &&
                        author.getIdValue() != null &&
                        !author.getIdType().isEmpty() &&
                        !author.getIdValue().isEmpty() &&
                        author.getAffiliation() != null &&
                        !author.getAffiliation().getDisplayValue().isEmpty()) {
                    if (author.getIdType().equals("ORCID")) {
                        contributorsElement.append("<ORCID>").append("https://orcid.org/").append(author.getIdValue()).append("</ORCID>");
                    }
                    if (author.getIdType().equals("ISNI")) {
                        contributorsElement.append("<ISNI>").append(author.getIdValue()).append("</ISNI>");
                    }
                    if (author.getIdType().equals("LCNA")) {
                        contributorsElement.append("<LCNA>").append(author.getIdValue()).append("</LCNA>");
                    }
                }

                contributorsElement.append("</person_name>");
            }
            contributorsElement.append("</contributors>");

        } else {
            contributorsElement.append("<contributors><person_name>")
                    .append(AbstractPidProvider.UNAVAILABLE)
                    .append("</person_name></contributors>");
        }

        datasetElement.append(contributorsElement);

        datasetElement.append("<titles><title>")
                .append(this.title)
                .append("</title></titles>");

        datasetElement.append("<doi_data><doi>")
                .append(this.identifier)
                .append("</doi><resource>")
                .append(this.baseUrl).append("/dataset.xhtml?persistentId=doi:").append(this.identifier)
                .append("</resource></doi_data>");

        datasetElement.append("</dataset>");
        xmlMetadata = xmlMetadata.replace("${datasets}", datasetElement.toString());
        return xmlMetadata;
    }

    public static String getTemplate() {
        return template;
    }

    public static void setTemplate(String template) {
        CrossRefMetadataTemplate.template = template;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(String depositor) {
        this.depositor = depositor;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDepositorEmail() {
        return depositorEmail;
    }

    public void setDepositorEmail(String depositorEmail) {
        this.depositorEmail = depositorEmail;
    }

    public String getDatabaseTitle() {
        return databaseTitle;
    }

    public void setDatabaseTitle(String databaseTitle) {
        this.databaseTitle = databaseTitle;
    }
}

class CrossRefFileUtil {

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Fail to close InputStream");
            }
        }
    }

    public static String readAndClose(InputStream inStream, String encoding) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        int cnt;
        while ((cnt = inStream.read(buf)) >= 0) {
            outStream.write(buf, 0, cnt);
        }
        return outStream.toString(encoding);
    }
}

