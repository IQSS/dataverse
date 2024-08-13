package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.export.OAI_OREExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This class is used to generate a JSON-LD representation of a Dataverse object leveraging the OAI_ORE and other community vocabularies. As of v1.0.0,
 * the format is being versioned and ANY CHANGES TO THE OUTPUT of this class must be reflected in a version increment (see DATAVERSE_ORE_FORMAT_VERSION).
 * 
 * The OREMap class is intended to record ALL the information needed to recreate an existing Dataverse dataset. As of v1.0.0, this is true with the 
 * exception that auxiliary files are not referenced in the OREMap. While many types of auxiliary files will be regenerated automatically based on datafile
 *  contents, Dataverse now allows manually uploaded auxiliary files and these cannot be reproduced solely from the dataset/datafile contents. 
 */
public class OREMap {

    //Required Services
    static SettingsServiceBean settingsService;
    static DatasetFieldServiceBean datasetFieldService;
    static SystemConfig systemConfig;
    
    private static final Logger logger = Logger.getLogger(OREMap.class.getCanonicalName());
    
    public static final String NAME = "OREMap";
    
    //NOTE: Update this value whenever the output of this class is changed
    private static final String DATAVERSE_ORE_FORMAT_VERSION = "Dataverse OREMap Format v1.0.0";
    private static final String DATAVERSE_SOFTWARE_NAME = "Dataverse";
    private static final String DATAVERSE_SOFTWARE_URL = "https://github.com/iqss/dataverse";
    
    
    private Map<String, String> localContext = new TreeMap<String, String>();
    private DatasetVersion version;
    private Boolean excludeEmail = null;

    public OREMap(DatasetVersion version) {
        this.version = version;
    }

    //Used when the ExcludeEmailFromExport needs to be overriden, i.e. for archiving
    public OREMap(DatasetVersion dv, boolean exclude) {
        this.version = dv;
        this.excludeEmail = exclude;
    }

