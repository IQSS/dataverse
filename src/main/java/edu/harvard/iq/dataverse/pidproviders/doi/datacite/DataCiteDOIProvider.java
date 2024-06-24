package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;

/**
 *
 * @author luopc
 */
public class DataCiteDOIProvider extends AbstractDOIProvider {

    private static final Logger logger = Logger.getLogger(DataCiteDOIProvider.class.getCanonicalName());

    static final String FINDABLE = "findable";     //public - published dataset versions
    static final String DRAFT = "draft";           //reserved but not findable yet - draft/unpublished datasets
    static final String REGISTERED = "registered"; //was findable once, not anymore - deaccessioned datasets
    static final String NONE = "none";             //no record - draft/unpublished datasets where the initial request to reserve has failed

    public static final String TYPE = "datacite";


    private String mdsUrl;
    private String apiUrl;
    private String username;
    private String password;
    
    private DOIDataCiteRegisterService doiDataCiteRegisterService;

    public DataCiteDOIProvider(String id, String label, String providerAuthority, String providerShoulder,
            String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList,
            String mdsUrl, String apiUrl, String username, String password) {
        super(id, label, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat, managedList,
                excludedList);
        this.mdsUrl = mdsUrl;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        doiDataCiteRegisterService = new DOIDataCiteRegisterService(mdsUrl, username, password);
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) {
        logger.log(Level.FINE, "alreadyRegistered");
        if (pid == null || pid.asString().isEmpty()) {
            logger.fine("No identifier sent.");
            return false;
        }
        boolean alreadyRegistered;
        String identifier = pid.asString();
        try {
            alreadyRegistered = doiDataCiteRegisterService.testDOIExists(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "alreadyRegistered failed");
            return false;
        }
        return alreadyRegistered;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Exception {
        logger.log(Level.FINE, "createIdentifier");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generatePid(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getMetadataForCreateIndicator(dvObject);
        metadata.put("_status", DRAFT);
        try {
            String retString = doiDataCiteRegisterService.reserveIdentifier(identifier, metadata, dvObject);
            logger.log(Level.FINE, "create DOI identifier retString : " + retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Identifier not created: create failed", e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.log(Level.FINE, "getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = doiDataCiteRegisterService.getMetadata(identifier);
            metadata.put("_status", getPidStatus(dvObject));
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed", e);
        }
        return metadata;
    }

    /**
     * Modifies the DOI metadata for a Dataset
     * 
     * @param dvObject the dvObject whose metadata needs to be modified
     * @return the Dataset identifier, or null if the modification failed
     * @throws java.lang.Exception
     */
    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        logger.log(Level.FINE, "modifyIdentifier");
        String identifier = getIdentifier(dvObject);
        try {
            Map<String, String> metadata = getIdentifierMetadata(dvObject);
            doiDataCiteRegisterService.modifyIdentifier(identifier, metadata, dvObject);
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed", e);
            throw e;
        }
        return identifier;
    }

    /*
     * Deletes a DOI if it is in DRAFT/DRAFT state or removes metadata and
     * changes it from PUBLIC/FINDABLE to REGISTERED.
     */
    @Override
    public void deleteIdentifier(DvObject dvObject) throws IOException, HttpException {
        logger.log(Level.FINE, "deleteIdentifier");
        String identifier = getIdentifier(dvObject);
        String idStatus = getPidStatus(dvObject);
        switch (idStatus) {
        case DRAFT:
            logger.log(Level.FINE, "Delete status is reserved..");
            deleteDraftIdentifier(dvObject);
            break;
        case FINDABLE:
            // if public then it has been released set to REGISTERED/unavailable and reset
            // target to n2t url
            Map<String, String> metadata = addDOIMetadataForDestroyedDataset(dvObject);
            metadata.put("_status", "registered");
            metadata.put("_target", getTargetUrl(dvObject));
            doiDataCiteRegisterService.deactivateIdentifier(identifier, metadata, dvObject);
            break;

        case REGISTERED:
        case NONE:
            // Nothing to do
        }
    }

    /**
     * Deletes DOI from the DataCite side, if possible. Only "draft" DOIs can be
     * deleted.
     */
    private void deleteDraftIdentifier(DvObject dvObject) throws IOException {

        GlobalId doi = dvObject.getGlobalId();
        /**
         * Deletes the DOI from DataCite if it can. Returns 204 if PID was deleted (only
         * possible for "draft" DOIs), 405 (method not allowed) if the DOI wasn't
         * deleted (because it's in "findable" state, for example, 404 if the DOI wasn't
         * found, and possibly other status codes such as 500 if DataCite is down.
         */

        URL url = new URL(getApiUrl() + "/dois/" + doi.getAuthority() + "/" + doi.getIdentifier());
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        String userpass = getUsername() + ":" + getPassword();
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);
        int status = connection.getResponseCode();
        if (status != HttpStatus.SC_NO_CONTENT) {
            logger.warning(
                    "Incorrect Response Status from DataCite: " + status + " : " + connection.getResponseMessage());
            throw new HttpException("Status: " + status);
        }
        logger.fine("deleteDoi status for " + doi.asString() + ": " + status);
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE, "updateIdentifierStatus");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generatePid(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        metadata.put("_status", FINDABLE);
        metadata.put("datacite.publicationyear", generateYear(dvObject));
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            doiDataCiteRegisterService.registerIdentifier(identifier, metadata, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of(getId(), "https://status.datacite.org");
    }

    @Override
    protected String getProviderKeyName() {
        return "DataCite";
    }

    @Override
    public String getProviderType() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getMdsUrl() {
        return mdsUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Method to determine the status of a dvObject's PID. It replaces keeping a
     * separate DOIDataCiteRegisterCache. We could also try to get this info from
     * DataCite directly, but it appears to not be in the xml metadata return, so it
     * would require another/different api call (possible ToDo).
     * 
     * @param dvObject - Dataset or DataFile
     * @return PID status - NONE, DRAFT, FINDABLE, or REGISTERED
     */
    String getPidStatus(DvObject dvObject) {
        String status = NONE;
        if (dvObject instanceof Dataset) {
            Dataset dataset = (Dataset) dvObject;
            // return true, if all published versions were deaccessioned
            boolean hasDeaccessionedVersions = false;
            for (DatasetVersion testDsv : dataset.getVersions()) {
                if (testDsv.isReleased()) {
                    // With any released version, we're done
                    return FINDABLE;
                }
                // Also check for draft version
                if (testDsv.isDraft()) {
                    if (dataset.isIdentifierRegistered()) {
                        status = DRAFT;
                        // Keep interating to see if there's a released version
                    }
                }
                if (testDsv.isDeaccessioned()) {
                    hasDeaccessionedVersions = true;
                    // Keep interating to see if there's a released version
                }
            }
            if (hasDeaccessionedVersions) {
                if (dataset.isIdentifierRegistered()) {
                    return REGISTERED;
                }
            }
            return status;
        } else if (dvObject instanceof DataFile) {
            DataFile df = (DataFile) dvObject;
            // return true, if all published versions were deaccessioned
            boolean isInDeaccessionedVersions = false;
            for (FileMetadata fmd : df.getFileMetadatas()) {
                DatasetVersion testDsv = fmd.getDatasetVersion();
                if (testDsv.isReleased()) {
                    // With any released version, we're done
                    return FINDABLE;
                }
                // Also check for draft version
                if (testDsv.isDraft()) {
                    if (df.isIdentifierRegistered()) {
                        status = DRAFT;
                        // Keep interating to see if there's a released/deaccessioned version
                    }
                }
                if (testDsv.isDeaccessioned()) {
                    isInDeaccessionedVersions = true;
                    // Keep interating to see if there's a released version
                }
            }
            if (isInDeaccessionedVersions) {
                if (df.isIdentifierRegistered()) {
                    return REGISTERED;
                }
            }

        }
        return status;
    }
    

    

}
