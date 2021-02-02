package edu.harvard.iq.dataverse.util.json;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang.StringUtils;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess.License;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

public class JSONLDUtil {

    private static final Logger logger = Logger.getLogger(JSONLDUtil.class.getCanonicalName());

    /*
     * private static Map<String, String> populateContext(JsonValue json) {
     * Map<String, String> context = new TreeMap<String, String>(); if (json
     * instanceof JsonArray) { logger.warning("Array @context not yet supported"); }
     * else { for (String key : ((JsonObject) json).keySet()) {
     * context.putIfAbsent(key, ((JsonObject) json).getString(key)); } } return
     * context; }
     */

    public static JsonObject getContext(Map<String, String> contextMap) {
        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        for (Entry<String, String> e : contextMap.entrySet()) {
            contextBuilder.add(e.getKey(), e.getValue());
        }
        return contextBuilder.build();
    }

    public static Dataset updateDatasetMDFromJsonLD(Dataset ds, String jsonLDBody,
            MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc, boolean append, boolean migrating) {

        DatasetVersion dsv = new DatasetVersion();

        JsonObject jsonld = decontextualizeJsonLD(jsonLDBody);
        if(migrating) {
        Optional<GlobalId> maybePid = GlobalId.parse(jsonld.getString("@id"));
          if (maybePid.isPresent()) {
            ds.setGlobalId(maybePid.get());
          } else {
            // unparsable PID passed. Terminate.
            throw new BadRequestException("Cannot parse the @id '" + jsonld.getString("@id")
                    + "'. Make sure it is in valid form - see Dataverse Native API documentation.");
          }
        }

        dsv = updateDatasetVersionMDFromJsonLD(dsv, jsonld, metadataBlockSvc, datasetFieldSvc, append, migrating);
        dsv.setDataset(ds);

        List<DatasetVersion> versions = new ArrayList<>(1);
        versions.add(dsv);

        ds.setVersions(versions);
        if (migrating) {
            if (jsonld.containsKey(JsonLDTerm.schemaOrg("dateModified").getUrl())) {
                String dateString = jsonld.getString(JsonLDTerm.schemaOrg("dateModified").getUrl());
                LocalDateTime dateTime = getDateTimeFrom(dateString);
                ds.setModificationTime(Timestamp.valueOf(dateTime));
            }
            try {
                logger.fine("Output dsv: " + new OREMap(dsv, false).getOREMap().toString());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return ds;
    }

    public static DatasetVersion updateDatasetVersionMDFromJsonLD(DatasetVersion dsv, String jsonLDBody,
            MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc, boolean append, boolean migrating) {
        JsonObject jsonld = decontextualizeJsonLD(jsonLDBody);
        return updateDatasetVersionMDFromJsonLD(dsv, jsonld, metadataBlockSvc, datasetFieldSvc, append, migrating);
    }

    /**
     * 
     * @param dsv
     * @param jsonld
     * @param metadataBlockSvc
     * @param datasetFieldSvc
     * @param append           - if append, will add new top level field values for
     *                         multi-valued fields, if true and field type isn't
     *                         multiple, will fail. if false will replace all
     *                         value(s) for fields found in the json-ld.
     * @return
     */
    public static DatasetVersion updateDatasetVersionMDFromJsonLD(DatasetVersion dsv, JsonObject jsonld,
            MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc, boolean append, boolean migrating) {

        populateFieldTypeMap(metadataBlockSvc);

        // get existing ones?
        List<DatasetField> dsfl = dsv.getDatasetFields();
        Map<DatasetFieldType, DatasetField> fieldByTypeMap = new HashMap<DatasetFieldType, DatasetField>();
        for (DatasetField dsf : dsfl) {
            if (fieldByTypeMap.containsKey(dsf.getDatasetFieldType())) {
                // May have multiple values per field, but not multiple fields of one type?
                logger.warning("Multiple fields of type " + dsf.getDatasetFieldType().getName());
            }
            fieldByTypeMap.put(dsf.getDatasetFieldType(), dsf);
        }
        TermsOfUseAndAccess terms = dsv.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();

        for (String key : jsonld.keySet()) {
            if (!key.equals("@context")) {
                if (dsftMap.containsKey(key)) {

                    DatasetFieldType dsft = dsftMap.get(key);
                    DatasetField dsf = null;
                    if (fieldByTypeMap.containsKey(dsft)) {
                        dsf = fieldByTypeMap.get(dsft);
                        // If there's an existing field, we use it with append and remove it for !append
                        // unless it's multiple
                        if (!append && !dsft.isAllowMultiples()) {
                            dsfl.remove(dsf);
                            dsf=null;
                        }
                    }
                    if (dsf == null) {
                        dsf = new DatasetField();
                        dsfl.add(dsf);
                        dsf.setDatasetFieldType(dsft);
                    }

                    // Todo - normalize object vs. array
                    JsonArray valArray = getValues(jsonld.get(key), dsft.isAllowMultiples(), dsft.getName());

                    addField(dsf, valArray, dsft, datasetFieldSvc, append);

                    // assemble new terms, add to existing
                    // multivalue?
                    // compound?
                    // merge with existing dv metadata
                    // dsfl.add(dsf);
                } else {
                    // Internal/non-metadatablock terms
                    // Add metadata related to the Dataset/DatasetVersion

                    // ("@id", id) - check is equal to existing globalID?
                    // Add to 'md on original' ?
                    // (JsonLDTerm.schemaOrg("version").getLabel(),
                    // version.getFriendlyVersionNumber())
                    // Citation metadata?
                    // (JsonLDTerm.schemaOrg("datePublished").getLabel(),
                    // dataset.getPublicationDateFormattedYYYYMMDD())
                    // (JsonLDTerm.schemaOrg("name").getLabel())
                    // (JsonLDTerm.schemaOrg("dateModified").getLabel())

                    if (key.equals(JsonLDTerm.schemaOrg("datePublished").getUrl())&& migrating && !append) {
                        dsv.setVersionState(VersionState.RELEASED);
                    } else if (key.equals(JsonLDTerm.schemaOrg("version").getUrl())&& migrating && !append) {
                        String friendlyVersion = jsonld.getString(JsonLDTerm.schemaOrg("version").getUrl());
                        int index = friendlyVersion.indexOf(".");
                        if (index > 0) {
                            dsv.setVersionNumber(Long.parseLong(friendlyVersion.substring(0, index)));
                            dsv.setMinorVersionNumber(Long.parseLong(friendlyVersion.substring(index + 1)));
                        }
                    } else if (key.equals(JsonLDTerm.schemaOrg("license").getUrl())) {
                        if (!append || !isSet(terms, key)) {
                            // Mirror rules from SwordServiceBean
                            if (jsonld.containsKey(JsonLDTerm.termsOfUse.getUrl())) {
                                throw new BadRequestException(
                                        "Cannot specify " + JsonLDTerm.schemaOrg("license").getUrl() + " and "
                                                + JsonLDTerm.termsOfUse.getUrl());
                            }
                            setSemTerm(terms, key, TermsOfUseAndAccess.defaultLicense);
                        } else {
                            throw new BadRequestException(
                                    "Can't append to a single-value field that already has a value: "
                                            + JsonLDTerm.schemaOrg("license").getUrl());
                        }

                    } else if (datasetTerms.contains(key)) {
                        if (!append || !isSet(terms, key)) {
                            // Other Dataset-level TermsOfUseAndAccess
                            setSemTerm(terms, key, jsonld.getString(key));
                        } else {
                            throw new BadRequestException(
                                    "Can't append to a single-value field that already has a value: " + key);
                        }
                    } else if (key.equals(JsonLDTerm.fileTermsOfAccess.getUrl())) {
                        JsonObject fAccessObject = jsonld.getJsonObject(JsonLDTerm.fileTermsOfAccess.getUrl());
                        for (String fileKey : fAccessObject.keySet()) {
                            if (datafileTerms.contains(fileKey)) {
                                if (!append || !isSet(terms, fileKey)) {
                                    // Other DataFile-level TermsOfUseAndAccess
                                    if (fileKey.equals(JsonLDTerm.fileRequestAccess.getUrl())) {
                                        setSemTerm(terms, fileKey, fAccessObject.getBoolean(fileKey));
                                    } else {
                                        setSemTerm(terms, fileKey, fAccessObject.getString(fileKey));
                                    }
                                } else {
                                    throw new BadRequestException(
                                            "Can't append to a single-value field that already has a value: "
                                                    + fileKey);
                                }
                            }
                        }
                    } else {
                        if (dsftMap.containsKey(JsonLDTerm.metadataOnOrig.getUrl())) {
                            DatasetFieldType dsft = dsftMap.get(JsonLDTerm.metadataOnOrig.getUrl());

                            DatasetField dsf = null;
                            if (fieldByTypeMap.containsKey(dsft)) {
                                dsf = fieldByTypeMap.get(dsft);
                                // If there's an existing field, we use it with append and remove it for !append
                                // (except if multiple, which is not the default)
                                if (!append && !dsft.isAllowMultiples()) {
                                    dsfl.remove(dsf);
                                }
                            }
                            if (dsf == null) {
                                dsf = new DatasetField();
                                dsfl.add(dsf);
                                dsf.setDatasetFieldType(dsft);
                            }

                            List<DatasetFieldValue> vals = dsf.getDatasetFieldValues();

                            JsonObject currentValue = null;
                            DatasetFieldValue datasetFieldValue = null;
                            if (vals.isEmpty()) {
                                datasetFieldValue = new DatasetFieldValue();
                                vals.add(datasetFieldValue);
                                datasetFieldValue.setDatasetField(dsf);
                                dsf.setDatasetFieldValues(vals);

                                currentValue = Json.createObjectBuilder().build();
                            } else {
                                datasetFieldValue = vals.get(0);
                                JsonObject currentVal = decontextualizeJsonLD(datasetFieldValue.getValueForEdit());

                            }
                            currentValue.put(key, jsonld.get(key));
                            JsonObject newValue = recontextualizeJsonLD(currentValue, metadataBlockSvc);
                            datasetFieldValue.setValue(prettyPrint(newValue));
                        }
                    }
                    dsv.setTermsOfUseAndAccess(terms);
                    // move to new dataverse?
                    // aggBuilder.add(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel(),
                    // dataset.getDataverseContext().getDisplayName());

                }

            }
        }

        dsv.setDatasetFields(dsfl);

        return dsv;
    }
    /**
     * 
     * @param dsv
     * @param jsonLDBody
     * @param metadataBlockService
     * @param datasetFieldSvc
     * @param b
     * @param c
     * @return
     */
    public static DatasetVersion deleteDatasetVersionMDFromJsonLD(DatasetVersion dsv, String jsonLDBody,
            MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc) {
logger.info("deleteDatasetVersionMD");
        JsonObject jsonld = decontextualizeJsonLD(jsonLDBody);
        //All terms are now URIs
        //Setup dsftMap - URI to datasetFieldType map
        populateFieldTypeMap(metadataBlockSvc);


        //Another map - from datasetFieldType to an existing field in the dataset
        List<DatasetField> dsfl = dsv.getDatasetFields();
        Map<DatasetFieldType, DatasetField> fieldByTypeMap = new HashMap<DatasetFieldType, DatasetField>();
        for (DatasetField dsf : dsfl) {
            if (fieldByTypeMap.containsKey(dsf.getDatasetFieldType())) {
                // May have multiple values per field, but not multiple fields of one type?
                logger.warning("Multiple fields of type " + dsf.getDatasetFieldType().getName());
            }
            fieldByTypeMap.put(dsf.getDatasetFieldType(), dsf);
        }
        
        TermsOfUseAndAccess terms = dsv.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();

        //Iterate through input json
        for (String key : jsonld.keySet()) {
            //Skip context (shouldn't be present with decontextualize used above)
            if (!key.equals("@context")) {
                if (dsftMap.containsKey(key)) {
                    //THere's a field type with theis URI
                    DatasetFieldType dsft = dsftMap.get(key);
                    DatasetField dsf = null;
                    if (fieldByTypeMap.containsKey(dsft)) {
                        //There's a field of this type
                        dsf = fieldByTypeMap.get(dsft);

                        // Todo - normalize object vs. array
                        JsonArray valArray = getValues(jsonld.get(key), dsft.isAllowMultiples(), dsft.getName());
logger.info("Deleting: " + key + " : " + valArray.toString());
                        DatasetField dsf2 = getReplacementField(dsf, valArray);
                        if(dsf2 == null) {
                            //Exact match - remove the field
                            dsfl.remove(dsf);
                        } else {
                            //Partial match - some values of a multivalue field match, so keep the remaining values
                            dsfl.remove(dsf);
                            dsfl.add(dsf2);
                        }
                    }
                } else {
                    // Internal/non-metadatablock terms
                    if (key.equals(JsonLDTerm.schemaOrg("license").getUrl())) {
                        if(jsonld.getString(key).equals(TermsOfUseAndAccess.CC0_URI)) {
                            setSemTerm(terms, key, TermsOfUseAndAccess.License.NONE);
                        } else {
                            throw new BadRequestException(
                                    "Term: " + key + " with value: " + jsonld.getString(key) + " not found.");
                        }
                    } else if (datasetTerms.contains(key)) {
                        if(!deleteIfSemTermMatches(terms, key, jsonld.get(key))) {
                            throw new BadRequestException(
                                    "Term: " + key + " with value: " + jsonld.getString(key) + " not found.");
                        }
                    } else if (key.equals(JsonLDTerm.fileTermsOfAccess.getUrl())) {
                        JsonObject fAccessObject = jsonld.getJsonObject(JsonLDTerm.fileTermsOfAccess.getUrl());
                        for (String fileKey : fAccessObject.keySet()) {
                            if (datafileTerms.contains(fileKey)) {
                                if(!deleteIfSemTermMatches(terms, key, jsonld.get(key))) {
                                    throw new BadRequestException(
                                            "Term: " + key + " with value: " + jsonld.getString(key) + " not found.");
                                }
                            }
                        }
                    } else {
                        throw new BadRequestException(
                                "Term: " + key + " not found.");
                                    }
                    
                    dsv.setTermsOfUseAndAccess(terms);
                }
            }
        }
        dsv.setDatasetFields(dsfl);
        return dsv;
    }
    
    /**
     * 
     * @param dsf
     * @param valArray
     * @return null if exact match, otherwise return a field without the value to be deleted
     */
    private static DatasetField getReplacementField(DatasetField dsf, JsonArray valArray) {
        // TODO Auto-generated method stub
        return null;
    }

    private static void addField(DatasetField dsf, JsonArray valArray, DatasetFieldType dsft,
            DatasetFieldServiceBean datasetFieldSvc, boolean append) {

        if (append && !dsft.isAllowMultiples()) {
            if ((dsft.isCompound() && !dsf.getDatasetFieldCompoundValues().isEmpty())
                    || (dsft.isAllowControlledVocabulary() && !dsf.getControlledVocabularyValues().isEmpty())
                    || !dsf.getDatasetFieldValues().isEmpty()) {
                throw new BadRequestException(
                        "Can't append to a single-value field that already has a value: " + dsft.getName());
            }
        }
        logger.fine("Name: " + dsft.getName());
        logger.fine("val: " + valArray.toString());
        logger.fine("Compound: " + dsft.isCompound());
        logger.fine("CV: " + dsft.isAllowControlledVocabulary());

        if (dsft.isCompound()) {
            /*
             * List<DatasetFieldCompoundValue> vals = parseCompoundValue(type,
             * jsonld.get(key),testType); for (DatasetFieldCompoundValue dsfcv : vals) {
             * dsfcv.setParentDatasetField(ret); } dsf.setDatasetFieldCompoundValues(vals);
             */
            List<DatasetFieldCompoundValue> cvList = dsf.getDatasetFieldCompoundValues();
            if (!cvList.isEmpty()) {
                if (!append) {
                    cvList.clear();
                } else if (!dsft.isAllowMultiples() && cvList.size() == 1) {
                    // Trying to append but only a single value is allowed (and there already is
                    // one)
                    // (and we don't currently support appending new fields within a compound value)
                    throw new BadRequestException(
                            "Append with compound field with single value not yet supported: " + dsft.getDisplayName());
                }
            }

            List<DatasetFieldCompoundValue> vals = new LinkedList<>();
            for (JsonValue val : valArray) {
                if (!(val instanceof JsonObject)) {
                    throw new BadRequestException(
                            "Compound field values must be JSON objects, field: " + dsft.getName());
                }
                DatasetFieldCompoundValue cv = null;

                cv = new DatasetFieldCompoundValue();
                cv.setDisplayOrder(cvList.size());
                cvList.add(cv);
                cv.setParentDatasetField(dsf);

                JsonObject obj = (JsonObject) val;
                for (String childKey : obj.keySet()) {
                    if (dsftMap.containsKey(childKey)) {
                        DatasetFieldType childft = dsftMap.get(childKey);
                        if (!dsft.getChildDatasetFieldTypes().contains(childft)) {
                            throw new BadRequestException(
                                    "Compound field " + dsft.getName() + "can't include term " + childKey);
                        }
                        DatasetField childDsf = new DatasetField();
                        cv.getChildDatasetFields().add(childDsf);
                        childDsf.setDatasetFieldType(childft);
                        childDsf.setParentDatasetFieldCompoundValue(cv);

                        JsonArray childValArray = getValues(obj.get(childKey), childft.isAllowMultiples(),
                                childft.getName());
                        addField(childDsf, childValArray, childft, datasetFieldSvc, append);
                    }
                }
            }

        } else if (dsft.isControlledVocabulary()) {

            List<ControlledVocabularyValue> vals = dsf.getControlledVocabularyValues();
            for (JsonString strVal : valArray.getValuesAs(JsonString.class)) {
                String strValue = strVal.getString();
                ControlledVocabularyValue cvv = datasetFieldSvc
                        .findControlledVocabularyValueByDatasetFieldTypeAndStrValue(dsft, strValue, true);
                if (cvv == null) {
                    throw new BadRequestException(
                            "Unknown value for Controlled Vocab Field: " + dsft.getName() + " : " + strValue);
                }
                // Only add value to the list if it is not a duplicate
                if (strValue.equals("Other")) {
                    System.out.println("vals = " + vals + ", contains: " + vals.contains(cvv));
                }
                if (!vals.contains(cvv)) {
                    if (vals.size() > 0) {
                        cvv.setDisplayOrder(vals.size());
                    }
                    vals.add(cvv);
                    cvv.setDatasetFieldType(dsft);
                }
            }
            dsf.setControlledVocabularyValues(vals);

        } else {
            List<DatasetFieldValue> vals = dsf.getDatasetFieldValues();

            for (JsonString strVal : valArray.getValuesAs(JsonString.class)) {
                String strValue = strVal.getString();
                DatasetFieldValue datasetFieldValue = new DatasetFieldValue();

                datasetFieldValue.setDisplayOrder(vals.size());
                datasetFieldValue.setValue(strValue.trim());
                vals.add(datasetFieldValue);
                datasetFieldValue.setDatasetField(dsf);

            }
            dsf.setDatasetFieldValues(vals);
        }
    }

    private static JsonArray getValues(JsonValue val, boolean allowMultiples, String name) {
        JsonArray valArray = null;
        if (val instanceof JsonArray) {
            if ((((JsonArray) val).size() > 1) && !allowMultiples) {
                throw new BadRequestException("Array for single value notsupported: " + name);
            } else {
                valArray = (JsonArray) val;
            }
        } else {
            valArray = Json.createArrayBuilder().add(val).build();
        }
        return valArray;
    }

    static Map<String, String> localContext = new TreeMap<String, String>();
    static Map<String, DatasetFieldType> dsftMap = new TreeMap<String, DatasetFieldType>();

    private static void populateFieldTypeMap(MetadataBlockServiceBean metadataBlockSvc) {
        if (dsftMap.isEmpty()) {

            List<MetadataBlock> mdbList = metadataBlockSvc.listMetadataBlocks();

            for (MetadataBlock mdb : mdbList) {
                boolean blockHasUri = mdb.getNamespaceUri() != null;
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getUri() != null) {
                        dsftMap.put(dsft.getUri(), dsft);
                    }
                    if (blockHasUri) {
                        if (dsft.getParentDatasetFieldType() != null) {
                            // ToDo - why not getName for child type? Would have to fix in ORE generation
                            // code and handle legacy bags
                            dsftMap.put(mdb.getNamespaceUri() + dsft.getParentDatasetFieldType().getName() + "#"
                                    + dsft.getTitle(), dsft);
                        } else {
                            dsftMap.put(mdb.getNamespaceUri() + dsft.getTitle(), dsft);
                        }
                    }
                }
            }
            logger.fine("DSFT Map: " + String.join(", ", dsftMap.keySet()));
        }
    }

