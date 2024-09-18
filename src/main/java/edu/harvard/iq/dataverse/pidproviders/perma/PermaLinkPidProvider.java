package edu.harvard.iq.dataverse.pidproviders.perma;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * PermaLink provider This is a minimalist permanent ID provider intended for
 * use with 'real' datasets/files where the use case none-the-less doesn't lend
 * itself to the use of DOIs or Handles, e.g. * due to cost * for a
 * catalog/archive where Dataverse has a dataset representing a dataset with
 * DOI/handle stored elsewhere
 * 
 * The initial implementation will mint identifiers locally and will provide the
 * existing page URLs (using the ?persistentID=<id> format). This will be
 * overridable by a configurable parameter to support use of an external
 * resolver.
 * 
 */
public class PermaLinkPidProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(PermaLinkPidProvider.class.getCanonicalName());

    public static final String PERMA_PROTOCOL = "perma";
    public static final String TYPE = "perma";
    public static final String SEPARATOR = "";

    // ToDo - remove
    @Deprecated
    public static final String PERMA_RESOLVER_URL = JvmSettings.PERMALINK_BASE_URL.lookupOptional("permalink")
            .orElse(SystemConfig.getDataverseSiteUrlStatic());

    
    private String separator = SEPARATOR;

    private String baseUrl;

    public PermaLinkPidProvider(String id, String label, String providerAuthority, String providerShoulder, String identifierGenerationStyle,
            String datafilePidFormat, String managedList, String excludedList, String baseUrl, String separator) {
        super(id, label, PERMA_PROTOCOL, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat,
                managedList, excludedList);
        this.baseUrl = baseUrl;
        this.separator = separator;
    }

    @Override
    public String getSeparator() {
        return separator;
    }

    @Override
    public boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) {
        // Perma doesn't manage registration, so we assume all local PIDs can be treated
        // as registered
        boolean existsLocally = !pidProviderService.isGlobalIdLocallyUnique(globalId);
        return existsLocally ? existsLocally : noProviderDefault;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of(getId(), getBaseUrl());
    }

    @Override
    public String createIdentifier(DvObject dvo) throws Throwable {
        // Call external resolver and send landing URL?
        // FWIW: Return value appears to only be used in RegisterDvObjectCommand where
        // success requires finding the dvo identifier in this string. (Also logged a
        // couple places).
        return (dvo.getGlobalId().asString());
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvo) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        return getTargetUrl(dvo);
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        // no-op
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        // Generate if needed (i.e. datafile case where we don't create/register early
        // (even with registerWhenPublished == false))
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generatePid(dvObject);
        }
        // Call external resolver and send landing URL?
        return true;
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        // ToDo - handle local PID resolver for dataset/file
        logger.info("Parsing in Perma: " + pidString);
        if (pidString.startsWith(getUrlPrefix())) {
            pidString = pidString.replace(getUrlPrefix(), (PERMA_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        logger.info("Checking Perma: " + identifierString);
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        String cleanIdentifier = PERMA_PROTOCOL + ":" + identifierString;
        // With permalinks, we have to check the sets before parsing since the permalinks in these sets could have different authority, spearator, and shoulders
        if (getExcludedSet().contains(cleanIdentifier)) {
            return null;
        }
        if(getManagedSet().contains(cleanIdentifier)) {
            /** With a variable separator that could also be empty, there is no way to determine the authority and shoulder for an unknown permalink.
             * Since knowing this split isn't relevant for permalinks except for minting, the code below just assumes the authority
             * is the first 4 characters and that the separator and the shoulder are empty.
             * If this is found to cause issues, users should be able to use a managed permalink provider as a work-around. The code here could 
             * be changed to allow default lengths for the authority, separator, and shoulder and/or to add a list of known (but unmanaged) authority, separator, shoulder combos.
             */
            if(identifierString.length() < 4) {
                return new GlobalId(protocol, "", identifierString, SEPARATOR, getUrlPrefix(),
                        getId());
            }
            return new GlobalId(protocol, identifierString.substring(0,4), identifierString.substring(4), SEPARATOR, getUrlPrefix(),
                    getId());
        }
        String identifier = null;
        if (getAuthority() != null) {
            if (identifierString.startsWith(getAuthority())) {
                identifier = identifierString.substring(getAuthority().length());
            } else {
                //Doesn't match authority
                return null;
            }
            if (identifier.startsWith(separator)) {
                identifier = identifier.substring(separator.length());
            } else {
                //Doesn't match separator
                return null;
            }
        }
        identifier = PidProvider.formatIdentifierString(identifier);
        if (PidProvider.testforNullTerminator(identifier)) {
            return null;
        }
        if(!identifier.startsWith(getShoulder())) {
            //Doesn't match shoulder
            return null;
        }
        return new GlobalId(PERMA_PROTOCOL, getAuthority(), identifier, separator, getUrlPrefix(), getId());
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {

        return getBaseUrl();
    }

    @Override
    public String getProtocol() {
        return PERMA_PROTOCOL;
    }

    @Override
    public String getProviderType() {
        return PERMA_PROTOCOL;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
