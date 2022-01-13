package edu.harvard.iq.dataverse.globalid;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author skraffmiller
 */
@Stateless
public class DOIEZIdServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.index.DOIEZIdServiceBean");

    private EZIDService ezidService;
    private String baseURLString;

    // -------------------- LOGIC --------------------

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyExists(DvObject dvObject) throws Exception {
        if (dvObject == null) {
            logger.severe("Null DvObject sent to alreadyExists().");
            return false;
        }
        return alreadyExists(dvObject.getGlobalId());
    }

    @Override
    public boolean alreadyExists(GlobalId pid) throws Exception {
        logger.log(Level.FINE, "alreadyExists");
        try {
            Map<String, String> result = ezidService.getMetadata(pid.asString());
            return result != null && !result.isEmpty();
        } catch (EZIDException e) {
            // No such identifier is treated as an exception
            // but if that is the case then we want to just return false
            if (pid.getIdentifier() == null || e.getLocalizedMessage().contains("no such identifier")) {
                return false;
            }
            logger.log(Level.WARNING, "alreadyExists failed"
                        + "\ngetIdentifier(dvObject) " + pid.asString()
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause " + e.getCause()
                        + "\nmessage " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.log(Level.FINE, "getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed"
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause " + e.getCause()
                        + "\nmessage " + e.getMessage());
            return metadata;
        }
        return metadata;
    }

    /**
     * Looks up the metadata for a Global Identifier
     *
     * @param protocol   the identifier system, e.g. "doi"
     * @param authority  the namespace that the authority manages in the
     *                   identifier system
     *                   identifier part
     * @param identifier the local identifier part
     * @return a Map of metadata. It is empty when the lookup failed, e.g. when
     * the identifier does not exist.
     */
    @Override
    public Map<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier) {
        logger.log(Level.FINE, "lookupMetadataFromIdentifier");
        String identifierOut = getIdentifierForLookup(protocol, authority, identifier);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = ezidService.getMetadata(identifierOut);
        } catch (EZIDException e) {
            logger.log(Level.FINE, "None existing so we can use this identifier\nidentifier: {0}", identifierOut);
            return metadata;
        }
        return metadata;
    }

    /**
     * Modifies the EZID metadata for a Dataset
     *
     * @param dvObject the Dataset whose metadata needs to be modified
     * @return the Dataset identifier, or null if the modification failed
     * @throws java.lang.Exception
     */
    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            ezidService.setMetadata(identifier, asHashMap(metadata));
            return identifier;
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "modifyMetadata failed"
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause" + e.getCause()
                        + "\nmessage " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteIdentifier(DvObject dvObject) throws Exception {
        logger.log(Level.FINE, "deleteIdentifier");
        String identifier = getIdentifier(dvObject);
        Map<String, String> doiMetadata;
        try {
            doiMetadata = ezidService.getMetadata(identifier);
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "get matadata failed cannot delete"
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause " + e.getCause()
                        + "\nmessage " + e.getMessage());
            return;
        }

        String idStatus = doiMetadata.get("_status");

        if ("reserved".equals(idStatus)) {
            logger.log(Level.INFO, "Delete status is reserved..");
            try {
                ezidService.deleteIdentifier(identifier);
            } catch (EZIDException e) {
                logger.log(Level.WARNING, "delete failed"
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause" + e.getCause()
                        + "\nmessage " + e.getMessage());
            }
        } else if ("public".equals(idStatus)) {
            // if public then it has been released set to unavaialble and reset target to n2t url
            updateIdentifierStatus(dvObject, "unavailable | withdrawn by author");
            try {
                modifyIdentifierTargetURL(dvObject);
                if (!(dvObject instanceof Dataset)) {
                    return;
                }
                Dataset dataset = (Dataset) dvObject;
                for (DataFile df : dataset.getFiles()) {
                    modifyIdentifierTargetURL(df);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception encountered: ", e);
            }
        }
    }

    @Override
    public List<String> getProviderInformation() {
        List<String> providerInfo = new ArrayList<>();
        String providerName = "EZID";
        String providerLink = baseURLString;
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.log(Level.FINE, "createIdentifier");
        if (StringUtils.isEmpty(dvObject.getIdentifier())) {
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        String objMetadata = getMetadataFromDvObject(identifier, dvObject);
        Map<String, String> dcMetadata = new HashMap<>();
        dcMetadata.put("datacite", objMetadata);
        dcMetadata.put("datacite.resourcetype", "Dataset");
        dcMetadata.put("_status", "reserved");

        try {
            String retString = ezidService.createIdentifier(identifier, asHashMap(dcMetadata));
            logger.log(Level.FINE, "create DOI identifier retString : {0}", retString);
            return retString;
        } catch (EZIDException e) {
            logger.log(Level.WARNING, "Identifier not created: create failed"
                    + "\nString " + e.toString()
                    + "\nlocalized message " + e.getLocalizedMessage()
                    + "\ncause" + e.getCause()
                    + "\nmessage " + e.getMessage()
                    + "\nidentifier: " + identifier);
            throw e;
        }
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE, "publicizeIdentifier - dvObject");
        if (!dvObject.isIdentifierRegistered()) {
            try {
                createIdentifier(dvObject);
            } catch (Throwable e) {
                return false;
            }
        }
        return updateIdentifierStatus(dvObject, "public");
    }

    // -------------------- PRIVATE --------------------

    @PostConstruct
    private void loginToEZId() {
        baseURLString = settingsService.getValueForKey(SettingsServiceBean.Key.DoiBaseUrlString);
        ezidService = new EZIDService(baseURLString);
        // get username and password from system properties
        String username = settingsService.getValueForKey(SettingsServiceBean.Key.DoiUsername);
        String password = settingsService.getValueForKey(SettingsServiceBean.Key.DoiPassword);
        try {
            ezidService.login(username, password);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean updateIdentifierStatus(DvObject dvObject, String statusIn) {
        logger.log(Level.FINE, "updateIdentifierStatus");
        String identifier = getIdentifier(dvObject);
        String objMetadata = getMetadataFromDvObject(identifier, dvObject);
        Map<String, String> dcMetadata = new HashMap<>();
        dcMetadata.put("datacite", objMetadata);
        dcMetadata.put("_status", statusIn);
        dcMetadata.put("_target", getTargetUrl(dvObject));

        try {
            // ezID API requires HashMap, not just any map.
            ezidService.setMetadata(identifier, asHashMap(dcMetadata));
            return true;

        } catch (EZIDException e) {
            logger.log(Level.WARNING, "modifyMetadata failed"
                        + "\nString " + e.toString()
                        + "\nlocalized message " + e.getLocalizedMessage()
                        + "\ncause " + e.getCause()
                        + "\nmessage " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns a HashMap with the same values as {@code map}. This can be either
     * {@code map} itself, or a new instance with the same values.
     * <p>
     * This is needed as some of the internal APIs here require HashMap, but we
     * don't want the external APIs to use an implementation class.
     *
     * @return A HashMap with the same values as {@code map}
     */
    private <T> HashMap<T, T> asHashMap(Map<T, T> map) {
        return map instanceof HashMap
                ? (HashMap<T, T>) map
                : new HashMap<>(map);
    }
}

