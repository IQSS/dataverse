package edu.harvard.iq.dataverse.util.json;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.api.dto.DataverseDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroup;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepData;
import org.apache.commons.validator.routines.DomainValidator;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

/**
 * Parses JSON objects into domain objects.
 *
 * @author michael
 */
public class JsonParser {

    private static final Logger logger = Logger.getLogger(JsonParser.class.getCanonicalName());

    DatasetFieldServiceBean datasetFieldSvc;
    MetadataBlockServiceBean blockService;
    SettingsServiceBean settingsService;
    LicenseServiceBean licenseService;
    DatasetTypeServiceBean datasetTypeService;
    HarvestingClient harvestingClient = null;
    boolean allowHarvestingMissingCVV = false;

    /**
     * if lenient, we will accept alternate spellings for controlled vocabulary values
     */
    boolean lenient = false;

    @Deprecated
    public JsonParser(DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService, SettingsServiceBean settingsService) {
        this.datasetFieldSvc = datasetFieldSvc;
        this.blockService = blockService;
        this.settingsService = settingsService;
    }

    public JsonParser(DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService, SettingsServiceBean settingsService, LicenseServiceBean licenseService, DatasetTypeServiceBean datasetTypeService) {
        this(datasetFieldSvc, blockService, settingsService, licenseService, datasetTypeService, null);
    }

    public JsonParser(DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService, SettingsServiceBean settingsService, LicenseServiceBean licenseService, DatasetTypeServiceBean datasetTypeService, HarvestingClient harvestingClient) {
        this.datasetFieldSvc = datasetFieldSvc;
        this.blockService = blockService;
        this.settingsService = settingsService;
        this.licenseService = licenseService;
        this.datasetTypeService = datasetTypeService;
        this.harvestingClient = harvestingClient;
        this.allowHarvestingMissingCVV = harvestingClient != null && harvestingClient.getAllowHarvestingMissingCVV();
    }

    public JsonParser() {
        this( null,null,null );
    }

    public boolean isLenient() {
        return lenient;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public Dataverse parseDataverse(JsonObject jobj) throws JsonParseException {
        Dataverse dv = new Dataverse();

        /**
         * @todo Instead of this getMandatoryString method we should run the
         * String through ConstraintValidator. See EMailValidatorTest and
         * EMailValidator for examples. That way we can check not only if it's
         * required or not but other bean validation rules such as "must match
         * this regex".
         */
        dv.setAlias(getMandatoryString(jobj, "alias"));
        dv.setName(getMandatoryString(jobj, "name"));
        dv.setDescription(jobj.getString("description", null));
        dv.setPermissionRoot(jobj.getBoolean("permissionRoot", false));
        dv.setFacetRoot(jobj.getBoolean("facetRoot", false));
        dv.setAffiliation(jobj.getString("affiliation", null));

        if (jobj.containsKey("dataverseContacts")) {
            JsonArray dvContacts = jobj.getJsonArray("dataverseContacts");
            int i = 0;
            List<DataverseContact> dvContactList = new LinkedList<>();
            for (JsonValue jsv : dvContacts) {
                DataverseContact dvc = new DataverseContact(dv);
                dvc.setContactEmail(getMandatoryString((JsonObject) jsv, "contactEmail"));
                dvc.setDisplayOrder(i++);
                dvContactList.add(dvc);
            }
            dv.setDataverseContacts(dvContactList);
        }

        if (jobj.containsKey("theme")) {
            DataverseTheme theme = parseDataverseTheme(jobj.getJsonObject("theme"));
            dv.setDataverseTheme(theme);
            theme.setDataverse(dv);
        }

        dv.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED); // default
        String receivedDataverseType = jobj.getString("dataverseType", null);
        if (receivedDataverseType != null) {
            Arrays.stream(Dataverse.DataverseType.values())
                    .filter(type -> type.name().equals(receivedDataverseType))
                    .findFirst()
                    .ifPresent(dv::setDataverseType);
        }

        if (jobj.containsKey("filePIDsEnabled")) {
            dv.setFilePIDsEnabled(jobj.getBoolean("filePIDsEnabled"));
        }
        if (jobj.containsKey("requireFilesToPublishDataset")) {
            dv.setRequireFilesToPublishDataset(jobj.getBoolean("requireFilesToPublishDataset"));
        }

        /*  We decided that subject is not user set, but gotten from the subject of the dataverse's
            datasets - leavig this code in for now, in case we need to go back to it at some point

        if (jobj.containsKey("dataverseSubjects")) {
            List<ControlledVocabularyValue> dvSubjectList = new LinkedList<>();
            DatasetFieldType subjectType = datasetFieldSvc.findByName(DatasetFieldConstant.subject);
            List<JsonString> subjectList = jobj.getJsonArray("dataverseSubjects").getValuesAs(JsonString.class);
            if (subjectList.size() > 0) {
                // check first value for "all"
                if (subjectList.get(0).getString().trim().toLowerCase().equals("all")) {
                    dvSubjectList.addAll(subjectType.getControlledVocabularyValues());
                } else {
                    for (JsonString subject : subjectList) {
                        ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectType, subject.getString(),lenient);
                        if (cvv != null) {
                            dvSubjectList.add(cvv);
                        } else {
                            throw new JsonParseException("Value '" + subject.getString() + "' does not exist in type '" + subjectType.getName() + "'");
                        }
                    }
                }
            }
            dv.setDataverseSubjects(dvSubjectList);
        }
        */

        return dv;
    }

