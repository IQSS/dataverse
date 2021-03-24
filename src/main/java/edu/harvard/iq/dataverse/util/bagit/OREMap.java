package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.export.OAI_OREExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class OREMap {

    static SettingsServiceBean settingsService;
    
    public static final String NAME = "OREMap";
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
        outputStream.write(getOREMap().toString().getBytes("UTF8"));
        outputStream.flush();
    }

    public JsonObject getOREMap() throws Exception {

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
        String id = dataset.getGlobalId().asString();
        JsonArrayBuilder fileArray = Json.createArrayBuilder();
        // The map describes an aggregation
        JsonObjectBuilder aggBuilder = Json.createObjectBuilder();
        List<DatasetField> fields = version.getDatasetFields();
        // That has it's own metadata
        for (DatasetField field : fields) {
            if (!field.isEmpty()) {
                DatasetFieldType dfType = field.getDatasetFieldType();
                if(excludeEmail && DatasetFieldType.FieldType.EMAIL.equals(dfType.getFieldType())) {
                    continue;
                }
                JsonLDTerm fieldName = getTermFor(dfType);
                if (fieldName.inNamespace()) {
                    localContext.putIfAbsent(fieldName.getNamespace().getPrefix(), fieldName.getNamespace().getUrl());
                } else {
                    localContext.putIfAbsent(fieldName.getLabel(), fieldName.getUrl());
                }
                JsonArrayBuilder vals = Json.createArrayBuilder();
                if (!dfType.isCompound()) {
                    for (String val : field.getValues_nondisplay()) {
                        vals.add(val);
                    }
                } else {
                    // ToDo: Needs to be recursive (as in JsonPrinter?)
                    for (DatasetFieldCompoundValue dscv : field.getDatasetFieldCompoundValues()) {
                        // compound values are of different types
                        JsonObjectBuilder child = Json.createObjectBuilder();

                        for (DatasetField dsf : dscv.getChildDatasetFields()) {
                            DatasetFieldType dsft = dsf.getDatasetFieldType();
                            if(excludeEmail && DatasetFieldType.FieldType.EMAIL.equals(dsft.getFieldType())) {
                                continue;
                            }
                            // which may have multiple values
                            if (!dsf.isEmpty()) {
                                // Add context entry 
                                //ToDo - also needs to recurse here?
                                JsonLDTerm subFieldName = getTermFor(dfType, dsft);
                                if (subFieldName.inNamespace()) {
                                    localContext.putIfAbsent(subFieldName.getNamespace().getPrefix(),
                                            subFieldName.getNamespace().getUrl());
                                } else {
                                    localContext.putIfAbsent(subFieldName.getLabel(), subFieldName.getUrl());
                                }

                                List<String> values = dsf.getValues_nondisplay();
                                if (values.size() > 1) {
                                    JsonArrayBuilder childVals = Json.createArrayBuilder();

                                    for (String val : dsf.getValues_nondisplay()) {
                                        childVals.add(val);
                                    }
                                    child.add(subFieldName.getLabel(), childVals);
                                } else {
                                    child.add(subFieldName.getLabel(), values.get(0));
                                }
                            }
                        }
                        vals.add(child);
                    }
                }
                // Add metadata value to aggregation, suppress array when only one value
                JsonArray valArray = vals.build();
                aggBuilder.add(fieldName.getLabel(), (valArray.size() != 1) ? valArray : valArray.get(0));
            }
        }
        // Add metadata related to the Dataset/DatasetVersion
        aggBuilder.add("@id", id)
                .add("@type",
                        Json.createArrayBuilder().add(JsonLDTerm.ore("Aggregation").getLabel())
                                .add(JsonLDTerm.schemaOrg("Dataset").getLabel()))
                .add(JsonLDTerm.schemaOrg("version").getLabel(), version.getFriendlyVersionNumber())
                .add(JsonLDTerm.schemaOrg("datePublished").getLabel(), dataset.getPublicationDateFormattedYYYYMMDD())
                .add(JsonLDTerm.schemaOrg("name").getLabel(), version.getTitle())
                .add(JsonLDTerm.schemaOrg("dateModified").getLabel(), version.getLastUpdateTime().toString());

        TermsOfUseAndAccess terms = version.getTermsOfUseAndAccess();
        if (terms.getLicense() == TermsOfUseAndAccess.License.CC0) {
            aggBuilder.add(JsonLDTerm.schemaOrg("license").getLabel(),
                    "https://creativecommons.org/publicdomain/zero/1.0/");
        } else {
            addIfNotNull(aggBuilder, JsonLDTerm.termsOfUse, terms.getTermsOfUse());
        }
        addIfNotNull(aggBuilder, JsonLDTerm.confidentialityDeclaration, terms.getConfidentialityDeclaration());
        addIfNotNull(aggBuilder, JsonLDTerm.specialPermissions, terms.getSpecialPermissions());
        addIfNotNull(aggBuilder, JsonLDTerm.restrictions, terms.getRestrictions());
        addIfNotNull(aggBuilder, JsonLDTerm.citationRequirements, terms.getCitationRequirements());
        addIfNotNull(aggBuilder, JsonLDTerm.depositorRequirements, terms.getDepositorRequirements());
        addIfNotNull(aggBuilder, JsonLDTerm.conditions, terms.getConditions());
        addIfNotNull(aggBuilder, JsonLDTerm.disclaimer, terms.getDisclaimer());

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
                BrandingUtil.getRootDataverseCollectionName());

        // The aggregation aggregates aggregatedresources (Datafiles) which each have
        // their own entry and metadata
        JsonArrayBuilder aggResArrayBuilder = Json.createArrayBuilder();

        for (FileMetadata fmd : version.getFileMetadatas()) {
            DataFile df = fmd.getDataFile();
            JsonObjectBuilder aggRes = Json.createObjectBuilder();

            if (fmd.getDescription() != null) {
                aggRes.add(JsonLDTerm.schemaOrg("description").getLabel(), fmd.getDescription());
            } else {
                addIfNotNull(aggRes, JsonLDTerm.schemaOrg("description"), df.getDescription());
            }
            addIfNotNull(aggRes, JsonLDTerm.schemaOrg("name"), fmd.getLabel()); // "label" is the filename
            addIfNotNull(aggRes, JsonLDTerm.restricted, fmd.isRestricted());
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
            if (df.getGlobalId().asString().length() != 0) {
                fileId = df.getGlobalId().asString();
                fileSameAs = SystemConfig.getDataverseSiteUrlStatic()
                        + "/api/access/datafile/:persistentId?persistentId=" + fileId;
            } else {
                fileId = SystemConfig.getDataverseSiteUrlStatic() + "/file.xhtml?fileId=" + df.getId();
                fileSameAs = SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + df.getId();
            }
            aggRes.add("@id", fileId);
            aggRes.add(JsonLDTerm.schemaOrg("sameAs").getLabel(), fileSameAs);
            fileArray.add(fileId);

            aggRes.add("@type", JsonLDTerm.ore("AggregatedResource").getLabel());
            addIfNotNull(aggRes, JsonLDTerm.schemaOrg("fileFormat"), df.getContentType());
            addIfNotNull(aggRes, JsonLDTerm.filesize, df.getFilesize());
            addIfNotNull(aggRes, JsonLDTerm.storageIdentifier, df.getStorageIdentifier());
            addIfNotNull(aggRes, JsonLDTerm.originalFileFormat, df.getOriginalFileFormat());
            addIfNotNull(aggRes, JsonLDTerm.originalFormatLabel, df.getOriginalFormatLabel());
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
            //Add latest resource to the array
            aggResArrayBuilder.add(aggRes.build());
        }
        // Build the '@context' object for json-ld based on the localContext entries
        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        for (Entry<String, String> e : localContext.entrySet()) {
            contextBuilder.add(e.getKey(), e.getValue());
        }
        // Now create the overall map object with it's metadata
        JsonObject oremap = Json.createObjectBuilder()
                .add(JsonLDTerm.dcTerms("modified").getLabel(), LocalDate.now().toString())
                .add(JsonLDTerm.dcTerms("creator").getLabel(),
                        BrandingUtil.getInstallationBrandName())
                .add("@type", JsonLDTerm.ore("ResourceMap").getLabel())
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
                .add("@context", contextBuilder.build()).build();
        return oremap;
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
        return getTermFor(DatasetFieldConstant.datasetContact, DatasetFieldConstant.datasetContactName);
    }

    public JsonLDTerm getContactEmailTerm() {
        return getTermFor(DatasetFieldConstant.datasetContact, DatasetFieldConstant.datasetContactEmail);
    }

    public JsonLDTerm getDescriptionTerm() {
        return getTermFor(DatasetFieldConstant.description);
    }

    public JsonLDTerm getDescriptionTextTerm() {
        return getTermFor(DatasetFieldConstant.description, DatasetFieldConstant.descriptionText);
    }

    private JsonLDTerm getTermFor(String fieldTypeName) {
        for (DatasetField dsf : version.getDatasetFields()) {
            DatasetFieldType dsft = dsf.getDatasetFieldType();
            if (dsft.getName().equals(fieldTypeName)) {
                return getTermFor(dsft);
            }
        }
        return null;
    }

    private JsonLDTerm getTermFor(DatasetFieldType dsft) {
        if (dsft.getUri() != null) {
            return new JsonLDTerm(dsft.getTitle(), dsft.getUri());
        } else {
            String namespaceUri = dsft.getMetadataBlock().getNamespaceUri();
            if (namespaceUri == null) {
                namespaceUri = SystemConfig.getDataverseSiteUrlStatic() + "/schema/" + dsft.getMetadataBlock().getName()
                        + "#";
            }
            JsonLDNamespace blockNamespace = new JsonLDNamespace(dsft.getMetadataBlock().getName(), namespaceUri);
            return new JsonLDTerm(blockNamespace, dsft.getTitle());
        }
    }

    private JsonLDTerm getTermFor(DatasetFieldType dfType, DatasetFieldType dsft) {
        if (dsft.getUri() != null) {
            return new JsonLDTerm(dsft.getTitle(), dsft.getUri());
        } else {
            // Use metadatablock URI or custom URI for this field based on the path
            String subFieldNamespaceUri = dfType.getMetadataBlock().getNamespaceUri();
            if (subFieldNamespaceUri == null) {
                subFieldNamespaceUri = SystemConfig.getDataverseSiteUrlStatic() + "/schema/"
                        + dfType.getMetadataBlock().getName() + "/";
            }
            subFieldNamespaceUri = subFieldNamespaceUri + dfType.getName() + "#";
            JsonLDNamespace fieldNamespace = new JsonLDNamespace(dfType.getName(), subFieldNamespaceUri);
            return new JsonLDTerm(fieldNamespace, dsft.getTitle());
        }
    }

    private JsonLDTerm getTermFor(String type, String subType) {
        for (DatasetField dsf : version.getDatasetFields()) {
            DatasetFieldType dsft = dsf.getDatasetFieldType();
            if (dsft.getName().equals(type)) {
                for (DatasetFieldCompoundValue dscv : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : dscv.getChildDatasetFields()) {
                        DatasetFieldType subFieldType = subField.getDatasetFieldType();
                        if (subFieldType.getName().equals(subType)) {
                            return getTermFor(dsft, subFieldType);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void injectSettingsService(SettingsServiceBean settingsSvc) {
        settingsService = settingsSvc;
    }
}
