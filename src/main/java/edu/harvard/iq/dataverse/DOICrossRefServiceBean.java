package edu.harvard.iq.dataverse;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DOICrossRefServiceBean extends DOIServiceBean {
    private static final Logger logger = Logger.getLogger(DOICrossRefServiceBean.class.getCanonicalName());

    @EJB
    DOICrossRefRegisterService doiCrossRefRegisterService;


    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) throws Exception {
        logger.info("CrossRef alreadyRegistered");
        if (pid == null || pid.asString().isEmpty()) {
            logger.fine("No identifier sent.");
            return false;
        }
        boolean alreadyExists;
        String identifier = pid.asString();
        try {
            alreadyExists = doiCrossRefRegisterService.testDOIExists(identifier);
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
    protected String getProviderKeyName() {
        return "CrossRef";
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.info("CrossRef createIdentifier");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        try {
            String retString = doiCrossRefRegisterService.reserveIdentifier(identifier, dvObject);
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
            metadata = doiCrossRefRegisterService.getMetadata(identifier);
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
            doiCrossRefRegisterService.modifyIdentifier(identifier, dvObject);
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
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);

        try {
            doiCrossRefRegisterService.reserveIdentifier(identifier, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
            return false;
        }
    }
}