    public DataverseDTO parseDataverseDTO(JsonObject jsonObject) throws JsonParseException {
        DataverseDTO dataverseDTO = new DataverseDTO();

        setDataverseDTOPropertyIfPresent(jsonObject, "alias", dataverseDTO::setAlias);
        setDataverseDTOPropertyIfPresent(jsonObject, "name", dataverseDTO::setName);
        setDataverseDTOPropertyIfPresent(jsonObject, "description", dataverseDTO::setDescription);
        setDataverseDTOPropertyIfPresent(jsonObject, "affiliation", dataverseDTO::setAffiliation);

        String dataverseType = jsonObject.getString("dataverseType", null);
        if (dataverseType != null) {
            Arrays.stream(Dataverse.DataverseType.values())
                    .filter(type -> type.name().equals(dataverseType))
                    .findFirst()
                    .ifPresent(dataverseDTO::setDataverseType);
        }

        if (jsonObject.containsKey("dataverseContacts")) {
            JsonArray dvContacts = jsonObject.getJsonArray("dataverseContacts");
            List<DataverseContact> contacts = new ArrayList<>();
            for (int i = 0; i < dvContacts.size(); i++) {
                JsonObject contactObj = dvContacts.getJsonObject(i);
                DataverseContact contact = new DataverseContact();
                contact.setContactEmail(getMandatoryString(contactObj, "contactEmail"));
                contact.setDisplayOrder(i);
                contacts.add(contact);
            }
            dataverseDTO.setDataverseContacts(contacts);
        }

        return dataverseDTO;
    }

    private void setDataverseDTOPropertyIfPresent(JsonObject jsonObject, String key, Consumer<String> setter) {
        String value = jsonObject.getString(key, null);
        if (value != null) {
            setter.accept(value);
        }
    }

    public DataverseTheme parseDataverseTheme(JsonObject obj) {

        DataverseTheme theme = new DataverseTheme();

        if (obj.containsKey("backgroundColor")) {
            theme.setBackgroundColor(obj.getString("backgroundColor", null));
        }
        if (obj.containsKey("linkColor")) {
            theme.setLinkColor(obj.getString("linkColor", null));
        }
        if (obj.containsKey("linkUrl")) {
            theme.setLinkUrl(obj.getString("linkUrl", null));
        }
        if (obj.containsKey("logo")) {
            theme.setLogo(obj.getString("logo", null));
        }
        if (obj.containsKey("logoAlignment")) {
            String align = obj.getString("logoAlignment");
            if (align.equalsIgnoreCase("left")) {
                theme.setLogoAlignment(DataverseTheme.Alignment.LEFT);
            }
            if (align.equalsIgnoreCase("right")) {
                theme.setLogoAlignment(DataverseTheme.Alignment.RIGHT);
            }
            if (align.equalsIgnoreCase("center")) {
                theme.setLogoAlignment(DataverseTheme.Alignment.CENTER);
            }
        }
        if (obj.containsKey("logoBackgroundColor")) {
            theme.setLogoBackgroundColor(obj.getString("logoBackgroundColor", null));
        }
        if (obj.containsKey("logoFormat")) {
            String format = obj.getString("logoFormat");
            if (format.equalsIgnoreCase("square")) {
                theme.setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
            }
            if (format.equalsIgnoreCase("rectangle")) {
                theme.setLogoFormat(DataverseTheme.ImageFormat.RECTANGLE);
            }
        }
        if (obj.containsKey("tagline")) {
            theme.setTagline(obj.getString("tagline", null));
        }
        if (obj.containsKey("textColor")) {
            theme.setTextColor(obj.getString("textColor", null));
        }

        return theme;
    }

    private static <T> T getMandatoryField(JsonObject jobj, String name, Function<String, T> getter) throws JsonParseException {
        if (jobj.containsKey(name)) {
            return getter.apply(name);
        }
        throw new JsonParseException("Field '" + name + "' is mandatory");
    }

    private static String getMandatoryString(JsonObject jobj, String name) throws JsonParseException {
        return getMandatoryField(jobj, name, jobj::getString);
    }

    private static Boolean getMandatoryBoolean(JsonObject jobj, String name) throws JsonParseException {
        return getMandatoryField(jobj, name, jobj::getBoolean);
    }

    public IpGroup parseIpGroup(JsonObject obj) {
        IpGroup retVal = new IpGroup();

        if (obj.containsKey("id")) {
            retVal.setId(Long.valueOf(obj.getInt("id")));
        }
        retVal.setDisplayName(obj.getString("name", null));
        retVal.setDescription(obj.getString("description", null));
        retVal.setPersistedGroupAlias(obj.getString("alias", null));

        if ( obj.containsKey("ranges") ) {
            obj.getJsonArray("ranges").stream()
                    .filter( jv -> jv.getValueType()==JsonValue.ValueType.ARRAY )
                    .map( jv -> (JsonArray)jv )
                    .forEach( rr -> {
                        retVal.add(
                            IpAddressRange.make(IpAddress.valueOf(rr.getString(0)),
                                                IpAddress.valueOf(rr.getString(1))));
            });
        }
        if ( obj.containsKey("addresses") ) {
            obj.getJsonArray("addresses").stream()
                    .map( jsVal -> IpAddress.valueOf(((JsonString)jsVal).getString()) )
                    .map( addr -> IpAddressRange.make(addr, addr) )
                    .forEach( retVal::add );
        }

        return retVal;
    }

