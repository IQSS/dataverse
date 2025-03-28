package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import com.beust.jcommander.Strings;

public abstract class AbstractPidProvider implements PidProvider {

    private static final Logger logger = Logger.getLogger(AbstractPidProvider.class.getCanonicalName());

    public static String UNAVAILABLE = ":unav";
    public static final String SEPARATOR = "/";

    protected PidProviderFactoryBean pidProviderService;

    private String protocol;

    private String authority = null;

    private String shoulder = null;

    private String identifierGenerationStyle = null;

    private String datafilePidFormat = null;

    protected HashSet<String> managedSet = new HashSet<String>();

    protected HashSet<String> excludedSet = new HashSet<String>();

    private String id;
    private String label;

    protected AbstractPidProvider(String id, String label, String protocol) {
        this.id = id;
        this.label = label;
        this.protocol = protocol;
    }

    protected AbstractPidProvider(String id, String label, String protocol, String authority, String shoulder,
            String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList) {
        this.id = id;
        this.label = label;
        this.protocol = protocol;
        this.authority = authority;
        this.shoulder = shoulder;
        this.identifierGenerationStyle = identifierGenerationStyle;
        this.datafilePidFormat = datafilePidFormat;
        if(!managedList.isEmpty()) {
            this.managedSet.addAll(Arrays.asList(managedList.split(",\\s")));
        }
        if(!excludedList.isEmpty()) {
            this.excludedSet.addAll(Arrays.asList(excludedList.split(",\\s")));
        }
        if (logger.isLoggable(Level.FINE)) {
            Iterator<String> iter = managedSet.iterator();
            while (iter.hasNext()) {
                logger.fine("managedSet in " + getId() + ": " + iter.next());
            }
            iter = excludedSet.iterator();
            while (iter.hasNext()) {
                logger.fine("excludedSet in " + getId() + ": " + iter.next());
            }
        }
    }

    @Override
    public Map<String, String> getMetadataForCreateIndicator(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getMetadataForCreateIndicator(DvObject)");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        metadata.put("_target", getTargetUrl(dvObjectIn));
        return metadata;
    }

    protected Map<String, String> getUpdateMetadata(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getUpdateMetadataFromDataset");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        return metadata;
    }

    protected Map<String, String> addBasicMetadata(DvObject dvObjectIn, Map<String, String> metadata) {

        String authorString = dvObjectIn.getAuthorString();
        if (authorString.isEmpty() || authorString.contains(DatasetField.NA_VALUE)) {
            authorString = UNAVAILABLE;
        }

        String producerString = pidProviderService.getProducer();

        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = UNAVAILABLE;
        }

        String titleString = dvObjectIn.getCurrentName();