    public static void populateContext(MetadataBlockServiceBean metadataBlockSvc) {
        if (localContext.isEmpty()) {

            List<MetadataBlock> mdbList = metadataBlockSvc.listMetadataBlocks();

            for (MetadataBlock mdb : mdbList) {
                boolean blockHasUri = mdb.getNamespaceUri() != null;
                if (blockHasUri) {
                    JsonLDNamespace.defineNamespace(mdb.getName(), mdb.getNamespaceUri());
                }
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if ((dsft.getUri() != null) && !JsonLDNamespace.isInNamespace(dsft.getUri())) {
                        //Add term if uri exists and it's not in one of the namespaces already defined
                        localContext.putIfAbsent(dsft.getName(), dsft.getUri());
                    }
                }
            }
            JsonLDNamespace.addNamespacesToContext(localContext);
            logger.fine("LocalContext keys: " + String.join(", ", localContext.keySet()));
        }
    }

    public static JsonObject decontextualizeJsonLD(String jsonLDString) {
        logger.fine(jsonLDString);
        try (StringReader rdr = new StringReader(jsonLDString)) {

            // Use JsonLd to expand/compact to localContext
            JsonObject jsonld = Json.createReader(rdr).readObject();
            JsonDocument doc = JsonDocument.of(jsonld);
            JsonArray array = null;
            try {
                array = JsonLd.expand(doc).get();
                jsonld = JsonLd.compact(JsonDocument.of(array), JsonDocument.of(Json.createObjectBuilder().build()))
                        .get();
                // jsonld = array.getJsonObject(0);
                logger.fine("Decontextualized object: " + jsonld);
                return jsonld;
            } catch (JsonLdError e) {
                System.out.println(e.getMessage());
                return null;
            }
        }
    }

    private static JsonObject recontextualizeJsonLD(JsonObject jsonldObj, MetadataBlockServiceBean metadataBlockSvc) {

        populateContext(metadataBlockSvc);

        // Use JsonLd to expand/compact to localContext
        JsonDocument doc = JsonDocument.of(jsonldObj);
        JsonArray array = null;
        try {
            array = JsonLd.expand(doc).get();

            jsonldObj = JsonLd.compact(JsonDocument.of(array), JsonDocument.of(JSONLDUtil.getContext(localContext)))
                    .get();
            logger.fine("Compacted: " + jsonldObj.toString());
            return jsonldObj;
        } catch (JsonLdError e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static String prettyPrint(JsonValue val) {
        StringWriter sw = new StringWriter();
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
        JsonWriter jsonWriter = writerFactory.createWriter(sw);
        jsonWriter.write(val);
        jsonWriter.close();
        return sw.toString();
    }

//Modified from https://stackoverflow.com/questions/3389348/parse-any-date-in-java

    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {
        {
            put("^\\d{8}$", "yyyyMMdd");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        }
    };

    private static final Map<String, String> DATETIME_FORMAT_REGEXPS = new HashMap<String, String>() {
        {
            put("^\\d{12}$", "yyyyMMddHHmm");
            put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
            put("^\\d{14}$", "yyyyMMddHHmmss");
            put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$", "yyyy-MM-dd HH:mm:ss.SSS");
            put("^[a-z,A-Z]{3}\\s[a-z,A-Z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-z,A-Z]{3}\\s\\d{4}$",
                    "EEE MMM dd HH:mm:ss zzz yyyy"); // Wed Sep 23 19:33:46 UTC 2020

        }
    };

    /**
     * Determine DateTimeFormatter pattern matching with the given date string.
     * Returns null if format is unknown. You can simply extend DateUtil with more
     * formats if needed.
     * 
     * @param dateString The date string to determine the SimpleDateFormat pattern
     *                   for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     * @see SimpleDateFormat
     */
    public static DateTimeFormatter determineDateTimeFormat(String dateString) {
        for (String regexp : DATETIME_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DateTimeFormatter.ofPattern(DATETIME_FORMAT_REGEXPS.get(regexp));
            }
        }
        logger.warning("Unknown datetime format: " + dateString);
        return null; // Unknown format.
    }

    public static DateTimeFormatter determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DateTimeFormatter.ofPattern(DATE_FORMAT_REGEXPS.get(regexp));
            }
        }
        logger.warning("Unknown date format: " + dateString);
        return null; // Unknown format.
    }

    public static LocalDateTime getDateTimeFrom(String dateString) {
        DateTimeFormatter dtf = determineDateTimeFormat(dateString);
        if (dtf != null) {
            return LocalDateTime.parse(dateString, dtf);
        } else {
            dtf = determineDateFormat(dateString);
            if (dtf != null) {
                return LocalDate.parse(dateString, dtf).atStartOfDay();
            }
        }

        return null;
    }

    // Convenience methods for TermsOfUseAndAccess

    public static final List<String> datasetTerms = new ArrayList<String>(Arrays.asList(
            "https://dataverse.org/schema/core#termsOfUse",
            "https://dataverse.org/schema/core#confidentialityDeclaration",
            "https://dataverse.org/schema/core#specialPermissions", "https://dataverse.org/schema/core#restrictions",
            "https://dataverse.org/schema/core#citationRequirements",
            "https://dataverse.org/schema/core#depositorRequirements", "https://dataverse.org/schema/core#conditions",
            "https://dataverse.org/schema/core#disclaimer"));
    public static final List<String> datafileTerms = new ArrayList<String>(Arrays.asList(
            "https://dataverse.org/schema/core#termsOfAccess", "https://dataverse.org/schema/core#fileRequestAccess",
            "https://dataverse.org/schema/core#dataAccessPlace", "https://dataverse.org/schema/core#originalArchive",
            "https://dataverse.org/schema/core#availabilityStatus",
            "https://dataverse.org/schema/core#contactForAccess", "https://dataverse.org/schema/core#sizeOfCollection",
            "https://dataverse.org/schema/core#studyCompletion"));

    public static boolean isSet(TermsOfUseAndAccess terms, String semterm) {
        switch (semterm) {
        case "http://schema.org/license":
            return !terms.getLicense().equals(TermsOfUseAndAccess.License.NONE);
        case "https://dataverse.org/schema/core#termsOfUse":
            return StringUtils.isBlank(terms.getTermsOfUse());
        case "https://dataverse.org/schema/core#confidentialityDeclaration":
            return StringUtils.isBlank(terms.getConfidentialityDeclaration());
        case "https://dataverse.org/schema/core#specialPermissions":
            return StringUtils.isBlank(terms.getSpecialPermissions());
        case "https://dataverse.org/schema/core#restrictions":
            return StringUtils.isBlank(terms.getRestrictions());
        case "https://dataverse.org/schema/core#citationRequirements":
            return StringUtils.isBlank(terms.getCitationRequirements());
        case "https://dataverse.org/schema/core#depositorRequirements":
            return StringUtils.isBlank(terms.getDepositorRequirements());
        case "https://dataverse.org/schema/core#conditions":
            return StringUtils.isBlank(terms.getConditions());
        case "https://dataverse.org/schema/core#disclaimer":
            return StringUtils.isBlank(terms.getDisclaimer());
        case "https://dataverse.org/schema/core#termsOfAccess":
            return StringUtils.isBlank(terms.getTermsOfAccess());
        case "https://dataverse.org/schema/core#fileRequestAccess":
            return !terms.isFileAccessRequest();
        case "https://dataverse.org/schema/core#dataAccessPlace":
            return StringUtils.isBlank(terms.getDataAccessPlace());
        case "https://dataverse.org/schema/core#originalArchive":
            return StringUtils.isBlank(terms.getOriginalArchive());
        case "https://dataverse.org/schema/core#availabilityStatus":
            return StringUtils.isBlank(terms.getAvailabilityStatus());
        case "https://dataverse.org/schema/core#contactForAccess":
            return StringUtils.isBlank(terms.getContactForAccess());
        case "https://dataverse.org/schema/core#sizeOfCollection":
            return StringUtils.isBlank(terms.getSizeOfCollection());
        case "https://dataverse.org/schema/core#studyCompletion":
            return StringUtils.isBlank(terms.getStudyCompletion());
        default:
            logger.warning("isSet called for " + semterm);
            return false;
        }
    }

    public static void setSemTerm(TermsOfUseAndAccess terms, String semterm, Object value) {
        switch (semterm) {
        case "http://schema.org/license":
            // Mirror rules from SwordServiceBean
            if (((License) value).equals(TermsOfUseAndAccess.defaultLicense)) {
                terms.setLicense(TermsOfUseAndAccess.defaultLicense);
            } else {
                throw new BadRequestException("The only allowed value for " + JsonLDTerm.schemaOrg("license").getUrl()
                        + " is " + TermsOfUseAndAccess.CC0_URI);
            }
            break;
        case "https://dataverse.org/schema/core#termsOfUse":
            terms.setTermsOfUse((String) value);
            break;
        case "https://dataverse.org/schema/core#confidentialityDeclaration":
            terms.setConfidentialityDeclaration((String) value);
            break;
        case "https://dataverse.org/schema/core#specialPermissions":
            terms.setSpecialPermissions((String) value);
            break;
        case "https://dataverse.org/schema/core#restrictions":
            terms.setRestrictions((String) value);
            break;
        case "https://dataverse.org/schema/core#citationRequirements":
            terms.setCitationRequirements((String) value);
            break;
        case "https://dataverse.org/schema/core#depositorRequirements":
            terms.setDepositorRequirements((String) value);
            break;
        case "https://dataverse.org/schema/core#conditions":
            terms.setConditions((String) value);
            break;
        case "https://dataverse.org/schema/core#disclaimer":
            terms.setDisclaimer((String) value);
            break;
        case "https://dataverse.org/schema/core#termsOfAccess":
            terms.setTermsOfAccess((String) value);
            break;
        case "https://dataverse.org/schema/core#fileRequestAccess":
            terms.setFileAccessRequest((boolean) value);
            break;
        case "https://dataverse.org/schema/core#dataAccessPlace":
            terms.setDataAccessPlace((String) value);
            break;
        case "https://dataverse.org/schema/core#originalArchive":
            terms.setOriginalArchive((String) value);
            break;
        case "https://dataverse.org/schema/core#availabilityStatus":
            terms.setAvailabilityStatus((String) value);
            break;
        case "https://dataverse.org/schema/core#contactForAccess":
            terms.setContactForAccess((String) value);
            break;
        case "https://dataverse.org/schema/core#sizeOfCollection":
            terms.setSizeOfCollection((String) value);
            break;
        case "https://dataverse.org/schema/core#studyCompletion":
            terms.setStudyCompletion((String) value);
            break;
        default:
            logger.warning("setSemTerm called for " + semterm);
            break;
        }
    }

    private static boolean deleteIfSemTermMatches(TermsOfUseAndAccess terms, String semterm, JsonValue jsonValue) {
        boolean foundTerm=false;
        switch (semterm) {
        
        case "https://dataverse.org/schema/core#termsOfUse":
            if(terms.getTermsOfUse().equals(jsonValue.toString())) {
                terms.setTermsOfAccess(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#confidentialityDeclaration":
            if(terms.getConfidentialityDeclaration().equals(jsonValue.toString())) {
                terms.setConfidentialityDeclaration(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#specialPermissions":
            if(terms.getSpecialPermissions().equals(jsonValue.toString())) {
                terms.setSpecialPermissions(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#restrictions":
            if(terms.getRestrictions().equals(jsonValue.toString())) {
                terms.setRestrictions(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#citationRequirements":
            if(terms.getCitationRequirements().equals(jsonValue.toString())) {
                terms.setCitationRequirements(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#depositorRequirements":
            if(terms.getDepositorRequirements().equals(jsonValue.toString())) {
                terms.setDepositorRequirements(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#conditions":
            if(terms.getConditions().equals(jsonValue.toString())) {
                terms.setConditions(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#disclaimer":
            if(terms.getDisclaimer().equals(jsonValue.toString())) {
                terms.setDisclaimer(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#termsOfAccess":
            if(terms.getTermsOfAccess().equals(jsonValue.toString())) {
                terms.setTermsOfAccess(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#fileRequestAccess":
            if(terms.isFileAccessRequest() && (jsonValue.equals(JsonValue.TRUE))) {
                terms.setFileAccessRequest(false);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#dataAccessPlace":
            if(terms.getDataAccessPlace().equals(jsonValue.toString())) {
                terms.setDataAccessPlace(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#originalArchive":
            if(terms.getOriginalArchive().equals(jsonValue.toString())) {
                terms.setOriginalArchive(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#availabilityStatus":
            if(terms.getAvailabilityStatus().equals(jsonValue.toString())) {
                terms.setAvailabilityStatus(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#contactForAccess":
            if(terms.getContactForAccess().equals(jsonValue.toString())) {
                terms.setContactForAccess(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#sizeOfCollection":
            if(terms.getSizeOfCollection().equals(jsonValue.toString())) {
                terms.setSizeOfCollection(null);
                foundTerm=true;
            }
            break;
        case "https://dataverse.org/schema/core#studyCompletion":
            if(terms.getStudyCompletion().equals(jsonValue.toString())) {
                terms.setStudyCompletion(null);
                foundTerm=true;
            }
            break;
        default:
            logger.warning("deleteIfSemTermMatches called for " + semterm);
            break;
        }
        return foundTerm; 
    }

}