    public MailDomainGroup parseMailDomainGroup(JsonObject obj) throws JsonParseException {
        MailDomainGroup grp = new MailDomainGroup();

        if (obj.containsKey("id")) {
            grp.setId(obj.getJsonNumber("id").longValue());
        }
        grp.setDisplayName(getMandatoryString(obj, "name"));
        grp.setDescription(obj.getString("description", null));
        grp.setPersistedGroupAlias(getMandatoryString(obj, "alias"));
        grp.setIsRegEx(obj.getBoolean("regex", false));
        if ( obj.containsKey("domains") ) {
            List<String> domains =
                Optional.ofNullable(obj.getJsonArray("domains"))
                    .orElse(Json.createArrayBuilder().build())
                    .getValuesAs(JsonString.class)
                    .stream()
                    .map(JsonString::getString)
                    // only validate if this group hasn't regex support enabled
                    .filter(d -> (grp.isRegEx() || DomainValidator.getInstance().isValid(d)))
                    .collect(Collectors.toList());
            if (domains.isEmpty())
                throw new JsonParseException("Field domains may not be an empty array or contain invalid domains. Enabled regex support?");
            grp.setEmailDomains(domains);
        } else {
            throw new JsonParseException("Field domains is mandatory.");
        }

        return grp;
    }

    public static <E extends Enum<E>> List<E> parseEnumsFromArray(JsonArray enumsArray, Class<E> enumClass) throws JsonParseException {
        final List<E> enums = new LinkedList<>();

        for (String name : enumsArray.getValuesAs(JsonString::getString)) {
            enums.add(Enum.valueOf(enumClass, name));
        }
        return enums;
    }

    public DatasetVersion parseDatasetVersion(JsonObject obj) throws JsonParseException {
        return parseDatasetVersion(obj, new DatasetVersion());
    }

    public Dataset parseDataset(JsonObject obj) throws JsonParseException {
        Dataset dataset = new Dataset();

        dataset.setAuthority(obj.getString("authority", null));
        dataset.setProtocol(obj.getString("protocol", null));
        dataset.setIdentifier(obj.getString("identifier",null));
        String mdl = obj.getString("metadataLanguage",null);
        if(mdl==null || settingsService.getBaseMetadataLanguageMap(new HashMap<String,String>(), true).containsKey(mdl)) {
          dataset.setMetadataLanguage(mdl);
        }else {
            throw new JsonParseException("Specified metadatalanguage not allowed.");
        }
        String datasetTypeIn = obj.getString("datasetType", DatasetType.DEFAULT_DATASET_TYPE);
        logger.fine("datasetTypeIn: " + datasetTypeIn);
        DatasetType datasetType = datasetTypeService.getByName(datasetTypeIn);
        if (datasetType != null) {
            dataset.setDatasetType(datasetType);
        } else {
            throw new JsonParseException("Invalid dataset type: " + datasetTypeIn);
        }

        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(dataset);
        dsv = parseDatasetVersion(obj.getJsonObject("datasetVersion"), dsv);
        List<DatasetVersion> versions = new ArrayList<>(1);
        versions.add(dsv);

        dataset.setVersions(versions);
        return dataset;
    }

    public DatasetVersion parseDatasetVersion(JsonObject obj, DatasetVersion dsv) throws JsonParseException {
        try {

            dsv.setDeaccessionLink(obj.getString("deaccessionLink", null));
            String deaccessionNote = obj.getString("deaccessionNote", null);
            // ToDo - the treatment of null inputs is inconsistent across different fields (either the original value is kept or set to null).
            // This is moot for most uses of this method, which start from an empty datasetversion, but use through https://github.com/IQSS/dataverse/blob/3e5a516670c42e019338063516a9d93a61833027/src/main/java/edu/harvard/iq/dataverse/api/datadeposit/ContainerManagerImpl.java#L112
            // starts from an existing version where this inconsistency could be/is a problem.
            if (deaccessionNote != null) {
                dsv.setDeaccessionNote(deaccessionNote);
            }
            dsv.setVersionNote(obj.getString("versionNote", null));
            int versionNumberInt = obj.getInt("versionNumber", -1);
            Long versionNumber = null;
            if (versionNumberInt !=-1) {
                versionNumber = new Long(versionNumberInt);
            }
            dsv.setVersionNumber(versionNumber);
            dsv.setMinorVersionNumber(parseLong(obj.getString("minorVersionNumber", null)));
            // if the existing datasetversion doesn not have an id
            // use the id from the json object.
            if (dsv.getId()==null) {
                 dsv.setId(parseLong(obj.getString("id", null)));
            }

            String versionStateStr = obj.getString("versionState", null);
            if (versionStateStr != null) {
                dsv.setVersionState(DatasetVersion.VersionState.valueOf(versionStateStr));
            }
            dsv.setReleaseTime(parseDate(obj.getString("releaseDate", null)));
            dsv.setLastUpdateTime(parseTime(obj.getString("lastUpdateTime", null)));
            dsv.setCreateTime(parseTime(obj.getString("createTime", null)));
            dsv.setArchiveTime(parseTime(obj.getString("archiveTime", null)));
            dsv.setUNF(obj.getString("UNF", null));
            // Terms of Use related fields
            TermsOfUseAndAccess terms = new TermsOfUseAndAccess();

            License license = null;

            try {
                // This method will attempt to parse the license in the format 
                // in which it appears in our json exports, as a compound
                // field, for ex.:
                // "license": {
                //    "name": "CC0 1.0",
                //    "uri": "http://creativecommons.org/publicdomain/zero/1.0"
                // }
                license = parseLicense(obj.getJsonObject("license"));
            } catch (ClassCastException cce) {
                logger.fine("class cast exception parsing the license section (will try parsing as a string)");
                // attempt to parse as string: 
                // i.e. this is for backward compatibility, after the bug in #9155
                // was fixed, with the old style of encoding the license info 
                // in input json, for ex.: 
                // "license" : "CC0 1.0"
                license = parseLicense(obj.getString("license", null));
            }

            if (license == null) {
                terms.setLicense(license);
                terms.setTermsOfUse(obj.getString("termsOfUse", null));
                terms.setConfidentialityDeclaration(obj.getString("confidentialityDeclaration", null));
                terms.setSpecialPermissions(obj.getString("specialPermissions", null));
                terms.setRestrictions(obj.getString("restrictions", null));
                terms.setCitationRequirements(obj.getString("citationRequirements", null));
                terms.setDepositorRequirements(obj.getString("depositorRequirements", null));
                terms.setConditions(obj.getString("conditions", null));
                terms.setDisclaimer(obj.getString("disclaimer", null));
            } else {
                terms.setLicense(license);
            }
            terms.setTermsOfAccess(obj.getString("termsOfAccess", null));
            terms.setDataAccessPlace(obj.getString("dataAccessPlace", null));
            terms.setOriginalArchive(obj.getString("originalArchive", null));
            terms.setAvailabilityStatus(obj.getString("availabilityStatus", null));
            terms.setContactForAccess(obj.getString("contactForAccess", null));
            terms.setSizeOfCollection(obj.getString("sizeOfCollection", null));
            terms.setStudyCompletion(obj.getString("studyCompletion", null));
            terms.setFileAccessRequest(obj.getBoolean("fileAccessRequest", false));
            dsv.setTermsOfUseAndAccess(terms);
            terms.setDatasetVersion(dsv);
            JsonObject metadataBlocks = obj.getJsonObject("metadataBlocks");
            if (metadataBlocks == null){
                throw new JsonParseException(BundleUtil.getStringFromBundle("jsonparser.error.metadatablocks.not.found"));
            }
            dsv.setDatasetFields(parseMetadataBlocks(metadataBlocks));

            JsonArray filesJson = obj.getJsonArray("files");
            if (filesJson == null) {
                filesJson = obj.getJsonArray("fileMetadatas");
            }
            if (filesJson != null) {
                dsv.setFileMetadatas(parseFiles(filesJson, dsv));
            }
            return dsv;
        } catch (ParseException ex) {
            throw new JsonParseException(BundleUtil.getStringFromBundle("jsonparser.error.parsing.date", Arrays.asList(ex.getMessage())) , ex);
        } catch (NumberFormatException ex) {
            throw new JsonParseException(BundleUtil.getStringFromBundle("jsonparser.error.parsing.number", Arrays.asList(ex.getMessage())), ex);
        }
    }