    public void writeOREMap(OutputStream outputStream) throws Exception {
        outputStream.write(getOREMap().toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public JsonObject getOREMap() {
        return getOREMap(false);
    }
    
    public JsonObject getOREMap(boolean aggregationOnly) {
        return getOREMapBuilder(aggregationOnly).build();
    }
    
    public JsonObjectBuilder getOREMapBuilder(boolean aggregationOnly) {

        //Set this flag if it wasn't provided
        if(excludeEmail==null) {
            excludeEmail = settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false);
        }
        
        // Add namespaces we'll definitely use to Context
        // Additional namespaces are added as needed below
        localContext.putIfAbsent(JsonLDNamespace.ore.getPrefix(), JsonLDNamespace.ore.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.dcterms.getPrefix(), JsonLDNamespace.dcterms.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.dvcore.getPrefix(), JsonLDNamespace.dvcore.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.schema.getPrefix(), JsonLDNamespace.schema.getUrl());

        Dataset dataset = version.getDataset();
        String id = dataset.getGlobalId().asURL();
        JsonArrayBuilder fileArray = Json.createArrayBuilder();
        // The map describes an aggregation
        JsonObjectBuilder aggBuilder = Json.createObjectBuilder();
        List<DatasetField> fields = version.getDatasetFields();
        // That has it's own metadata
        Map<Long, JsonObject> cvocMap = datasetFieldService.getCVocConf(true);
        for (DatasetField field : fields) {
            if (!field.isEmpty()) {
                DatasetFieldType dfType = field.getDatasetFieldType();
                JsonLDTerm fieldName = dfType.getJsonLDTerm();
                JsonValue jv = getJsonLDForField(field, excludeEmail, cvocMap, localContext);
                if(jv!=null) {
                    aggBuilder.add(fieldName.getLabel(), jv);
                }
            }
        }
        // Add metadata related to the Dataset/DatasetVersion
        aggBuilder.add("@id", id)
                .add("@type",
                        Json.createArrayBuilder().add(JsonLDTerm.ore("Aggregation").getLabel())
                                .add(JsonLDTerm.schemaOrg("Dataset").getLabel()))
                .add(JsonLDTerm.schemaOrg("version").getLabel(), version.getFriendlyVersionNumber())
                .add(JsonLDTerm.schemaOrg("name").getLabel(), version.getTitle())
                .add(JsonLDTerm.schemaOrg("dateModified").getLabel(), version.getLastUpdateTime().toString());
        addIfNotNull(aggBuilder, JsonLDTerm.schemaOrg("datePublished"), dataset.getPublicationDateFormattedYYYYMMDD());
        //Add version state info - DRAFT, RELEASED, DEACCESSIONED, ARCHIVED with extra info for DEACCESIONED
        VersionState vs = version.getVersionState();
        if(vs.equals(VersionState.DEACCESSIONED)) {
            JsonObjectBuilder deaccBuilder = Json.createObjectBuilder();
            deaccBuilder.add(JsonLDTerm.schemaOrg("name").getLabel(), vs.name());
            deaccBuilder.add(JsonLDTerm.DVCore("reason").getLabel(), version.getVersionNote());
            addIfNotNull(deaccBuilder, JsonLDTerm.DVCore("forwardUrl"), version.getArchiveNote());
            aggBuilder.add(JsonLDTerm.schemaOrg("creativeWorkStatus").getLabel(), deaccBuilder);
            
        } else {
            aggBuilder.add(JsonLDTerm.schemaOrg("creativeWorkStatus").getLabel(), vs.name());
        }

        TermsOfUseAndAccess terms = version.getTermsOfUseAndAccess();
        if (terms.getLicense() != null) {
            aggBuilder.add(JsonLDTerm.schemaOrg("license").getLabel(),
                    terms.getLicense().getUri().toString());
        } else {
            addIfNotNull(aggBuilder, JsonLDTerm.termsOfUse, terms.getTermsOfUse());
            addIfNotNull(aggBuilder, JsonLDTerm.confidentialityDeclaration, terms.getConfidentialityDeclaration());
            addIfNotNull(aggBuilder, JsonLDTerm.specialPermissions, terms.getSpecialPermissions());
            addIfNotNull(aggBuilder, JsonLDTerm.restrictions, terms.getRestrictions());
            addIfNotNull(aggBuilder, JsonLDTerm.citationRequirements, terms.getCitationRequirements());
            addIfNotNull(aggBuilder, JsonLDTerm.depositorRequirements, terms.getDepositorRequirements());
            addIfNotNull(aggBuilder, JsonLDTerm.conditions, terms.getConditions());
            addIfNotNull(aggBuilder, JsonLDTerm.disclaimer, terms.getDisclaimer());
        }
        // Add fileTermsofAccess as an object since it is compound
        JsonObjectBuilder fAccess = Json.createObjectBuilder();
        addIfNotNull(fAccess, JsonLDTerm.termsOfAccess, terms.getTermsOfAccess());
        addIfNotNull(fAccess, JsonLDTerm.fileRequestAccess, terms.isFileAccessRequest());
        addIfNotNull(fAccess, JsonLDTerm.dataAccessPlace, terms.getDataAccessPlace());
        addIfNotNull(fAccess, JsonLDTerm.originalArchive, terms.getOriginalArchive());
        addIfNotNull(fAccess, JsonLDTerm.availabilityStatus, terms.getAvailabilityStatus());
        addIfNotNull(fAccess, JsonLDTerm.contactForAccess, terms.getContactForAccess());
        addIfNotNull(fAccess, JsonLDTerm.sizeOfCollection, terms.getSizeOfCollection());
        addIfNotNull(fAccess, JsonLDTerm.studyCompletion, terms.getStudyCompletion());
        JsonObject fAccessObject = fAccess.build();
        if (!fAccessObject.isEmpty()) {
            aggBuilder.add(JsonLDTerm.fileTermsOfAccess.getLabel(), fAccessObject);
        }

        aggBuilder.add(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel(),
                BrandingUtil.getInstallationBrandName());

        aggBuilder.add(JsonLDTerm.schemaOrg("isPartOf").getLabel(), getDataverseDescription(dataset.getOwner()));
        String mdl = dataset.getMetadataLanguage();
        if (DvObjectContainer.isMetadataLanguageSet(mdl)) {
            aggBuilder.add(JsonLDTerm.schemaOrg("inLanguage").getLabel(), mdl);
        }
        
        // The aggregation aggregates aggregatedresources (Datafiles) which each have
        // their own entry and metadata
        JsonArrayBuilder aggResArrayBuilder = Json.createArrayBuilder();
        if (!aggregationOnly) {

            for (FileMetadata fmd : version.getFileMetadatas()) {
                DataFile df = fmd.getDataFile();
                JsonObjectBuilder aggRes = Json.createObjectBuilder();

                if (fmd.getDescription() != null) {
                    aggRes.add(JsonLDTerm.schemaOrg("description").getLabel(), fmd.getDescription());
                } else {
                    addIfNotNull(aggRes, JsonLDTerm.schemaOrg("description"), df.getDescription());
                }
                String fileName = fmd.getLabel();// "label" is the filename
                long fileSize = df.getFilesize();
                String mimeType = df.getContentType();
                String currentIngestedName = null;
                boolean ingested=df.getOriginalFileName()!= null || df.getOriginalFileSize()!=null || df.getOriginalFileFormat()!=null;
                if(ingested) {
                    if(df.getOriginalFileName()!=null) {
                        currentIngestedName= fileName;
                        fileName = df.getOriginalFileName();
                    } else {
                        logger.warning("Missing Original file name for id: " + df.getId());
                    }
                    if(df.getOriginalFileSize()!=null) {
                        fileSize = df.getOriginalFileSize();
                    } else {
                        logger.warning("Missing Original file size for id: " + df.getId());
                    }
                    if(df.getOriginalFileFormat()!=null) {
                        mimeType = df.getOriginalFileFormat();
                    } else {
                        logger.warning("Missing Original file format for id: " + df.getId());
                    }

                    
                }
                addIfNotNull(aggRes, JsonLDTerm.schemaOrg("name"), fileName); 
                addIfNotNull(aggRes, JsonLDTerm.restricted, fmd.isRestricted());
                Embargo embargo=df.getEmbargo(); 
                if(embargo!=null) {
                    String date = embargo.getFormattedDateAvailable();
                    String reason= embargo.getReason();
                    JsonObjectBuilder embargoObject = Json.createObjectBuilder();
                    embargoObject.add(JsonLDTerm.DVCore("dateAvailable").getLabel(), date);
                    if(reason!=null) {
                        embargoObject.add(JsonLDTerm.DVCore("reason").getLabel(), reason);
                    }
                    aggRes.add(JsonLDTerm.DVCore("embargoed").getLabel(), embargoObject);
                }
                Retention retention = df.getRetention();
                if(retention!=null) {
                    String date = retention.getFormattedDateUnavailable();
                    String reason= retention.getReason();
                    JsonObjectBuilder retentionObject = Json.createObjectBuilder();
                    retentionObject.add(JsonLDTerm.DVCore("dateUnavailable").getLabel(), date);
                    if(reason!=null) {
                        retentionObject.add(JsonLDTerm.DVCore("reason").getLabel(), reason);
                    }
                    aggRes.add(JsonLDTerm.DVCore("retained").getLabel(), retentionObject);
                }
                addIfNotNull(aggRes, JsonLDTerm.directoryLabel, fmd.getDirectoryLabel());
                addIfNotNull(aggRes, JsonLDTerm.schemaOrg("version"), fmd.getVersion());
                addIfNotNull(aggRes, JsonLDTerm.datasetVersionId, fmd.getDatasetVersion().getId());
                JsonArray catArray = null;
                if (fmd != null) {
                    List<String> categories = fmd.getCategoriesByName();
                    if (categories.size() > 0) {
                        JsonArrayBuilder jab = Json.createArrayBuilder();
                        for (String s : categories) {
                            jab.add(s);
                        }
                        catArray = jab.build();
                    }
                }
                addIfNotNull(aggRes, JsonLDTerm.categories, catArray);
                // File DOI if it exists
                String fileId = null;
                String fileSameAs = null;
                if (df.getGlobalId()!=null) {
                    fileId = df.getGlobalId().asString();
                    fileSameAs = SystemConfig.getDataverseSiteUrlStatic()
                            + "/api/access/datafile/:persistentId?persistentId=" + fileId + (ingested ? "&format=original":"");
                } else {
                    fileId = SystemConfig.getDataverseSiteUrlStatic() + "/file.xhtml?fileId=" + df.getId();
                    fileSameAs = SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + df.getId() + (ingested ? "?format=original":"");
                }
                aggRes.add("@id", fileId);
                aggRes.add(JsonLDTerm.schemaOrg("sameAs").getLabel(), fileSameAs);
                fileArray.add(fileId);

                aggRes.add("@type", JsonLDTerm.ore("AggregatedResource").getLabel());
                addIfNotNull(aggRes, JsonLDTerm.schemaOrg("fileFormat"), mimeType);
                addIfNotNull(aggRes, JsonLDTerm.filesize, fileSize);
                addIfNotNull(aggRes, JsonLDTerm.storageIdentifier, df.getStorageIdentifier());
                addIfNotNull(aggRes, JsonLDTerm.currentIngestedName, currentIngestedName);
                addIfNotNull(aggRes, JsonLDTerm.UNF, df.getUnf());
                addIfNotNull(aggRes, JsonLDTerm.rootDataFileId, df.getRootDataFileId());
                addIfNotNull(aggRes, JsonLDTerm.previousDataFileId, df.getPreviousDataFileId());
                JsonObject checksum = null;
                // Add checksum. RDA recommends SHA-512
                if (df.getChecksumType() != null && df.getChecksumValue() != null) {
                    checksum = Json.createObjectBuilder().add("@type", df.getChecksumType().toString())
                            .add("@value", df.getChecksumValue()).build();
                    aggRes.add(JsonLDTerm.checksum.getLabel(), checksum);
                }
                JsonArray tabTags = null;
                JsonArrayBuilder jab = JsonPrinter.getTabularFileTags(df);
                if (jab != null) {
                    tabTags = jab.build();
                }
                addIfNotNull(aggRes, JsonLDTerm.tabularTags, tabTags);
                // Add latest resource to the array
                aggResArrayBuilder.add(aggRes.build());
            }
        }
        // Build the '@context' object for json-ld based on the localContext entries
        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        for (Entry<String, String> e : localContext.entrySet()) {
            contextBuilder.add(e.getKey(), e.getValue());
        }
        if (aggregationOnly) {
            return aggBuilder.add("@context", contextBuilder.build());
        } else {
            // Now create the overall map object with it's metadata
            
            //Start with a reference to the Dataverse software
            JsonObjectBuilder dvSoftwareBuilder = Json.createObjectBuilder()
                    .add("@type", JsonLDTerm.schemaOrg("SoftwareApplication").getLabel())
                    .add(JsonLDTerm.schemaOrg("name").getLabel(), DATAVERSE_SOFTWARE_NAME)
                    .add(JsonLDTerm.schemaOrg("version").getLabel(), systemConfig.getVersion(true))
                    .add(JsonLDTerm.schemaOrg("url").getLabel(), DATAVERSE_SOFTWARE_URL);
            
            //Now the OREMAP object itself
            JsonObjectBuilder oremapBuilder = Json.createObjectBuilder()
                    .add(JsonLDTerm.dcTerms("modified").getLabel(), LocalDate.now().toString())
                    .add(JsonLDTerm.dcTerms("creator").getLabel(), BrandingUtil.getInstallationBrandName())
                    .add("@type", JsonLDTerm.ore("ResourceMap").getLabel())
                    //Add the version of our ORE format used
                    .add(JsonLDTerm.schemaOrg("additionalType").getLabel(), DATAVERSE_ORE_FORMAT_VERSION)
                    //Indicate which Dataverse version created it
                    .add(JsonLDTerm.DVCore("generatedBy").getLabel(), dvSoftwareBuilder)
                    // Define an id for the map itself (separate from the @id of the dataset being
                    // described
                    .add("@id",
                            SystemConfig.getDataverseSiteUrlStatic() + "/api/datasets/export?exporter="
                                    + OAI_OREExporter.NAME + "&persistentId=" + id)
                    // Add the aggregation (Dataset) itself to the map.
                    .add(JsonLDTerm.ore("describes").getLabel(),
                            aggBuilder.add(JsonLDTerm.ore("aggregates").getLabel(), aggResArrayBuilder.build())
                                    .add(JsonLDTerm.schemaOrg("hasPart").getLabel(), fileArray.build()).build())
                    // and finally add the context
                    .add("@context", contextBuilder.build());
            return oremapBuilder;
        }
    }

    private JsonObjectBuilder getDataverseDescription(Dataverse dv) {
        //Schema.org is already in local context, no updates needed as long as we only use chemaOrg and "@id" here
        JsonObjectBuilder dvjob = Json.createObjectBuilder().add(JsonLDTerm.schemaOrg("name").getLabel(), dv.getCurrentName()).add("@id", dv.getLocalURL());
        addIfNotNull(dvjob, JsonLDTerm.schemaOrg("description"), dv.getDescription());
        Dataverse owner = dv.getOwner();
        if(owner!=null) {
            dvjob.add(JsonLDTerm.schemaOrg("isPartOf").getLabel(), getDataverseDescription(owner));
        }
        return dvjob;
    }

    /*
     * Simple methods to only add an entry to JSON if the value of the term is
     * non-null. Methods created for string, JsonValue, boolean, and long
     */

    private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, String value) {
        if (value != null) {
            builder.add(key.getLabel(), value);
            addToContextMap(key);
        }
    }

