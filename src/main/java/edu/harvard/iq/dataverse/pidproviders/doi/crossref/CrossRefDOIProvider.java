package edu.harvard.iq.dataverse.pidproviders.doi.crossref;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossRefDOIProvider extends AbstractDOIProvider {
    private static final Logger logger = Logger.getLogger(CrossRefDOIProvider.class.getCanonicalName());

    public static final String TYPE = "crossref";

    CrossRefDOIRegisterService crossRefDOIRegisterService;

    public CrossRefDOIProvider(String id, String label, String providerAuthority, String providerShoulder, String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList,
                               String url, String apiUrl, String username, String password, String depositor, String depositorEmail) {
        super(id, label, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat,
                managedList, excludedList);

        crossRefDOIRegisterService = new CrossRefDOIRegisterService(url, apiUrl, username, password, depositor, depositorEmail);
    }

    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) {
        logger.info("CrossRef alreadyRegistered");
        if (pid == null || pid.asString().isEmpty()) {
            logger.fine("No identifier sent.");
            return false;
        }
        boolean alreadyExists;
        String identifier = pid.asString();
        try {
            alreadyExists = crossRefDOIRegisterService.testDOIExists(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "alreadyExists failed");
            return false;
        }
        return alreadyExists;
    }

    @Override
    public boolean registerWhenPublished() {
        return true;
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of("CrossRef", "https://status.crossref.org/");
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.info("CrossRef createIdentifier");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generatePid(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        try {
            String retString = crossRefDOIRegisterService.reserveIdentifier(identifier, dvObject);
            logger.log(Level.FINE, "CrossRef create DOI identifier retString : " + retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "CrossRef Identifier not created: create failed", e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.info("CrossRef getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata = crossRefDOIRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed", e);
        }
        return metadata;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        logger.info("CrossRef modifyIdentifier");
        String identifier = getIdentifier(dvObject);
        try {
            crossRefDOIRegisterService.modifyIdentifier(identifier, dvObject);
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed", e);
            throw e;
        }
        return identifier;
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        logger.info("CrossRef deleteIdentifier");
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.info("CrossRef updateIdentifierStatus");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generatePid(dvObject);
        }
        String identifier = getIdentifier(dvObject);

        try {
            crossRefDOIRegisterService.reserveIdentifier(identifier, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getProviderType() {
        return TYPE;
    }
}