    private edu.harvard.iq.dataverse.license.License parseLicense(String licenseNameOrUri) throws JsonParseException {
        if (licenseNameOrUri == null){
            boolean safeDefaultIfKeyNotFound = true;
            if (settingsService.isTrueForKey(SettingsServiceBean.Key.AllowCustomTermsOfUse, safeDefaultIfKeyNotFound)){
                return null;
            } else {
                return licenseService.getDefault();
            }
        }
        License license = licenseService.getByNameOrUri(licenseNameOrUri);
        if (license == null) throw new JsonParseException("Invalid license: " + licenseNameOrUri);
        return license;
    }

    private edu.harvard.iq.dataverse.license.License parseLicense(JsonObject licenseObj) throws JsonParseException {
        if (licenseObj == null){
            boolean safeDefaultIfKeyNotFound = true;
            if (settingsService.isTrueForKey(SettingsServiceBean.Key.AllowCustomTermsOfUse, safeDefaultIfKeyNotFound)){
                return null;
            } else {
                return licenseService.getDefault();
            }
        }

        String licenseName = licenseObj.getString("name", null);
        String licenseUri = licenseObj.getString("uri", null);

        License license = null;

        // If uri is provided, we'll try that first. This is an easier lookup
        // method; the uri is always the same. The name may have been customized
        // (translated) on this instance, so we may be dealing with such translated
        // name, if this is exported json that we are processing. Meaning, unlike 
        // the uri, we cannot simply check it against the name in the License
        // database table. 
        if (licenseUri != null) {
            license = licenseService.getByNameOrUri(licenseUri);
        }

        if (license != null) {
            return license;
        }

        if (licenseName == null) {
            String exMsg = "Invalid or unsupported license section submitted"
                    + (licenseUri != null ? ": " + licenseUri : ".");
            throw new JsonParseException("Invalid or unsupported license section submitted.");
        }

        license = licenseService.getByPotentiallyLocalizedName(licenseName);
        if (license == null) {
            throw new JsonParseException("Invalid or unsupported license: " + licenseName);
        }
        return license;
    }

    public List<DatasetField> parseMetadataBlocks(JsonObject json) throws JsonParseException {
        Set<String> keys = json.keySet();
        List<DatasetField> fields = new LinkedList<>();

        for (String blockName : keys) {
            JsonObject blockJson = json.getJsonObject(blockName);
            JsonArray fieldsJson = blockJson.getJsonArray("fields");
            fields.addAll(parseFieldsFromArray(fieldsJson, true));
        }
        return fields;
    }

    public List<DatasetField> parseMultipleFields(JsonObject json) throws JsonParseException {
        return parseMultipleFields(json, false);
    }

    public List<DatasetField> parseMultipleFields(JsonObject json, boolean replaceData) throws JsonParseException {
        JsonArray fieldsJson = json.getJsonArray("fields");
        List<DatasetField> fields = parseFieldsFromArray(fieldsJson, false, replaceData);
        return fields;
    }

    public List<DatasetField> parseMultipleFieldsForDelete(JsonObject json) throws JsonParseException {
        List<DatasetField> fields = new LinkedList<>();
        for (JsonObject fieldJson : json.getJsonArray("fields").getValuesAs(JsonObject.class)) {
            fields.add(parseFieldForDelete(fieldJson));
        }
        return fields;
    }