    private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, JsonValue value) {
        if (value != null) {
            builder.add(key.getLabel(), value);
            addToContextMap(key);
        }
    }

    private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, Boolean value) {
        if (value != null) {
            builder.add(key.getLabel(), value);
            addToContextMap(key);
        }
    }

    private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, Long value) {
        if (value != null) {
            builder.add(key.getLabel(), value);
            addToContextMap(key);
        }
    }

    private void addToContextMap(JsonLDTerm key) {
        if (!key.inNamespace()) {
            localContext.putIfAbsent(key.getLabel(), key.getUrl());
        }
    }

    public JsonLDTerm getContactTerm() {
        return getTermFor(DatasetFieldConstant.datasetContact);
    }

    public JsonLDTerm getContactNameTerm() {
        return getTermFor(DatasetFieldConstant.datasetContactName);
    }

    public JsonLDTerm getContactEmailTerm() {
        return getTermFor(DatasetFieldConstant.datasetContactEmail);
    }

    public JsonLDTerm getDescriptionTerm() {
        return getTermFor(DatasetFieldConstant.description);
    }

    public JsonLDTerm getDescriptionTextTerm() {
        return getTermFor(DatasetFieldConstant.descriptionText);
    }

    private JsonLDTerm getTermFor(String fieldTypeName) {
        //Could call datasetFieldService.findByName(fieldTypeName) - is that faster/prefereable?
        for (DatasetField dsf : version.getFlatDatasetFields()) {
            DatasetFieldType dsft = dsf.getDatasetFieldType();
            if (dsft.getName().equals(fieldTypeName)) {
                return dsft.getJsonLDTerm();
            }
        }
        return null;
    }
    
    public static JsonValue getJsonLDForField(DatasetField field, Boolean excludeEmail, Map<Long, JsonObject> cvocMap,
            Map<String, String> localContext) {

        DatasetFieldType dfType = field.getDatasetFieldType();
        if (excludeEmail && DatasetFieldType.FieldType.EMAIL.equals(dfType.getFieldType())) {
            return null;
        }

        JsonLDTerm fieldName = dfType.getJsonLDTerm();
        if (fieldName.inNamespace()) {
            localContext.putIfAbsent(fieldName.getNamespace().getPrefix(), fieldName.getNamespace().getUrl());
        } else {
            localContext.putIfAbsent(fieldName.getLabel(), fieldName.getUrl());
        }
        JsonArrayBuilder vals = Json.createArrayBuilder();
        if (!dfType.isCompound()) {
            for (String val : field.getValues_nondisplay()) {
                if (cvocMap.containsKey(dfType.getId())) {
                    addCvocValue(val, vals, cvocMap.get(dfType.getId()), localContext);
                } else {
                    vals.add(val);
                }
            }
        } else {
            // ToDo: Needs to be recursive (as in JsonPrinter?)
            for (DatasetFieldCompoundValue dscv : field.getDatasetFieldCompoundValues()) {
                // compound values are of different types
                JsonObjectBuilder child = Json.createObjectBuilder();

                for (DatasetField dsf : dscv.getChildDatasetFields()) {
                    DatasetFieldType dsft = dsf.getDatasetFieldType();
                    if (excludeEmail && DatasetFieldType.FieldType.EMAIL.equals(dsft.getFieldType())) {
                        continue;
                    }
                    // which may have multiple values
                    if (!dsf.isEmpty()) {
                        // Add context entry
                        // ToDo - also needs to recurse here?
                        JsonLDTerm subFieldName = dsft.getJsonLDTerm();
                        if (subFieldName.inNamespace()) {
                            localContext.putIfAbsent(subFieldName.getNamespace().getPrefix(),
                                    subFieldName.getNamespace().getUrl());
                        } else {
                            localContext.putIfAbsent(subFieldName.getLabel(), subFieldName.getUrl());
                        }

                        List<String> values = dsf.getValues_nondisplay();

                        JsonArrayBuilder childVals = Json.createArrayBuilder();

                        for (String val : dsf.getValues_nondisplay()) {
                            logger.fine("Child name: " + dsft.getName());
                            if (cvocMap.containsKey(dsft.getId())) {
                                logger.fine("Calling addcvocval for: " + dsft.getName());
                                addCvocValue(val, childVals, cvocMap.get(dsft.getId()), localContext);
                            } else {
                                childVals.add(val);
                            }
                        }
                        if (values.size() > 1) {
                            child.add(subFieldName.getLabel(), childVals);
                        } else {
                            child.add(subFieldName.getLabel(), childVals.build().get(0));
                        }
                    }
                }
                vals.add(child);
            }
        }
        // Add metadata value to aggregation, suppress array when only one value
        JsonArray valArray = vals.build();
        return (valArray.size() != 1) ? valArray : valArray.get(0);
    }

    private static void addCvocValue(String val, JsonArrayBuilder vals, JsonObject cvocEntry,
            Map<String, String> localContext) {
        try {
            if (cvocEntry.containsKey("retrieval-filtering")) {
                JsonObject filtering = cvocEntry.getJsonObject("retrieval-filtering");
                JsonObject context = filtering.getJsonObject("@context");
                for (String prefix : context.keySet()) {
                    localContext.putIfAbsent(prefix, context.getString(prefix));
                }
                JsonObjectBuilder job = Json.createObjectBuilder(datasetFieldService.getExternalVocabularyValue(val));
                job.add("@id", val);
                JsonObject extVal = job.build();
                logger.fine("Adding: " + extVal);
                vals.add(extVal);
            } else {
                vals.add(val);
            }
        } catch (Exception e) {
            logger.warning("Couldn't interpret value for : " + val + " : " + e.getMessage());
            logger.log(Level.FINE, ExceptionUtils.getStackTrace(e));
            vals.add(val);
        }
    }

    //These are used to pick up various settings/constants from the application
    public static void injectServices(SettingsServiceBean settingsSvc, DatasetFieldServiceBean datasetFieldSvc, SystemConfig systemCfg) {
        settingsService = settingsSvc;
        datasetFieldService = datasetFieldSvc;
        systemConfig = systemCfg;
    }
}
