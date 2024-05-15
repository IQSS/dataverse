/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;

/**
 *
 * @author luopc
 */
public class DOIDataCiteRegisterService {

    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());
    
        
    //A singleton since it, and the httpClient in it can be reused.
    private DataCiteRESTfullClient client=null;

    public DOIDataCiteRegisterService(String url, String username, String password) {
            client = new DataCiteRESTfullClient(url, username, password);
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

    public String deactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
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

        XmlMetadataTemplate metadataTemplate = new XmlMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Arrays.asList(metadata.get("datacite.creator").split("; ")));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            //While getDescriptionPlainText strips < and > from HTML, it leaves '&' (at least so we need to xml escape as well
            String description = StringEscapeUtils.escapeXml10(dataset.getLatestVersion().getDescriptionPlainText());
            if (description.isEmpty() || description.equals(DatasetField.NA_VALUE)) {
                description = AbstractPidProvider.UNAVAILABLE;
            }
            metadataTemplate.setDescription(description);
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            //Note: File metadata is not escaped like dataset metadata is, so adding an xml escape here.
            //This could/should be removed if the datafile methods add escaping
            String fileDescription = StringEscapeUtils.escapeXml10(df.getDescription());
            metadataTemplate.setDescription(fileDescription == null ? AbstractPidProvider.UNAVAILABLE : fileDescription);
        }

        metadataTemplate.setContacts(dataset.getLatestVersion().getDatasetContacts());
        metadataTemplate.setProducers(dataset.getLatestVersion().getDatasetProducers());
        String title = dvObject.getCurrentName();
        if(dvObject.isInstanceofDataFile()) {
            //Note file title is not currently escaped the way the dataset title is, so adding it here.
            title = StringEscapeUtils.escapeXml10(title);
        }
        
        if (title.isEmpty() || title.equals(DatasetField.NA_VALUE)) {
            title = AbstractPidProvider.UNAVAILABLE;
        }
        
        metadataTemplate.setTitle(title);
        String producerString = BrandingUtil.getRootDataverseCollectionName();
        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = AbstractPidProvider.UNAVAILABLE;
        }
        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public static String getMetadataForDeactivateIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) {

        XmlMetadataTemplate metadataTemplate = new XmlMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Arrays.asList(metadata.get("datacite.creator").split("; ")));

        metadataTemplate.setDescription(AbstractPidProvider.UNAVAILABLE);

        String title =metadata.get("datacite.title");
        
        System.out.print("Map metadata title: "+ metadata.get("datacite.title"));
        
        metadataTemplate.setAuthors(null);
        
        metadataTemplate.setTitle(title);
        String producerString = AbstractPidProvider.UNAVAILABLE;

        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
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
            XmlMetadataTemplate template = new XmlMetadataTemplate(xmlMetadata);
            metadata.put("datacite.creator", String.join("; ", template.getCreators()));
            metadata.put("datacite.title", template.getTitle());
            metadata.put("datacite.publisher", template.getPublisher());
            metadata.put("datacite.publicationyear", template.getPublisherYear());
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }
}