    private List<DatasetField> parseFieldsFromArray(JsonArray fieldsArray, Boolean testType) throws JsonParseException {
        return parseFieldsFromArray(fieldsArray, testType, false);
    }

    private List<DatasetField> parseFieldsFromArray(JsonArray fieldsArray, Boolean testType, boolean replaceData) throws JsonParseException {
            List<DatasetField> fields = new LinkedList<>();
            for (JsonObject fieldJson : fieldsArray.getValuesAs(JsonObject.class)) {
                try {
                    DatasetField field = parseField(fieldJson, testType, replaceData);
                    if (field != null) {
                        fields.add(field);
                    }
                } catch (CompoundVocabularyException ex) {
                    DatasetFieldType fieldType = datasetFieldSvc.findByNameOpt(fieldJson.getString("typeName", ""));
                    if (lenient && (DatasetFieldConstant.geographicCoverage).equals(fieldType.getName())) {
                        fields.add(remapGeographicCoverage( ex));
                    } else {
                        // if not lenient mode, re-throw exception
                        throw ex;
                    }
                }

            }
        return fields;

    }

    public List<FileMetadata> parseFiles(JsonArray metadatasJson, DatasetVersion dsv) throws JsonParseException {
        List<FileMetadata> fileMetadatas = new LinkedList<>();
        if (metadatasJson != null) {
            for (JsonObject filemetadataJson : metadatasJson.getValuesAs(JsonObject.class)) {
                String label = filemetadataJson.getString("label");
                String directoryLabel = filemetadataJson.getString("directoryLabel", null);
                String description = filemetadataJson.getString("description", null);

                FileMetadata fileMetadata = new FileMetadata();
                fileMetadata.setLabel(label);
                fileMetadata.setDirectoryLabel(directoryLabel);
                fileMetadata.setDescription(description);
                fileMetadata.setDatasetVersion(dsv);

                if ( filemetadataJson.containsKey("dataFile") ) {
                    DataFile dataFile = parseDataFile(filemetadataJson.getJsonObject("dataFile"));
                    dataFile.getFileMetadatas().add(fileMetadata);
                    dataFile.setOwner(dsv.getDataset());
                    fileMetadata.setDataFile(dataFile);
                    if (dsv.getDataset() != null) {
                        if (dsv.getDataset().getFiles() == null) {
                            dsv.getDataset().setFiles(new ArrayList<>());
                        }
                        dsv.getDataset().getFiles().add(dataFile);
                    }
                }

                fileMetadatas.add(fileMetadata);
                fileMetadata.setCategories(getCategories(filemetadataJson, dsv.getDataset()));
            }
        }

        return fileMetadatas;
    }

    public DataFile parseDataFile(JsonObject datafileJson) {
        DataFile dataFile = new DataFile();

        Timestamp timestamp = new Timestamp(new Date().getTime());
        dataFile.setCreateDate(timestamp);
        dataFile.setModificationTime(timestamp);
        dataFile.setPermissionModificationTime(timestamp);

        if ( datafileJson.containsKey("filesize") ) {
            dataFile.setFilesize(datafileJson.getJsonNumber("filesize").longValueExact());
        }

        String contentType = datafileJson.getString("contentType", null);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String storageIdentifier = null;
        /**
         * When harvesting from other Dataverses using this json format, we 
         * don't want to import their storageidentifiers verbatim. Instead, we 
         * will modify them to point to the access API location on the remote
         * archive side.
         */
        if (harvestingClient != null && datafileJson.containsKey("id")) {
            String remoteId = datafileJson.getJsonNumber("id").toString();
            storageIdentifier = harvestingClient.getArchiveUrl()
                    + "/api/access/datafile/"
                    + remoteId;
            /**
             * Note that we don't have any practical use for these urls as 
             * of now. We used to, in the past, perform some tasks on harvested
             * content that involved trying to access the files. In any event, it
             * makes more sense to collect these urls, than the storage 
             * identifiers imported as is, which become completely meaningless 
             * on the local system.
             */
        } else {
            storageIdentifier = datafileJson.getString("storageIdentifier", null);
        }
        JsonObject checksum = datafileJson.getJsonObject("checksum");
        if (checksum != null) {
            // newer style that allows for SHA-1 rather than MD5
            /**
             * @todo Add more error checking. Do we really expect people to set
             * file metadata without uploading files? Some day we'd like to work
             * on a "native" API that allows for multipart upload of the JSON
             * describing the files (this "parseDataFile" method) and the bits
             * of the files themselves. See
             * https://github.com/IQSS/dataverse/issues/1612
             */
            String type = checksum.getString("type");
            if (type != null) {
                String value = checksum.getString("value");
                if (value != null) {
                    try {
                        dataFile.setChecksumType(DataFile.ChecksumType.fromString(type));
                        dataFile.setChecksumValue(value);
                    } catch (IllegalArgumentException ex) {
                        logger.info("Invalid");
                    }
                }
            }
        } else {
            // older, MD5 logic, still her for backward compatibility
            String md5 = datafileJson.getString("md5", null);
            if (md5 == null) {
                md5 = "unknown";
            }
            dataFile.setChecksumType(DataFile.ChecksumType.MD5);
            dataFile.setChecksumValue(md5);
        }

        // TODO: 
        // unf (if available)... etc.?

        dataFile.setContentType(contentType);
        dataFile.setStorageIdentifier(storageIdentifier);

        return dataFile;
    }
    /**
     * Special processing for GeographicCoverage compound field:
     * Handle parsing exceptions caused by invalid controlled vocabulary in the "country" field by
     * putting the invalid data in "otherGeographicCoverage" in a new compound value.
     *
     * @param ex - contains the invalid values to be processed
     * @return a compound DatasetField that contains the newly created values, in addition to
     * the original valid values.
     * @throws JsonParseException
     */
    private DatasetField remapGeographicCoverage(CompoundVocabularyException ex) throws JsonParseException{
        List<HashSet<FieldDTO>> geoCoverageList = new ArrayList<>();
        // For each exception, create HashSet of otherGeographic Coverage and add to list
        for (ControlledVocabularyException vocabEx : ex.getExList()) {
            HashSet<FieldDTO> set = new HashSet<>();
            set.add(FieldDTO.createPrimitiveFieldDTO(DatasetFieldConstant.otherGeographicCoverage, vocabEx.getStrValue()));
            geoCoverageList.add(set);
        }
        FieldDTO geoCoverageDTO = FieldDTO.createMultipleCompoundFieldDTO(DatasetFieldConstant.geographicCoverage, geoCoverageList);

        // convert DTO to datasetField so we can back valid values.
        Gson gson = new Gson();
        String jsonString = gson.toJson(geoCoverageDTO);
        JsonObject obj = JsonUtil.getJsonObject(jsonString);
        DatasetField geoCoverageField = parseField(obj);

        // add back valid values
        for (DatasetFieldCompoundValue dsfcv : ex.getValidValues()) {
            if (!dsfcv.getChildDatasetFields().isEmpty()) {
                dsfcv.setParentDatasetField(geoCoverageField);
                geoCoverageField.getDatasetFieldCompoundValues().add(dsfcv);
            }
        }
        return geoCoverageField;
    }


