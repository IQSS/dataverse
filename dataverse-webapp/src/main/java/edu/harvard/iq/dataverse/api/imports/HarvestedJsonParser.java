package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import io.vavr.control.Option;
import org.apache.commons.lang3.EnumUtils;

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
            dsv.setDatasetFields(parseMetadataBlocks(obj.getJsonObject("metadataBlocks")));


            FileTermsOfUse oldLicense = parseLicense(obj.getString("license", null))
                    .getOrElse(termsOfUseFactory.createUnknownTermsOfUse());

            JsonArray filesJson = obj.getJsonArray("files");

            if (filesJson != null) {
                List<FileMetadata> parsedMetadataFiles = parseFiles(filesJson, dsv, oldLicense);

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

        return Option.none();
    }

    private List<DatasetField> parseMetadataBlocks(JsonObject json) {
        Set<String> keys = json.keySet();
        List<DatasetField> fields = new LinkedList<>();

        for (String blockName : keys) {
            JsonObject blockJson = json.getJsonObject(blockName);
            JsonArray fieldsJson = blockJson.getJsonArray("fields");
            fields.addAll(parseFieldsFromArray(fieldsJson));
        }
        return fields;
    }

    private List<DatasetField> parseFieldsFromArray(JsonArray fieldsArray) {
        List<DatasetField> fields = new LinkedList<>();
        for (JsonObject fieldJson : fieldsArray.getValuesAs(JsonObject.class)) {
            List<DatasetField> c = parseField(fieldJson);
            fields.addAll(c);

        }
        return fields;

    }

    private List<DatasetField> parseField(JsonObject json) {
        if (json == null) {
            return null;
        }

        DatasetFieldType type = datasetFieldSvc.findByNameOpt(json.getString("typeName", ""));


        if (type == null || isUnrecoverableTypeMismatch(json, type)) {
            return Collections.emptyList();
        }

        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(type);

        ArrayList<DatasetField> parsedFields = new ArrayList<>();

        if (type.isCompound()) {
            parsedFields.addAll(parseCompoundValue(type, json));
        } else if (type.isControlledVocabulary()) {
            List<ControlledVocabularyValue> vals = parseControlledVocabularyValue(type, json);
            if (vals.size() > 0) {
                datasetField.setControlledVocabularyValues(vals);
                parsedFields.add(datasetField);
            }
        } else {
            // primitive
            List<DatasetField> values = parsePrimitiveValue(type, json);
            parsedFields.addAll(values);
        }

        return parsedFields;
    }

    private boolean isUnrecoverableTypeMismatch(JsonObject json, DatasetFieldType type) {
        return (type.isCompound() && !json.getString("typeClass").equals("compound")) ||
                (!type.isControlledVocabulary() && type.isPrimitive() && !json.getString("typeClass").equals("primitive"));
    }

    private List<ControlledVocabularyValue> parseControlledVocabularyValue(DatasetFieldType cvvType, JsonObject json) {
        JsonValue valueFieldValue = json.get("value");
        if (valueFieldValue == null) {
            return Collections.emptyList();
        }

        if (cvvType.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            return parseControlledVocabularyMultiValues(cvvType, json);
        } else if (!cvvType.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            List<ControlledVocabularyValue> vals = parseControlledVocabularyMultiValues(cvvType, json);
            for (ControlledVocabularyValue val : vals) {
                if (!"other".equalsIgnoreCase(val.getStrValue())) {
                    return Collections.singletonList(val);
                }
            }
            return vals.size() > 0 ? Collections.singletonList(vals.get(0)) : Collections.emptyList();
        } else {
            return parseControlledVocabularySingleValue(cvvType, json);
        }
    }

    private List<ControlledVocabularyValue> parseControlledVocabularyMultiValues(DatasetFieldType cvvType, JsonObject json) {
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

    private List<ControlledVocabularyValue> parseControlledVocabularySingleValue(DatasetFieldType cvvType, JsonObject json) {
        String strValue = json.getString("value");
        ControlledVocabularyValue cvv = datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(cvvType, strValue, true);
        if (cvv == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(cvv);
    }

    private List<DatasetField> parseCompoundValue(DatasetFieldType compoundType, JsonObject json) {
        List<DatasetField> vals = new LinkedList<>();
        JsonValue valueFieldValue = json.get("value");
        if (valueFieldValue == null) {
            return vals;
        }

        if (compoundType.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            vals.addAll(parseCompoundMultiValueField(compoundType, json));
        } else if (!compoundType.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            List<DatasetField> multiValues = parseCompoundMultiValueField(compoundType, json);
            if (multiValues.size() > 0) {
                vals.add(multiValues.get(0));
            }
        } else {
            vals.add(parseCompoundSingleValueField(compoundType, json));
        }

        return vals;
    }

    private DatasetField parseCompoundSingleValueField(DatasetFieldType compoundType, JsonObject json) {
        JsonObject value = json.getJsonObject("value");
        DatasetField parentField = new DatasetField();
        parentField.setDatasetFieldType(compoundType);

        for (String key : value.keySet()) {
            JsonObject childFieldJson = value.getJsonObject(key);
            DatasetField childField = parseField(childFieldJson).get(0);

            if (!compoundType.getChildDatasetFieldTypes().contains(childField.getDatasetFieldType())) {
                continue;
            }

            parentField.getDatasetFieldsChildren().add(childField);
            childField.setDatasetFieldParent(parentField);
        }

        return parentField;
    }

    private List<DatasetField> parseCompoundMultiValueField(DatasetFieldType compoundType, JsonObject json) {
        List<DatasetField> vals = new LinkedList<>();
        for (JsonObject obj : json.getJsonArray("value").getValuesAs(JsonObject.class)) {
            DatasetField parentField = new DatasetField();
            parentField.setDatasetFieldType(compoundType);

            for (String fieldName : obj.keySet()) {
                JsonObject childFieldJson = obj.getJsonObject(fieldName);
                List<DatasetField> childFields = parseField(childFieldJson);
                if (childFields.isEmpty()) {
                    continue;
                }

                DatasetField childField = childFields.get(0);
                if (!compoundType.getChildDatasetFieldTypes().contains(childField.getDatasetFieldType())) {
                    continue;
                }

                parentField.getDatasetFieldsChildren().add(childField);
                childField.setDatasetFieldParent(parentField);
            }
            if (parentField.getDatasetFieldsChildren().size() > 0) {
                vals.add(parentField);
            }
        }
        return vals;
    }

    private List<DatasetField> parsePrimitiveValue(DatasetFieldType dft, JsonObject json) {

        List<DatasetField> vals = new LinkedList<>();
        JsonValue valueFieldValue = json.get("value");
        if (valueFieldValue == null) {
            return vals;
        }

        if (dft.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            vals.addAll(parsePrimitiveMultiValues(dft, json));
        } else if (!dft.isAllowMultiples() && valueFieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            List<DatasetField> multiValues = parsePrimitiveMultiValues(dft, json);
            if (multiValues.size() > 0) {
                vals.add(multiValues.get(0));
            }
        } else {
            vals.add(parsePrimitiveSingleValue(dft, json));
        }

        return vals;
    }

    private List<DatasetField> parsePrimitiveMultiValues(DatasetFieldType dft, JsonObject json) {
        List<DatasetField> vals = new LinkedList<>();
        for (JsonString val : json.getJsonArray("value").getValuesAs(JsonString.class)) {
            DatasetField datasetFieldValue = new DatasetField();
            datasetFieldValue.setDisplayOrder(vals.size());
            datasetFieldValue.setFieldValue(val.getString().trim());
            datasetFieldValue.setDatasetFieldType(dft);
            vals.add(datasetFieldValue);
        }
        return vals;
    }

    private DatasetField parsePrimitiveSingleValue(DatasetFieldType dft, JsonObject json) {
        DatasetField datasetFieldValue = new DatasetField();
        datasetFieldValue.setFieldValue(json.getString("value").trim());
        datasetFieldValue.setDatasetFieldType(dft);

        return datasetFieldValue;
    }

    private List<FileMetadata> parseFiles(JsonArray metadatasJson, DatasetVersion dsv, FileTermsOfUse termsOfUseFallback) throws JsonParseException {
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

                fileMetadata.setTermsOfUse(parseFileTermsOfUse(filemetadataJson).getOrElse(termsOfUseFallback));

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

    private Option<FileTermsOfUse> parseFileTermsOfUse(JsonObject filemetadataJson) {

        String termsOfUseType = filemetadataJson.getString("termsOfUseType", FileTermsOfUse.TermsOfUseType.TERMS_UNKNOWN.name());

        if (FileTermsOfUse.TermsOfUseType.LICENSE_BASED.name().equals(termsOfUseType)) {
            String licenseName = filemetadataJson.getString("licenseName");
            return Option.ofOptional(termsOfUseFactory.createTermsOfUseWithExistingLicense(licenseName));
        }

        if (FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.name().equals(termsOfUseType)) {
            return Option.some(termsOfUseFactory.createAllRightsReservedTermsOfUse());
        }

        if (FileTermsOfUse.TermsOfUseType.RESTRICTED.name().equals(termsOfUseType)){
            String accessConditions = filemetadataJson.getString("accessConditions");

            if (!EnumUtils.isValidEnum(FileTermsOfUse.RestrictType.class, accessConditions)) {
                return Option.none();
            }
            FileTermsOfUse.RestrictType restrictType = FileTermsOfUse.RestrictType.valueOf(accessConditions);
                
            if (FileTermsOfUse.RestrictType.CUSTOM == restrictType){
                String accessConditionsCustomText = filemetadataJson.getString("accessConditionsCustomText");
                return Option.some(termsOfUseFactory.createRestrictedCustomTermsOfUse(accessConditionsCustomText));
            } else {
                return Option.some(termsOfUseFactory.createRestrictedTermsOfUse(restrictType));
            }

        }
        
        return Option.none();
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