        if (titleString.isEmpty() || titleString.equals(DatasetField.NA_VALUE)) {
            titleString = UNAVAILABLE;
        }

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", titleString);
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        return metadata;
    }

    protected Map<String, String> addDOIMetadataForDestroyedDataset(DvObject dvObjectIn) {
        Map<String, String> metadata = new HashMap<>();
        String authorString = UNAVAILABLE;
        String producerString = UNAVAILABLE;
        String titleString = "This item has been removed from publication";

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", titleString);
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", "9999");
        return metadata;
    }

    protected String getTargetUrl(DvObject dvObjectIn) {
        logger.log(Level.FINE, "getTargetUrl");
        return SystemConfig.getDataverseSiteUrlStatic() + dvObjectIn.getTargetUrl()
                + dvObjectIn.getGlobalId().asString();
    }

    @Override
    public String getIdentifier(DvObject dvObject) {
        GlobalId gid = dvObject.getGlobalId();
        return gid != null ? gid.asString() : null;
    }

    protected String generateYear(DvObject dvObjectIn) {
        return dvObjectIn.getYearPublishedCreated();
    }

    public Map<String, String> getMetadataForTargetURL(DvObject dvObject) {
        logger.log(Level.FINE, "getMetadataForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        return metadata;
    }

    @Override
    public boolean alreadyRegistered(DvObject dvo) throws Exception {
        if (dvo == null) {
            logger.severe("Null DvObject sent to alreadyRegistered().");
            return false;
        }
        GlobalId globalId = dvo.getGlobalId();
        if (globalId == null) {
            return false;
        }
        return alreadyRegistered(globalId, false);
    }

    public abstract boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) throws Exception;

    /*
     * ToDo: the DvObject being sent in provides partial support for the case where
     * it has a different authority/protocol than what is configured (i.e. a legacy
     * Pid that can actually be updated by the Pid account being used.) Removing
     * this now would potentially break/make it harder to handle that case prior to
     * support for configuring multiple Pid providers. Once that exists, it would be
     * cleaner to always find the PidProvider associated with the
     * protocol/authority/shoulder of the current dataset and then not pass the
     * DvObject as a param. (This would also remove calls to get the settings since
     * that would be done at construction.)
     */
    @Override
    public DvObject generatePid(DvObject dvObject) {

        if (dvObject.getProtocol() == null) {
            dvObject.setProtocol(getProtocol());
        } else {
            if (!dvObject.getProtocol().equals(getProtocol())) {
                logger.warning("The protocol of the DvObject (" + dvObject.getProtocol()
                        + ") does not match the configured protocol (" + getProtocol() + ")");
                throw new IllegalArgumentException("The protocol of the DvObject (" + dvObject.getProtocol()
                        + ") doesn't match that of the provider, id: " + getId());
            }
        }
        if (dvObject.getAuthority() == null) {
            dvObject.setAuthority(getAuthority());
        } else {
            if (!dvObject.getAuthority().equals(getAuthority())) {
                logger.warning("The authority of the DvObject (" + dvObject.getAuthority()
                        + ") does not match the configured authority (" + getAuthority() + ")");
                throw new IllegalArgumentException("The authority of the DvObject (" + dvObject.getAuthority()
                        + ") doesn't match that of the provider, id: " + getId());
            }
        }
        if (dvObject.getSeparator() == null) {
            dvObject.setSeparator(getSeparator());
        } else {
            if (!dvObject.getSeparator().equals(getSeparator())) {
                logger.warning("The separator of the DvObject (" + dvObject.getSeparator()
                        + ") does not match the configured separator (" + getSeparator() + ")");
                throw new IllegalArgumentException("The separator of the DvObject (" + dvObject.getSeparator()
                        + ") doesn't match that of the provider, id: " + getId());
            }
        }
        if (dvObject.isInstanceofDataset()) {
            dvObject.setIdentifier(generateDatasetIdentifier((Dataset) dvObject));
        } else {
            dvObject.setIdentifier(generateDataFileIdentifier((DataFile) dvObject));
        }
        return dvObject;
    }

    private String generateDatasetIdentifier(Dataset dataset) {
        String shoulder = getShoulder();

        switch (getIdentifierGenerationStyle()) {
        case "randomString":
            return generateIdentifierAsRandomString(dataset, shoulder);
        case "storedProcGenerated":
            return generateIdentifierFromStoredProcedureIndependent(dataset, shoulder);
        default:
            /* Should we throw an exception instead?? -- L.A. 4.6.2 */
            return generateIdentifierAsRandomString(dataset, shoulder);
        }
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used for
     * any other study in this Dataverse Network) also check for duplicate in EZID
     * if needed
     * 
     * @param userIdentifier
     * @param dataset
     * @return {@code true} if the identifier is unique, {@code false} otherwise.
     */
    public boolean isGlobalIdUnique(GlobalId globalId) {
        if (!pidProviderService.isGlobalIdLocallyUnique(globalId)) {
            return false; // duplication found in local database
        }

        // not in local DB, look in the persistent identifier service
        try {
            return !alreadyRegistered(globalId, false);
        } catch (Exception e) {
            // we can live with failure - means identifier not found remotely
        }

        return true;
    }

    /**
     * Parse a Persistent Id and set the protocol, authority, and identifier
     * 
     * Example 1: doi:10.5072/FK2/BYM3IW protocol: doi authority: 10.5072
     * identifier: FK2/BYM3IW
     * 
     * Example 2: hdl:1902.1/111012 protocol: hdl authority: 1902.1 identifier:
     * 111012
     *
     * @param identifierString
     * @param separator        the string that separates the authority from the
     *                         identifier.
     * @param destination      the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     *         {@code null} if parsing failed.
     */
    @Override
    public GlobalId parsePersistentId(String fullIdentifierString) {
        // Occasionally, the protocol separator character ':' comes in still
        // URL-encoded as %3A (usually as a result of the URL having been
        // encoded twice):
        fullIdentifierString = fullIdentifierString.replace("%3A", ":");

        int index1 = fullIdentifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
            String protocol = fullIdentifierString.substring(0, index1);
            GlobalId globalId = parsePersistentId(protocol, fullIdentifierString.substring(index1 + 1));
            return globalId;
        }
        logger.log(Level.INFO, "Error parsing identifier: {0}: ''<protocol>:'' not found in string",
                fullIdentifierString);
        return null;
    }

    protected GlobalId parsePersistentId(String protocol, String identifierString) {
        String authority;
        String identifier;
        if (identifierString == null) {
            return null;
        }
        int index = identifierString.indexOf(getSeparator());
        if (index > 0 && (index + 1) < identifierString.length()) {
            // '/' found with one or more characters
            // before and after it
            // Strip any whitespace, ; and ' from authority (should finding them cause a
            // failure instead?)
            authority = PidProvider.formatIdentifierString(identifierString.substring(0, index));

            if (PidProvider.testforNullTerminator(authority)) {
                return null;
            }
            identifier = PidProvider.formatIdentifierString(identifierString.substring(index + 1));
            if (PidProvider.testforNullTerminator(identifier)) {
                return null;
            }

        } else {
            logger.log(Level.INFO, "Error parsing identifier: {0}: '':<authority>/<identifier>'' not found in string",
                    identifierString);
            return null;
        }
        return parsePersistentId(protocol, authority, identifier);
    }

    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        return parsePersistentId(protocol, authority, identifier, false);
    }
    
    public GlobalId parsePersistentId(String protocol, String authority, String identifier, boolean isCaseInsensitive) {
        logger.fine("Parsing: " + protocol + ":" + authority + getSeparator() + identifier + " in " + getId());
        if (!PidProvider.isValidGlobalId(protocol, authority, identifier)) {
            return null;
        }
        if(isCaseInsensitive) {
            identifier = identifier.toUpperCase();
        }
        // Check authority/identifier if this is a provider that manages specific
        // identifiers
        // /is not one of the unmanaged providers that has null authority
        if (getAuthority() != null) {

            String cleanIdentifier = protocol + ":" + authority + getSeparator() + identifier;
            /*
             * Test if this provider manages this identifier - return null if it does not.
             * It does match if ((the identifier's authority and shoulder match the
             * provider's), or the identifier is in the managed set), and, in either case,
             * the identifier is not in the excluded set.
             */
            logger.fine("clean pid in " + getId() + ": " + cleanIdentifier);
            logger.fine("managed in " + getId() + ": " + getManagedSet().contains(cleanIdentifier));
            logger.fine("excluded from " + getId() + ": " + getExcludedSet().contains(cleanIdentifier));

            if (!(((authority.equals(getAuthority()) && identifier.startsWith(getShoulder().toUpperCase()))
                    || getManagedSet().contains(cleanIdentifier)) && !getExcludedSet().contains(cleanIdentifier))) {
                return null;
            }
        }
        return new GlobalId(protocol, authority, identifier, getSeparator(), getUrlPrefix(), getId());
    }

    public String getSeparator() {
        // The standard default
        return SEPARATOR;
    }

    private String generateDataFileIdentifier(DataFile datafile) {
        String doiDataFileFormat = getDatafilePidFormat();

        String prepend = "";
        if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.DEPENDENT.toString())) {
            // If format is dependent then pre-pend the dataset identifier
            prepend = datafile.getOwner().getIdentifier() + SEPARATOR;
            datafile.setProtocol(datafile.getOwner().getProtocol());
            datafile.setAuthority(datafile.getOwner().getAuthority());
        } else {
            // If there's a shoulder prepend independent identifiers with it
            prepend = getShoulder();
            datafile.setProtocol(getProtocol());
            datafile.setAuthority(getAuthority());
        }

        switch (getIdentifierGenerationStyle()) {
        case "randomString":
            return generateIdentifierAsRandomString(datafile, prepend);
        case "storedProcGenerated":
            if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.INDEPENDENT.toString())) {
                return generateIdentifierFromStoredProcedureIndependent(datafile, prepend);
            } else {
                return generateIdentifierFromStoredProcedureDependent(datafile, prepend);
            }
        default:
            /* Should we throw an exception instead?? -- L.A. 4.6.2 */
            return generateIdentifierAsRandomString(datafile, prepend);
        }
    }

    /*
     * This method checks locally for a DvObject with the same PID and if that is
     * OK, checks with the PID service.
     * 
     * @param dvo - the object to check (ToDo - get protocol/authority from this
     * PidProvider object)
     * 
     * @param prepend - for Datasets, this is always the shoulder, for DataFiles, it
     * could be the shoulder or the parent Dataset identifier
     */
    private String generateIdentifierAsRandomString(DvObject dvo, String prepend) {
        String identifier = null;
        do {
            identifier = prepend + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (!isGlobalIdUnique(new GlobalId(dvo.getProtocol(), dvo.getAuthority(), identifier, this.getSeparator(),
                this.getUrlPrefix(), this.getId())));

        return identifier;
    }

    /*
     * This method checks locally for a DvObject with the same PID and if that is
     * OK, checks with the PID service.
     * 
     * @param dvo - the object to check (ToDo - get protocol/authority from this
     * PidProvider object)
     * 
     * @param prepend - for Datasets, this is always the shoulder, for DataFiles, it
     * could be the shoulder or the parent Dataset identifier
     */

    private String generateIdentifierFromStoredProcedureIndependent(DvObject dvo, String prepend) {
        String identifier;
        do {
            String identifierFromStoredProcedure = pidProviderService.generateNewIdentifierByStoredProcedure();
            // some diagnostics here maybe - is it possible to determine that it's failing
            // because the stored procedure hasn't been created in the database?
            if (identifierFromStoredProcedure == null) {
                return null;
            }
            identifier = prepend + identifierFromStoredProcedure;
        } while (!isGlobalIdUnique(new GlobalId(dvo.getProtocol(), dvo.getAuthority(), identifier, this.getSeparator(),
                this.getUrlPrefix(), this.getId())));

        return identifier;
    }

    /*
     * This method is only used for DataFiles with DEPENDENT Pids. It is not for
     * Datasets
     * 
     */
    private String generateIdentifierFromStoredProcedureDependent(DataFile datafile, String prepend) {
        String identifier;
        Long retVal;
        retVal = Long.valueOf(0L);
        // ToDo - replace loops with one lookup for largest entry? (the do loop runs
        // ~n**2/2 calls). The check for existingIdentifiers means this is mostly a
        // local loop now, versus involving db or PidProvider calls, but still...)

        // This will catch identifiers already assigned in the current transaction (e.g.
        // in FinalizeDatasetPublicationCommand) that haven't been committed to the db
        // without having to make a call to the PIDProvider
        Set<String> existingIdentifiers = new HashSet<String>();
        List<DataFile> files = datafile.getOwner().getFiles();
        for (DataFile f : files) {
            existingIdentifiers.add(f.getIdentifier());
        }

        do {
            retVal++;
            identifier = prepend + retVal.toString();

        } while (existingIdentifiers.contains(identifier) || !isGlobalIdUnique(new GlobalId(datafile.getProtocol(),
                datafile.getAuthority(), identifier, this.getSeparator(), this.getUrlPrefix(), this.getId())));

        return identifier;
    }


    @Override
    public boolean canManagePID() {
        // The default expectation is that PID providers are configured to manage some
        // set (i.e. based on protocol/authority/shoulder) of PIDs
        return true;
    }

    @Override
    public void setPidProviderServiceBean(PidProviderFactoryBean pidProviderServiceBean) {
        this.pidProviderService = pidProviderServiceBean;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getShoulder() {
        return shoulder;
    }

    @Override
    public String getIdentifierGenerationStyle() {
        return identifierGenerationStyle;
    }

    @Override
    public String getDatafilePidFormat() {
        return datafilePidFormat;
    }

    @Override
    public Set<String> getManagedSet() {
        return managedSet;
    }

    @Override
    public Set<String> getExcludedSet() {
        return excludedSet;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    /**
     * True if this provider can manage PIDs in general, this pid is not in the
     * managedSet (meaning it is managed but the provider does not generally manage
     * it's protocol/authority/separator/shoulder) and either this provider is the
     * same as the pid's or we're allowed to create INDEPENDENT pids. The latter
     * clause covers the potential case where the effective pid provider/generator
     * for the dataset is set to a different one that handles the dataset's pid
     * itself. In this case, we can create file PIDs if they are independent.
     * 
     * @param pid - the related pid to check
     * @return true if this provider can manage PIDs like the one supplied
     */
    public boolean canCreatePidsLike(GlobalId pid) {
        return canManagePID() && !managedSet.contains(pid.asString())
                && (getIdentifierGenerationStyle().equals("INDEPENDENT") || getId().equals(pid.getProviderId()));
    }
    
    @Override
    public JsonObject getProviderSpecification() {
        JsonObjectBuilder providerSpecification = Json.createObjectBuilder();
        providerSpecification.add("id", id);
        providerSpecification.add("label", label);
        providerSpecification.add("protocol", protocol);
        providerSpecification.add("authority", authority);
        providerSpecification.add("separator", getSeparator());
        providerSpecification.add("shoulder", shoulder);
        providerSpecification.add("identifierGenerationStyle", identifierGenerationStyle);
        providerSpecification.add("datafilePidFormat", datafilePidFormat);
        providerSpecification.add("managedSet", Strings.join(",", managedSet.toArray()));
        providerSpecification.add("excludedSet", Strings.join(",", excludedSet.toArray()));
        return providerSpecification.build();
    }
    
    @Override
    public boolean updateIdentifier(DvObject dvObject) {
        //By default, these are the same
        return publicizeIdentifier(dvObject);
    }
    
    /** By default, this is not implemented */
    @Override
    public JsonObject getCSLJson(DatasetVersion datasetVersion) {
        return new DataCitation(datasetVersion).getCSLJsonFormat();
    }
}