    public DatasetField parseFieldForDelete(JsonObject json) throws JsonParseException{
        DatasetField ret = new DatasetField();
        DatasetFieldType type = datasetFieldSvc.findByNameOpt(json.getString("typeName", ""));
        if (type == null) {
            throw new JsonParseException("Can't find type '" + json.getString("typeName", "") + "'");
        }
        return ret;
    }


    public DatasetField parseField(JsonObject json) throws JsonParseException {
        return parseField(json, true, false);
    }

    public DatasetField parseField(JsonObject json, Boolean testType) throws JsonParseException {
        return parseField(json, testType, false);
    }

    public DatasetField parseField(JsonObject json, Boolean testType, boolean replaceData) throws JsonParseException {
        if (json == null) {
            return null;
        }

        DatasetField ret = new DatasetField();
        DatasetFieldType type = datasetFieldSvc.findByNameOpt(json.getString("typeName", ""));


        if (type == null) {
            logger.fine("Can't find type '" + json.getString("typeName", "") + "'");
            return null;
        }
        if (testType && type.isAllowMultiples() != json.getBoolean("multiple")) {
            throw new JsonParseException("incorrect multiple   for field " + json.getString("typeName", ""));
        }
        if (testType && type.isCompound() && !json.getString("typeClass").equals("compound")) {
            throw new JsonParseException("incorrect  typeClass for field " + json.getString("typeName", "") + ", should be compound.");
        }
        if (testType && !type.isControlledVocabulary() && type.isPrimitive() && !json.getString("typeClass").equals("primitive")) {
            throw new JsonParseException("incorrect  typeClass for field: " + json.getString("typeName", "") + ", should be primitive");
        }
        if (testType && type.isControlledVocabulary() && !json.getString("typeClass").equals("controlledVocabulary")) {
            throw new JsonParseException("incorrect  typeClass for field " + json.getString("typeName", "") + ", should be controlledVocabulary");
        }


        ret.setDatasetFieldType(type);

        if (type.isCompound()) {
            parseCompoundValue(ret, type, json, testType, replaceData);
        } else if (type.isControlledVocabulary()) {
            parseControlledVocabularyValue(ret, type, json, replaceData);
        } else {
            parsePrimitiveValue(ret, type, json);
        }

        return ret;
    }

    public void parseCompoundValue(DatasetField dsf, DatasetFieldType compoundType, JsonObject json, Boolean testType, boolean replaceData) throws JsonParseException {
        List<ControlledVocabularyException> vocabExceptions = new ArrayList<>();
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        if (compoundType.isAllowMultiples()) {
            int order = 0;
            try {
                json.getJsonArray("value").getValuesAs(JsonObject.class);
            } catch (ClassCastException cce) {
                throw new JsonParseException("Invalid values submitted for " + compoundType.getName() + ". It should be an array of values.");
            }
            for (JsonObject obj : json.getJsonArray("value").getValuesAs(JsonObject.class)) {
                DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
                List<DatasetField> fields = new LinkedList<>();
                for (String fieldName : obj.keySet()) {
                    JsonObject childFieldJson = obj.getJsonObject(fieldName);
                    DatasetField f=null;
                    try {
                        f = parseField(childFieldJson, testType, replaceData);
                    } catch(ControlledVocabularyException ex) {
                        vocabExceptions.add(ex);
                    }

                    if (f!=null) {
                        if (!compoundType.getChildDatasetFieldTypes().contains(f.getDatasetFieldType())) {
                            throw new JsonParseException("field " + f.getDatasetFieldType().getName() + " is not a child of " + compoundType.getName());
                        }
                        f.setParentDatasetFieldCompoundValue(cv);
                            fields.add(f);
                    }
                }
                if (!fields.isEmpty()) {
                    cv.setChildDatasetFields(fields);
                    cv.setDisplayOrder(order);
                    vals.add(cv);
                }
                order++;
            }



        } else {

            DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
            List<DatasetField> fields = new LinkedList<>();
            JsonObject value = json.getJsonObject("value");
            for (String key : value.keySet()) {
                JsonObject childFieldJson = value.getJsonObject(key);
                DatasetField f = null;
                try {
                    f=parseField(childFieldJson, testType, replaceData);
                } catch(ControlledVocabularyException ex ) {
                    vocabExceptions.add(ex);
                }
                if (f!=null) {
                    f.setParentDatasetFieldCompoundValue(cv);
                    fields.add(f);
                }
            }
            if (!fields.isEmpty()) {
                cv.setChildDatasetFields(fields);
                vals.add(cv);
            }

    }
        if (!vocabExceptions.isEmpty()) {
            throw new CompoundVocabularyException( "Invalid controlled vocabulary in compound field ", vocabExceptions, vals);
        }

        for (DatasetFieldCompoundValue dsfcv : vals) {
            dsfcv.setParentDatasetField(dsf);
        }
        dsf.setDatasetFieldCompoundValues(vals);
    }

