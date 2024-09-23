package edu.harvard.iq.dataverse.pidproviders.doi;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;


public abstract class AbstractDOIProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(AbstractDOIProvider.class.getCanonicalName());

    public static final String DOI_PROTOCOL = "doi";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String HTTP_DOI_RESOLVER_URL = "http://doi.org/";
    public static final String DXDOI_RESOLVER_URL = "https://dx.doi.org/";
    public static final String HTTP_DXDOI_RESOLVER_URL = "http://dx.doi.org/";

    public AbstractDOIProvider(String id, String label, String providerAuthority, String providerShoulder, String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList) {
        super(id, label, DOI_PROTOCOL, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat, managedList, excludedList);
    }

    //For Unmanged provider
    public AbstractDOIProvider(String name, String label) {
        super(name, label, DOI_PROTOCOL);
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        if (pidString.startsWith(DOI_RESOLVER_URL)) {
            pidString = pidString.replace(DOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        } else if (pidString.startsWith(HTTP_DOI_RESOLVER_URL)) {
            pidString = pidString.replace(HTTP_DOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        } else if (pidString.startsWith(DXDOI_RESOLVER_URL)) {
            pidString = pidString.replace(DXDOI_RESOLVER_URL,
                    (DOI_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {

        if (!DOI_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        if (globalId!=null && !PidProvider.checkDOIAuthority(globalId.getAuthority())) {
            return null;
        }
        return globalId;
    }
    
    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {

        if (!DOI_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    public String getUrlPrefix() {
        return DOI_RESOLVER_URL;
    }

    protected String getProviderKeyName() {
        return null;
    }
    
    public String getProtocol() {
        return DOI_PROTOCOL;
    }
    
    public String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

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
            doiMetadata.setDescription(dataset.getLatestVersion().getDescriptionPlainText());
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            String fileDescription = df.getDescription();
            doiMetadata.setDescription(fileDescription == null ? "" : fileDescription);
        }

        doiMetadata.setContacts(dataset.getLatestVersion().getDatasetContacts());
        doiMetadata.setProducers(dataset.getLatestVersion().getDatasetProducers());
        doiMetadata.setTitle(dvObject.getCurrentName());
        String producerString = pidProviderService.getProducer();
        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = UNAVAILABLE;
        }
        doiMetadata.setPublisher(producerString);
        doiMetadata.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = new XmlMetadataTemplate(doiMetadata).generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

}