/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.util.DateUtil;
import org.apache.commons.text.StringEscapeUtils;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.DoiMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 *
 * @author luopc
 */
public class DOIDataCiteRegisterService {

    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());
    
        
    //A singleton since it, and the httpClient in it can be reused.
    private DataCiteRESTfullClient client=null;

    public DOIDataCiteRegisterService(String url, String restApiUrl, String username, String password) {
            client = new DataCiteRESTfullClient(url, restApiUrl, username, password);
    }

    /**
     * This "reserveIdentifier" method is heavily based on the
     * "registerIdentifier" method below but doesn't, this one doesn't doesn't
     * register a URL, which causes the "state" of DOI to transition from
     * "draft" to "findable". Here are some DataCite docs on the matter:
     *
     * "DOIs can exist in three states: draft, registered, and findable. DOIs
     * are in the draft state when metadata have been registered, and will
     * transition to the findable state when registering a URL." --
     * https://support.datacite.org/docs/mds-api-guide#doi-states
     */
    public String reserveIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        retString = client.postMetadata(xmlMetadata);
        
        return retString;
    }

    public String registerIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        String target = metadata.get("_target");
        
        retString = client.postMetadata(xmlMetadata);
        client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);

        return retString;
    }
    
    
    public String reRegisterIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
        // "bare identifier" is the canonical pid with the "doi:" prefix stripped
        String bareIdentifier = identifier.substring(identifier.indexOf(":") + 1);
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        String target = metadata.get("_target");
        String currentMetadata = null;
        boolean hasDifferences = false;
        try {
            currentMetadata = client.getMetadata(bareIdentifier);
            Diff myDiff = DiffBuilder.compare(xmlMetadata).withTest(currentMetadata).ignoreWhitespace().checkForSimilar()
                    .build();
            hasDifferences = myDiff.hasDifferences();
            if (hasDifferences) {
                for (Difference d : myDiff.getDifferences()) {
                    logger.fine(d.toString());
                }
            }
        } catch (RuntimeException e) {
            logger.log(Level.INFO, "DOI " + bareIdentifier + " not registered with DataCite, registering now.");
            hasDifferences = true;
        }

        if (hasDifferences) {
            retString = "metadata:\\r" + client.postMetadata(xmlMetadata) + "\\r";
        }
        String currentUrl = null;
        try {
            //May get a 204 if the DOI is still draft
            currentUrl = client.getUrl(bareIdentifier);
        } catch (RuntimeException ex) {
            logger.fine("Error getting Url for " + bareIdentifier + ": " + ex.getMessage());
        }
        if (!target.equals(currentUrl)) {
            logger.info("Updating target URL to " +  target);
            client.postUrl(bareIdentifier, target);
            retString = retString + "url:\\r" + target;

        }

        return retString;
    }


    public String deactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";

            String metadataString = getMetadataForDeactivateIdentifier(identifier, metadata, dvObject);
            retString = client.postMetadata(metadataString);
            retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));

        return retString;
    }


    private static String getPublisherFrom(DatasetVersion dsv) {
        if (!dsv.getDataset().isHarvested()) {
            return BrandingUtil.getInstallationBrandName();
        } else {
            return dsv.getDistributorName();
            // remove += [distributor] SEK 8-18-2016
        }
    }
    private static Date getDateFrom(DatasetVersion dsv) {
        Date citationDate = null;

        if (dsv.getDataset().isHarvested()) {
            citationDate = DateUtil.parseDate(dsv.getProductionDate());
            if (citationDate == null) {
                citationDate = DateUtil.parseDate(dsv.getDistributionDate());
            }
        }

        if (citationDate == null) {
            if (dsv.getCitationDate() != null) {
                citationDate = dsv.getCitationDate();
            } else if (dsv.getDataset().getCitationDate() != null) {
                citationDate = dsv.getDataset().getCitationDate();
            } else { // for drafts
                citationDate = dsv.getLastUpdateTime();
            }
        }

        if (citationDate == null) {
            //As a last resort, pick the current date
            logger.warning("Unable to find citation date for datasetversion: " + dsv.getId());
            citationDate = new Date();
        }
        return citationDate;
    }

    private static String getAuthorsString(DatasetVersion dsv) {
        List<String> authors = new ArrayList<String>();
        dsv.getDatasetAuthors().stream().forEach((author) -> {
            if (!author.isEmpty()) {
                String an = author.getName().getDisplayValue().trim();
                authors.add(an);
            }
        });
        return String.join("; ", authors);
    }

    /**
     * Builds the basic DataCite metadata required for DOI registration.
     *
     * <p>This logic belongs to the DataCite registration flow rather than (here it is was before the refactoring)
     * {@link DataCitation}, which is responsible for citation rendering.</p>
     *
     * @param datasetVersion the dataset version to extract metadata from
     * @return DataCite metadata keyed by the internal {@code datacite.*} property names
     */
    public static Map<String, String> getDataCiteMetadata(DatasetVersion dvObject) {
        Map<String, String> metadata = new HashMap<>();
        String authorString = getAuthorsString(dvObject);

        if (authorString.isEmpty()) {
            authorString = AbstractPidProvider.UNAVAILABLE;
        }
        String producerString = getPublisherFrom(dvObject);

        if (producerString.isEmpty()) {
            producerString =  AbstractPidProvider.UNAVAILABLE;
        }

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", dvObject.getTitle());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationdate", getDateFrom(dvObject).toInstant().toString());
        metadata.put("datacite.publicationyear",  new SimpleDateFormat("yyyy").format(getDateFrom(dvObject)));
        return metadata;
    }


    /**
     * Generates DataCite XML metadata for the given Dataverse object.
     *
     * <p>For datasets, the required DataCite metadata is derived from the latest
     * dataset version before delegating to the metadata generation logic. Same behavior as before the code move from {@link DataCitation} </p>
     *
     * @param identifier the persistent identifier
     * @param dvObject the Dataverse object to generate metadata for
     * @return DataCite XML metadata
     */
    public static String getMetadataFromDvObject(String identifier, DvObject dvObject) {
        Map<String, String> metadata = new HashMap<>();
        if(dvObject.isInstanceofDataset()) {
            metadata = getDataCiteMetadata( ((Dataset) dvObject).getLatestVersion());
        }
        return getMetadataFromDvObject(identifier,metadata, dvObject);
    }
        public static String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

        Dataset dataset = null;

        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else {
            dataset = (Dataset) dvObject.getOwner();
        }

        DoiMetadata doiMetadata = new DoiMetadata();
        doiMetadata.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        doiMetadata.setCreators(Arrays.asList(metadata.get("datacite.creator").split("; ")));
        doiMetadata.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            //While getDescriptionPlainText strips < and > from HTML, it leaves '&' (at least so we need to xml escape as well
            String description = StringEscapeUtils.escapeXml10(dataset.getLatestVersion().getDescriptionPlainText());
            if (description.isEmpty() || description.equals(DatasetField.NA_VALUE)) {
                description = AbstractPidProvider.UNAVAILABLE;
            }
            doiMetadata.setDescription(description);
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            //Note: File metadata is not escaped like dataset metadata is, so adding an xml escape here.
            //This could/should be removed if the datafile methods add escaping
            String fileDescription = StringEscapeUtils.escapeXml10(df.getDescription());
            doiMetadata.setDescription(fileDescription == null ? AbstractPidProvider.UNAVAILABLE : fileDescription);
        }

        doiMetadata.setContacts(dataset.getLatestVersion().getDatasetContacts());
        doiMetadata.setProducers(dataset.getLatestVersion().getDatasetProducers());
        String title = dvObject.getCurrentName();
        if(dvObject.isInstanceofDataFile()) {
            //Note file title is not currently escaped the way the dataset title is, so adding it here.
            title = StringEscapeUtils.escapeXml10(title);
        }
        
        if (title.isEmpty() || title.equals(DatasetField.NA_VALUE)) {
            title = AbstractPidProvider.UNAVAILABLE;
        }
        
        doiMetadata.setTitle(title);
        String producerString = BrandingUtil.getInstallationBrandName();
        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = AbstractPidProvider.UNAVAILABLE;
        }
        doiMetadata.setPublisher(producerString);
        doiMetadata.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = new XmlMetadataTemplate(doiMetadata).generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public static String getMetadataForDeactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) {

        DoiMetadata doiMetadata = new DoiMetadata();
        
        doiMetadata.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        doiMetadata.setCreators(Arrays.asList(metadata.get("datacite.creator").split("; ")));

        doiMetadata.setDescription(AbstractPidProvider.UNAVAILABLE);

        String title =metadata.get("datacite.title");
        
        System.out.print("Map metadata title: "+ metadata.get("datacite.title"));
        
        doiMetadata.setAuthors(null);
        
        doiMetadata.setTitle(title);
        String producerString = AbstractPidProvider.UNAVAILABLE;

        doiMetadata.setPublisher(producerString);
        doiMetadata.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = new XmlMetadataTemplate(doiMetadata).generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public String modifyIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject)
            throws IOException {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        logger.fine("XML to send to DataCite: " + xmlMetadata);

        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
        switch (status) {
        case DataCiteDOIProvider.DRAFT:
            // draft DOIs aren't currently being updated after every edit - ToDo - should
            // this be changed or made optional?
            retString = "success to reserved " + identifier;
            break;
        case DataCiteDOIProvider.FINDABLE:
            try {
                retString = client.postMetadata(xmlMetadata);
                client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (RuntimeException rte) {
                logger.log(Level.SEVERE, "Error creating DOI at DataCite: {0}", rte.getMessage());
                logger.log(Level.SEVERE, "Exception", rte);
            }
            break;
        case DataCiteDOIProvider.REGISTERED:
            retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
            break;
        }
        return retString;
    }

    public boolean testDOIExists(String identifier) {
        boolean doiExists;
        try {
            doiExists = client.testDOIExists(identifier.substring(identifier.indexOf(":") + 1));
        } catch (Exception e) {
            logger.log(Level.INFO, identifier, e);
            return false;
        }
        return doiExists;
    }

    Map<String, String> getMetadata(String identifier) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        try {
            String xmlMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":") + 1));
            DoiMetadata doiMetadata = new DoiMetadata();
            doiMetadata.parseDataCiteXML(xmlMetadata);
            metadata.put("datacite.creator", String.join("; ", doiMetadata.getCreators()));
            metadata.put("datacite.title", doiMetadata.getTitle());
            metadata.put("datacite.publisher", doiMetadata.getPublisher());
            metadata.put("datacite.publicationyear", doiMetadata.getPublisherYear());
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }
}