    public void parsePrimitiveValue(DatasetField dsf, DatasetFieldType dft , JsonObject json) throws JsonParseException {

        Map<Long, JsonObject> cvocMap = datasetFieldSvc.getCVocConf(true);
        boolean extVocab = cvocMap.containsKey(dft.getId());
        List<DatasetFieldValue> vals = new LinkedList<>();
        if (dft.isAllowMultiples()) {
           try {
            json.getJsonArray("value").getValuesAs(JsonObject.class);
            } catch (ClassCastException cce) {
                throw new JsonParseException("Invalid values submitted for " + dft.getName() + ". It should be an array of values.");
            }
            for (JsonString val : json.getJsonArray("value").getValuesAs(JsonString.class)) {
                DatasetFieldValue datasetFieldValue = new DatasetFieldValue(dsf);
                datasetFieldValue.setDisplayOrder(vals.size() - 1);
                datasetFieldValue.setValue(val.getString().trim());
                if(extVocab) {
                    if(!datasetFieldSvc.isValidCVocValue(dft, datasetFieldValue.getValue())) {
                        throw new JsonParseException("Invalid values submitted for " + dft.getName() + " which is limited to specific vocabularies.");
                    }
                }
                vals.add(datasetFieldValue);
            }

        } else {
            try {json.getString("value");}
            catch (ClassCastException cce) {
                throw new JsonParseException("Invalid value submitted for " + dft.getName() + ". It should be a single value.");
            }
            DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
            datasetFieldValue.setValue(json.getString("value", "").trim());
            datasetFieldValue.setDatasetField(dsf);
            if(extVocab) {
                if(!datasetFieldSvc.isValidCVocValue(dft, datasetFieldValue.getValue())) {
                    throw new JsonParseException("Invalid values submitted for " + dft.getName() + " which is limited to specific vocabularies.");
                }
            }
            vals.add(datasetFieldValue);
        }

        dsf.setDatasetFieldValues(vals);
    }

    public Workflow parseWorkflow(JsonObject json) throws JsonParseException {
        Workflow retVal = new Workflow();
        validate("", json, "name", ValueType.STRING);
        validate("", json, "steps", ValueType.ARRAY);
        retVal.setName( json.getString("name") );
        JsonArray stepArray = json.getJsonArray("steps");
        List<WorkflowStepData> steps = new ArrayList<>(stepArray.size());
        for ( JsonValue jv : stepArray ) {
            steps.add(parseStepData((JsonObject) jv));
        }
        retVal.setSteps(steps);
        return retVal;
    }

    public WorkflowStepData parseStepData( JsonObject json ) throws JsonParseException {
        WorkflowStepData wsd = new WorkflowStepData();
        validate("step", json, "provider", ValueType.STRING);
        validate("step", json, "stepType", ValueType.STRING);

        wsd.setProviderId(json.getString("provider"));
        wsd.setStepType(json.getString("stepType"));
        if ( json.containsKey("parameters") ) {
            JsonObject params = json.getJsonObject("parameters");
            Map<String,String> paramMap = new HashMap<>();
            params.keySet().forEach(k -> paramMap.put(k,jsonValueToString(params.get(k))));
            wsd.setStepParameters(paramMap);
        }
        if ( json.containsKey("requiredSettings") ) {
            JsonObject settings = json.getJsonObject("requiredSettings");
            Map<String,String> settingsMap = new HashMap<>();
            settings.keySet().forEach(k -> settingsMap.put(k,jsonValueToString(settings.get(k))));
            wsd.setStepSettings(settingsMap);
        }
        return wsd;
    }

    private String jsonValueToString(JsonValue jv) {
        switch ( jv.getValueType() ) {
            case STRING: return ((JsonString)jv).getString();
            default: return jv.toString();
        }
    }

