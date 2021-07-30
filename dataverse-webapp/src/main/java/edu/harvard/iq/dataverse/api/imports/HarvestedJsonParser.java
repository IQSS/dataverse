package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.CompoundVocabularyException;
import edu.harvard.iq.dataverse.util.json.ControlledVocabularyException;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import io.vavr.control.Option;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class HarvestedJsonParser {

    private final Logger logger = Logger.getLogger(HarvestedJsonParser.class.getName());

    private SettingsServiceBean settingsService;
    private DatasetFieldServiceBean datasetFieldSvc;
    private TermsOfUseFactory termsOfUseFactory;

    public HarvestedJsonParser() {
    }

    @Inject
    public HarvestedJsonParser(SettingsServiceBean settingsService, DatasetFieldServiceBean datasetFieldSvc, TermsOfUseFactory termsOfUseFactory) {
        this.settingsService = settingsService;
        this.datasetFieldSvc = datasetFieldSvc;
        this.termsOfUseFactory = termsOfUseFactory;
    }

    public Dataset parseDataset(String json) throws JsonParseException {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();

        Dataset dataset = new Dataset();

        dataset.setAuthority(obj.getString("authority", null) == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Authority) : obj.getString("authority"));
        dataset.setProtocol(obj.getString("protocol", null) == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Protocol) : obj.getString("protocol"));
        dataset.setIdentifier(obj.getString("identifier", null));

        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(dataset);
        dsv = parseDatasetVersion(obj.getJsonObject("datasetVersion"), dsv);
        List<DatasetVersion> versions = new ArrayList<>(1);
        versions.add(dsv);

        dataset.setVersions(versions);
        return dataset;
    }

    private DatasetVersion parseDatasetVersion(JsonObject obj, DatasetVersion dsv) throws JsonParseException {
        try {

            String archiveNote = obj.getString("archiveNote", null);
            if (archiveNote != null) {
                dsv.setArchiveNote(archiveNote);
            }

            dsv.setDeaccessionLink(obj.getString("deaccessionLink", null));
            int versionNumberInt = obj.getInt("versionNumber", -1);
            Long versionNumber = null;
            if (versionNumberInt != -1) {
                versionNumber = (long) versionNumberInt;
            }
            dsv.setVersionNumber(versionNumber);
            dsv.setMinorVersionNumber(Option.of(obj.getString("minorVersionNumber", null)).map(Long::parseLong).getOrNull());
            // if the existing datasetversion doesn not have an id
            // use the id from the json object.
            if (dsv.getId() == null) {
                dsv.setId(Option.of(obj.getString("id", null)).map(Long::parseLong).getOrNull());
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
            dsv.setTermsOfUseAndAccess(new TermsOfUseAndAccess());

            dsv.setDatasetFields(parseMetadataBlocks(obj.getJsonObject("metadataBlocks")));

            JsonArray filesJson = obj.getJsonArray("files");

            Option<FileTermsOfUse> oldLicense = parseLicense(obj.getString("license", null));

            if (filesJson == null) {
                filesJson = obj.getJsonArray("fileMetadatas");
            }
            if (filesJson != null) {
                List<FileMetadata> parsedMetadataFiles = parseFiles(filesJson, dsv, oldLicense);
                dsv.setFileMetadatas(new LinkedList<>());
                for (FileMetadata parsedMetadataFile : parsedMetadataFiles) {
                    dsv.addFileMetadata(parsedMetadataFile);
                }
            }
            return dsv;

        } catch (ParseException ex) {
            throw new JsonParseException("Error parsing date:" + ex.getMessage(), ex);
        } catch (NumberFormatException ex) {
            throw new JsonParseException("Error parsing number:" + ex.getMessage(), ex);
        }
    }

    private Date parseDate(String str) throws ParseException {
        return str == null ? null : Util.getDateFormat().parse(str);
    }

    private Date parseTime(String str) throws ParseException {
        return str == null ? null : Util.getDateTimeFormat().parse(str);
    }

    private Option<FileTermsOfUse> parseLicense(String inString) {
        if (inString != null && inString.equalsIgnoreCase("CC0")) {
            return Option.of(termsOfUseFactory.createTermsOfUseFromCC0License());
        }

        if (inString != null && inString.equalsIgnoreCase("NONE")) {
            return Option.of(termsOfUseFactory.createUnknownTermsOfUse());
        }

        return Option.none();
    }

    private List<DatasetField> parseMetadataBlocks(JsonObject json) throws JsonParseException {
        Set<String> keys = json.keySet();
        List<DatasetField> fields = new LinkedList<>();

        for (String blockName : keys) {
            JsonObject blockJson = json.getJsonObject(blockName);
            JsonArray fieldsJson = blockJson.getJsonArray("fields");
            fields.addAll(parseFieldsFromArray(fieldsJson));
        }
        return fields;
    }

    private List<DatasetField> parseFieldsFromArray(JsonArray fieldsArray) throws JsonParseException {
        List<DatasetField> fields = new LinkedList<>();
        for (JsonObject fieldJson : fieldsArray.getValuesAs(JsonObject.class)) {
            try {
                List<DatasetField> c = parseField(fieldJson, true);
                fields.addAll(c);
            } catch (CompoundVocabularyException ex) {
                DatasetFieldType fieldType = datasetFieldSvc.findByNameOpt(fieldJson.getString("typeName", ""));
                if ((DatasetFieldConstant.geographicCoverage).equals(fieldType.getName())) {
                    fields.addAll(remapGeographicCoverage(ex));
                } else {
                    // if not lenient mode, re-throw exception
                    throw ex;
                }
            }

        }
        return fields;

    }

    /**
     * Special processing for GeographicCoverage compound field:
     * Handle parsing exceptions caused by invalid controlled vocabulary in the "country" field by
     * putting the invalid data in "otherGeographicCoverage" in a new compound value.
     *
     * @param ex - contains the invalid values to be processed
     * @return a compound DatasetFields that contains the newly created values, in addition to
     * the original valid values.
     * @throws JsonParseException
     */
    private List<DatasetField> remapGeographicCoverage(CompoundVocabularyException ex) throws JsonParseException {
        List<Set<FieldDTO>> geoCoverageList = new ArrayList<>();
        // For each exception, create HashSet of otherGeographic Coverage and add to list
        for (ControlledVocabularyException vocabEx : ex.getExList()) {
            Set<FieldDTO> set = new HashSet<>();
            set.add(FieldDTO.createPrimitiveFieldDTO(DatasetFieldConstant.otherGeographicCoverage, vocabEx.getStrValue()));
            geoCoverageList.add(set);
        }
        FieldDTO geoCoverageDTO = FieldDTO.createMultipleCompoundFieldDTO(DatasetFieldConstant.geographicCoverage, geoCoverageList);

        // convert DTO to datasetField so we can back valid values.
        Gson gson = new Gson();
        String jsonString = gson.toJson(geoCoverageDTO);
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject obj = jsonReader.readObject();

        return parseField(obj, true);
    }

    private List<DatasetField> parseField(JsonObject json, Boolean testType) throws JsonParseException {
        if (json == null) {
            return null;
        }

        DatasetFieldType type = datasetFieldSvc.findByNameOpt(json.getString("typeName", ""));


        if (type == null) {
            throw new JsonParseException("Can't find type '" + json.getString("typeName", "") + "'");
        }
        if (testType && type.isAllowMultiples() != json.getBoolean("multiple")) {
            throw new JsonParseException("incorrect multiple   for field " + json.getString("typeName", ""));
        }
        if (testType && type.isCompound() && !json.getString("typeClass").equals("compound") && !type.isControlledVocabulary()) {
            throw new JsonParseException("incorrect  typeClass for field " + json.getString("typeName", "") + ", should be compound.");
        }
        if (testType && !type.isControlledVocabulary() && type.isPrimitive() && !json.getString("typeClass").equals("primitive")) {
            throw new JsonParseException("incorrect  typeClass for field: " + json.getString("typeName", "") + ", should be primitive");
        }
        if (testType && type.isControlledVocabulary() && !json.getString("typeClass").equals("controlledVocabulary")) {
            throw new JsonParseException("incorrect  typeClass for field " + json.getString("typeName", "") + ", should be controlledVocabulary");
        }

        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(type);

        ArrayList<DatasetField> parsedFields = new ArrayList<>();

        if (type.isCompound()) {
            parsedFields.addAll(parseCompoundValue(type, json, testType));
        } else if (type.isControlledVocabulary()) {
            List<ControlledVocabularyValue> vals = parseControlledVocabularyValue(type, json);
            for (ControlledVocabularyValue cvv : vals) {
                cvv.setDatasetFieldType(type);
            }
            datasetField.setControlledVocabularyValues(vals);
            parsedFields.add(datasetField);

        } else {
            // primitive
            List<DatasetField> values = parsePrimitiveValue(type, json);

            if (values.size() == 1) {
                datasetField.setFieldValue(values.get(0).getValue());
                parsedFields.add(datasetField);
            } else {
                parsedFields.addAll(values);
            }
        }

        return parsedFields;
    }

    public List<ControlledVocabularyValue> parseControlledVocabularyValue(DatasetFieldType cvvType, JsonObject json) throws JsonParseException {
        try {
            if (cvvType.isAllowMultiples()) {
                try {
                    return parseControlledVocabularyValues(cvvType, json);
                } catch (ClassCastException cce) {
                    return parseSingleControlledVocabularyValue(cvvType, json);
                }
            } else {
                try {
                    return parseSingleControlledVocabularyValue(cvvType, json);
                } catch (ClassCastException cce) {
                    List<ControlledVocabularyValue> vals = parseControlledVocabularyValues(cvvType, json);
                    for (ControlledVocabularyValue val : vals) {
                        if (!"other".equalsIgnoreCase(val.getStrValue())) {
                            return Collections.singletonList(val);
                        }
                    }
                    return vals.size() > 0 ? Collections.singletonList(vals.get(0)) : Collections.emptyList();
                }
            }
        } catch (ClassCastException cce) {
            throw new JsonParseException("Invalid values submitted for " + cvvType.getName());
        }
    }

    private List<ControlledVocabularyValue> parseControlledVocabularyValues(DatasetFieldType cvvType, JsonObject json) throws ClassCastException {
        List<ControlledVocabularyValue> vals = new LinkedList<>();
        for (JsonString strVal : json.getJsonArray("value").getValuesAs(JsonString.class)) {
            String strValue = strVal.getString();
            ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(cvvType, strValue, true);
            if (cvv == null) {
                continue;
            }
            // Only add value to the list if it is not a duplicate
            if (strValue.equalsIgnoreCase("other")) {
                System.out.println("vals = " + vals + ", contains: " + vals.contains(cvv));
            }
            if (!vals.contains(cvv)) {
                vals.add(cvv);
            }
        }
        return vals;
    }

    private List<ControlledVocabularyValue> parseSingleControlledVocabularyValue(DatasetFieldType cvvType, JsonObject json) throws ClassCastException {
        String strValue = json.getString("value");
        ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(cvvType, strValue, true);
        if (cvv == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(cvv);
    }

    private List<DatasetField> parseCompoundValue(DatasetFieldType compoundType, JsonObject json, Boolean testType) throws JsonParseException {
        List<ControlledVocabularyException> vocabExceptions = new ArrayList<>();
        List<DatasetField> vals = new LinkedList<>();
        if (compoundType.isAllowMultiples()) {
            try {
                json.getJsonArray("value").getValuesAs(JsonObject.class);
            } catch (ClassCastException cce) {
                throw new JsonParseException("Invalid values submitted for " + compoundType.getName() + ". It should be an array of values.");
            }

            JsonArray value = json.getJsonArray("value");
            System.out.println(value);

            for (JsonObject obj : json.getJsonArray("value").getValuesAs(JsonObject.class)) {
                DatasetField parentField = new DatasetField();
                parentField.setDatasetFieldType(compoundType);

                for (String fieldName : obj.keySet()) {
                    JsonObject childFieldJson = obj.getJsonObject(fieldName);
                    DatasetField childField = null;
                    try {
                        childField = parseField(childFieldJson, testType).get(0);
                    } catch (ControlledVocabularyException ex) {
                        vocabExceptions.add(ex);
                    }

                    if (childField != null) {
                        if (!compoundType.getChildDatasetFieldTypes().contains(childField.getDatasetFieldType())) {
                            throw new JsonParseException("field " + childField.getDatasetFieldType().getName() + " is not a child of " + compoundType.getName());
                        }

                        parentField.getDatasetFieldsChildren().add(childField);
                        childField.setDatasetFieldParent(parentField);
                    }
                }
                vals.add(parentField);
            }


        } else {
            JsonObject value = json.getJsonObject("value");
            DatasetField parentField = new DatasetField();
            parentField.setDatasetFieldType(compoundType);

            for (String key : value.keySet()) {
                JsonObject childFieldJson = value.getJsonObject(key);
                DatasetField childField = null;
                try {
                    childField = parseField(childFieldJson, testType).get(0);
                } catch (ControlledVocabularyException ex) {
                    vocabExceptions.add(ex);
                }
                if (childField != null) {
                    parentField.getDatasetFieldsChildren().add(childField);
                    childField.setDatasetFieldParent(parentField);
                }
            }

            vals.add(parentField);
        }
        if (!vocabExceptions.isEmpty()) {
            throw new CompoundVocabularyException("Invalid controlled vocabulary in compound field ", vocabExceptions, vals);
        }
        return vals;
    }

    private List<DatasetField> parsePrimitiveValue(DatasetFieldType dft, JsonObject json) throws JsonParseException {

        List<DatasetField> vals = new LinkedList<>();
        if (dft.isAllowMultiples() && json.get("value").getValueType().equals(JsonValue.ValueType.ARRAY)) {

            for (JsonString val : json.getJsonArray("value").getValuesAs(JsonString.class)) {
                DatasetField datasetFieldValue = new DatasetField();
                datasetFieldValue.setDisplayOrder(vals.size() - 1);
                datasetFieldValue.setFieldValue(val.getString().trim());
                datasetFieldValue.setDatasetFieldType(dft);
                vals.add(datasetFieldValue);
            }

        } else {
            try {
                json.getString("value");
            } catch (ClassCastException cce) {
                throw new JsonParseException("Invalid value submitted for " + dft.getName() + ". It should be a single value.");
            }
            DatasetField datasetFieldValue = new DatasetField();
            datasetFieldValue.setFieldValue(json.getString("value", "").trim());
            datasetFieldValue.setDatasetFieldType(dft);
            vals.add(datasetFieldValue);
        }

        return vals;
    }

    private List<FileMetadata> parseFiles(JsonArray metadatasJson, DatasetVersion dsv, Option<FileTermsOfUse> license) throws JsonParseException {
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

                boolean isLicenseAvailableAndSet = setDatasetBasedLicense(license, fileMetadata);

                if (!isLicenseAvailableAndSet) {
                    setFileBasedLicense(filemetadataJson, fileMetadata);
                }

                if (filemetadataJson.containsKey("dataFile")) {
                    DataFile dataFile = parseDataFile(filemetadataJson.getJsonObject("dataFile"));
                    dataFile.getFileMetadatas().add(fileMetadata);
                    dataFile.setOwner(dsv.getDataset());
                    fileMetadata.setDataFile(dataFile);
                    if (dsv.getDataset().getFiles() == null) {
                        dsv.getDataset().setFiles(new ArrayList<>());
                    }
                    dsv.getDataset().getFiles().add(dataFile);
                }


                fileMetadatas.add(fileMetadata);
                fileMetadata.setCategories(getCategories(filemetadataJson, dsv.getDataset()));
            }
        }

        return fileMetadatas;
    }

    private Boolean setDatasetBasedLicense(Option<FileTermsOfUse> license, FileMetadata fileMetadata) {
        return license.toStream()
                .map(datasetLicense -> {
                    fileMetadata.setTermsOfUse(datasetLicense);
                    return true;
                }).getOrElse(false);
    }

    private void setFileBasedLicense(JsonObject filemetadataJson, FileMetadata fileMetadata) {
        String termsOfUseType = filemetadataJson.getString("termsOfUseType");

        if (FileTermsOfUse.TermsOfUseType.LICENSE_BASED.name().equals(termsOfUseType)) {
            fileMetadata.setTermsOfUse(termsOfUseFactory.createTermsOfUseWithExistingLicense(filemetadataJson.getString("licenseName")));
        }
        if (FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.name().equals(termsOfUseType)) {
            fileMetadata.setTermsOfUse(termsOfUseFactory.createAllRightsReservedTermsOfUse());
        }
        if (FileTermsOfUse.TermsOfUseType.RESTRICTED.name().equals(termsOfUseType)) {
            final String accessConditions = filemetadataJson.getString("accessConditions");

            if (FileTermsOfUse.RestrictType.CUSTOM.name().equals(accessConditions)) {
                fileMetadata.setTermsOfUse(termsOfUseFactory.createRestrictedCustomTermsOfUse(filemetadataJson.getString("accessConditionsCustomText")));
            } else {
                fileMetadata.setTermsOfUse(termsOfUseFactory.createRestrictedTermsOfUse(FileTermsOfUse.RestrictType.valueOf(accessConditions)));
            }
        }
    }

    private List<DataFileCategory> getCategories(JsonObject filemetadataJson, Dataset dataset) {
        JsonArray categories = filemetadataJson.getJsonArray("categories");
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

    private DataFile parseDataFile(JsonObject datafileJson) {
        DataFile dataFile = new DataFile();

        Timestamp timestamp = new Timestamp(new Date().getTime());
        dataFile.setCreateDate(timestamp);
        dataFile.setModificationTime(timestamp);
        dataFile.setPermissionModificationTime(timestamp);

        if (datafileJson.containsKey("filesize")) {
            dataFile.setFilesize(datafileJson.getJsonNumber("filesize").longValueExact());
        }

        String contentType = datafileJson.getString("contentType", null);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String storageIdentifier = datafileJson.getString("storageIdentifier", " ");
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
}