    public void parseControlledVocabularyValue(DatasetField dsf, DatasetFieldType cvvType, JsonObject json, boolean replaceData) throws JsonParseException {
        List<ControlledVocabularyValue> vals = new LinkedList<>();
        try {
            if (cvvType.isAllowMultiples()) {
                try {
                    json.getJsonArray("value").getValuesAs(JsonObject.class);
                } catch (ClassCastException cce) {
                    throw new JsonParseException("Invalid values submitted for " + cvvType.getName() + ". It should be an array of values.");
                }
                for (JsonString strVal : json.getJsonArray("value").getValuesAs(JsonString.class)) {
                    String strValue = strVal.getString();
                    ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(cvvType, strValue, lenient);
                    if (cvv == null) {
                        if (allowHarvestingMissingCVV) {
                            // we need to process these as primitive values
                            logger.warning("Value '" + strValue + "' does not exist in type '" + cvvType.getName() + "'. Processing as primitive per setting override.");
                            parsePrimitiveValue(dsf, cvvType, json);
                            return;
                        } else {
                            throw new ControlledVocabularyException("Value '" + strValue + "' does not exist in type '" + cvvType.getName() + "'", cvvType, strValue);
                        }
                    }
                    cvv.setDatasetFieldType(cvvType);
                    // Only add value to the list if it is not a duplicate
                    if (!vals.contains(cvv)) {
                        vals.add(cvv);
                    }
                }

            } else {
                try {
                    json.getString("value");
                } catch (ClassCastException cce) {
                    throw new JsonParseException("Invalid value submitted for " + cvvType.getName() + ". It should be a single value.");
                }
                String strValue = json.getString("value", "");

                if (strValue.isEmpty() && replaceData) {
                    parsePrimitiveValue(dsf, cvvType, json);
                    return;
                }

                ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(cvvType, strValue, lenient);
                if (cvv == null) {
                    if (allowHarvestingMissingCVV) {
                        // we need to process this as a primitive value
                        parsePrimitiveValue(dsf, cvvType , json);
                        return;
                    } else {
                        throw new ControlledVocabularyException("Value '" + strValue + "' does not exist in type '" + cvvType.getName() + "'", cvvType, strValue);
                    }
                }
                cvv.setDatasetFieldType(cvvType);
                vals.add(cvv);
            }
        } catch (ClassCastException cce) {
            throw new JsonParseException("Invalid values submitted for " + cvvType.getName());
        }

        dsf.setControlledVocabularyValues(vals);
    }

    Date parseDate(String str) throws ParseException {
        return str == null ? null : Util.getDateFormat().parse(str);
    }

    Date parseTime(String str) throws ParseException {
        return str == null ? null : Util.getDateTimeFormat().parse(str);
    }

    Long parseLong(String str) throws NumberFormatException {
        return (str == null) ? null : Long.valueOf(str);
    }

    int parsePrimitiveInt(String str, int defaultValue) {
        return str == null ? defaultValue : Integer.parseInt(str);
    }

    public String parseHarvestingClient(JsonObject obj, HarvestingClient harvestingClient) throws JsonParseException {

        String dataverseAlias = obj.getString("dataverseAlias",null);

        harvestingClient.setName(obj.getString("nickName",null));
        harvestingClient.setSourceName(obj.getString("sourceName",null));
        harvestingClient.setHarvestStyle(obj.getString("style", "default"));
        harvestingClient.setHarvestingUrl(obj.getString("harvestUrl",null));
        harvestingClient.setArchiveUrl(obj.getString("archiveUrl",null));
        harvestingClient.setArchiveDescription(obj.getString("archiveDescription", null));
        harvestingClient.setMetadataPrefix(obj.getString("metadataFormat",null));
        harvestingClient.setHarvestingSet(obj.getString("set",null));
        harvestingClient.setCustomHttpHeaders(obj.getString("customHeaders", null));
        harvestingClient.setAllowHarvestingMissingCVV(obj.getBoolean("allowHarvestingMissingCVV", false));
        harvestingClient.setUseListrecords(obj.getBoolean("useListRecords", false));
        harvestingClient.setUseOaiIdentifiersAsPids(obj.getBoolean("useOaiIdentifiersAsPids", false));
        
        harvestingClient.readScheduleDescription(obj.getString("schedule", null));

        return dataverseAlias;
    }

    private List<DataFileCategory> getCategories(JsonObject filemetadataJson, Dataset dataset) {
        JsonArray categories = filemetadataJson.getJsonArray(OptionalFileParams.CATEGORIES_ATTR_NAME);
        if (categories == null || categories.isEmpty() || dataset == null) {
            return null;
        }
        List<DataFileCategory> dataFileCategories = new ArrayList<>();
        for (Object category : categories.getValuesAs(JsonString.class)) {
            JsonString categoryAsJsonString;
            try {
                categoryAsJsonString = (JsonString) category;
            } catch (ClassCastException ex) {
                logger.info("ClassCastException caught in getCategories: " + ex);
                return null;
            }
            DataFileCategory dfc = new DataFileCategory();
            dfc.setDataset(dataset);
            dfc.setName(categoryAsJsonString.getString());
            dataFileCategories.add(dfc);
        }
        return dataFileCategories;
    }

    /**
     * Validate than a JSON object has a field of an expected type, or throw an
     * inforamtive exception.
     * @param objectName
     * @param jobject
     * @param fieldName
     * @param expectedValueType
     * @throws JsonParseException
     */
    private void validate(String objectName, JsonObject jobject, String fieldName, ValueType expectedValueType) throws JsonParseException {
        if ( (!jobject.containsKey(fieldName))
              || (jobject.get(fieldName).getValueType()!=expectedValueType) ) {
            throw new JsonParseException( objectName + " missing a field named '"+fieldName+"' of type " + expectedValueType );
        }
    }

    public UserDTO parseUserDTO(JsonObject jobj) throws JsonParseException {
        UserDTO userDTO = new UserDTO();

        userDTO.setUsername(jobj.getString("username", null));
        userDTO.setEmailAddress(jobj.getString("emailAddress", null));
        userDTO.setFirstName(jobj.getString("firstName", null));
        userDTO.setLastName(jobj.getString("lastName", null));
        userDTO.setAffiliation(jobj.getString("affiliation", null));
        userDTO.setPosition(jobj.getString("position", null));

        if (!FeatureFlags.API_BEARER_AUTH_HANDLE_TOS_ACCEPTANCE_IN_IDP.enabled()) {
            userDTO.setTermsAccepted(getMandatoryBoolean(jobj, "termsAccepted"));
        }

        return userDTO;
    }
}
