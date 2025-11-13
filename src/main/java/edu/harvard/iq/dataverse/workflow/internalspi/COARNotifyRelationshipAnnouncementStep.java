package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import static edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult.OK;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.apache.commons.lang3.Strings;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * A workflow step that generates and sends a COAR Notify Relationship
 * Announcement message to the inbox of a configured target (using the Linked
 * Data Notification standard). An example use case is for Dataverse to anounce
 * new dataset versions to the DSpace-based Harvard DASH preprint repository so
 * that a DASH admin can create a backlink for any dataset versions that
 * reference a DASH deposit or a paper with a DOI where DASH has a preprint
 * copy.
 * 
 * @author qqmyers
 */

public class COARNotifyRelationshipAnnouncementStep implements WorkflowStep {
    private static final Logger logger = Logger.getLogger(COARNotifyRelationshipAnnouncementStep.class.getName());
    /*
     * Keeping db settings since only DB settings are supported in workflows at
     * present. Going forward, it might make sense to have a config model that links
     * the trigger field and targets (so different fields can trigger notices to
     * different types of target repositories/services)
     */
    private static final String REQUIRED_FIELDS = ":COARNotifyRelationshipAnnouncementTriggerFields";
    private static final String CN_RA_TARGETS = ":COARNotifyRelationshipAnnouncementTargets";
    private static final String RELATED_PUBLICATION = "publication";
    public static final String DATACITE_URI_PREFIX = "https://purl.org/datacite/ontology#";

    public COARNotifyRelationshipAnnouncementStep(Map<String, String> paramSet) {
        new HashMap<>(paramSet);
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {

        JsonArray targets = JsonUtil.getJsonArray((String) context.getSettings().get(CN_RA_TARGETS));
        if (targets != null && !targets.isEmpty()) {
            CloseableHttpClient client = HttpClients.createDefault();

            try {
                // First check that we have what is required
                Dataset d = context.getDataset();
                DatasetVersion dv = d.getReleasedVersion();
                DatasetVersion priorVersion = d.getPriorReleasedVersion();
                List<DatasetField> dvf = dv.getDatasetFields();
                Map<String, DatasetField> fields = new HashMap<String, DatasetField>();
                List<String> reqFields = Arrays
                        .asList(((String) context.getSettings().getOrDefault(REQUIRED_FIELDS, "")).split(",\\s*"));
                
                Map<String, DatasetField> priorFields = new HashMap<String, DatasetField>();
                if (priorVersion != null) {
                    for (DatasetField pdf : priorVersion.getDatasetFields()) {
                        if (!pdf.isEmpty() && reqFields.contains(pdf.getDatasetFieldType().getName())) {
                            priorFields.put(pdf.getDatasetFieldType().getName(), pdf);
                        }
                    }
                }
                
                for (DatasetField df : dvf) {
                    if (!df.isEmpty() && reqFields.contains(df.getDatasetFieldType().getName())) {
                        DatasetField priorField = priorFields.get(df.getDatasetFieldType().getName());
                        
                        if (priorVersion == null || priorField == null) {
                            // No prior version, include all values
                            fields.put(df.getDatasetFieldType().getName(), df);
                        } else {
                            // Create a filtered field with only new values
                            DatasetField filteredField = filterNewValues(df, priorField);
                            if (!filteredField.isEmpty()) {
                                fields.put(df.getDatasetFieldType().getName(), filteredField);
                            }
                        }
                    }
                }

                // Get all relationship objects once
                JsonArray relationships = getObjects(context, fields);

                if (relationships.isEmpty()) {
                    logger.fine("No valid relationships found to announce");
                    return OK;
                }

                // Track overall success
                boolean anySuccess = false;
                StringBuilder errors = new StringBuilder();

                // Loop through each target
                for (JsonObject target : targets.getValuesAs(JsonObject.class)) {
                    String inboxUrl = target.getString("inbox");

                    // Send a message for each relationship to this target
                    for (JsonObject rel : relationships.getValuesAs(JsonObject.class)) {
                        HttpPost announcement = buildAnnouncementPost(rel, target);

                        try (CloseableHttpResponse response = client.execute(announcement)) {
                            int code = response.getStatusLine().getStatusCode();
                            if (code >= 200 && code < 300) {
                                // HTTP OK range
                                anySuccess = true;
                                logger.fine("Successfully sent message for " + rel.toString() + " to " + inboxUrl);
                            } else {
                                String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                                        StandardCharsets.UTF_8);
                                String errorMsg = "Error communicating with " + inboxUrl + " for relationship "
                                        + rel.toString() + ". Server response: " + responseBody + " (" + response
                                        + ").";
                                logger.warning(errorMsg);
                                errors.append(errorMsg).append("\n");
                            }
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Error communicating with " + inboxUrl + ": " + ex.getMessage(),
                                    ex);
                            String errorMsg = "Error executing request to " + inboxUrl + ": "
                                    + ex.getLocalizedMessage();
                            errors.append(errorMsg).append("\n");
                        }
                    }
                }

                // If we had any errors but also some successes, report partial failure
                if (errors.length() > 0) {
                    return new Failure((anySuccess ? "Partial failure: " : "") + errors.toString());
                }

                // All succeeded
                return OK;

            } catch (URISyntaxException e) {
                return new Failure(
                        "COARNotifyRelationshipAnnouncementStep workflow step failed: unable to parse inbox in target setting.");
            }
        }
        return new Failure("COARNotifyRelationshipAnnouncementStep workflow step failed: " + CN_RA_TARGETS
                + " setting missing or invalid.");
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    /**
     * Scan through all fields and return an array of relationship JsonObjects with
     * subjectId, relationship, objectId, and @context
     * 
     * @param ctxt
     * @param fields
     * @return JsonArray of JsonObjects with subjectId, relationship, objectId,
     *         and @context
     */
    JsonArray getObjects(WorkflowContext ctxt, Map<String, DatasetField> fields) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        Map<String, String> localContext = new HashMap<String, String>();
        Map<Long, JsonObject> emptyCvocMap = new HashMap<Long, JsonObject>();

        Dataset d = ctxt.getDataset();
        for (Entry<String, DatasetField> entry : fields.entrySet()) {
            DatasetField field = entry.getValue();
            DatasetFieldType dft = field.getDatasetFieldType();
            JsonValue jv = OREMap.getJsonLDForField(field, false, emptyCvocMap, localContext);
            // jv is a JsonArray for multi-val fields, so loop
            if (jv != null) {
                if (jv instanceof JsonArray) {
                    JsonArray rels = (JsonArray) jv;
                    Iterator<JsonValue> iter = rels.iterator();
                    while (iter.hasNext()) {
                        JsonValue jval = iter.next();
                        JsonObject relObject = getRelationshipObject(dft, jval, d, localContext);
                        if (relObject != null) {
                            jab.add(relObject);
                        }
                    }
                } else {
                    JsonObject relObject = getRelationshipObject(dft, jv, d, localContext);
                    if (relObject != null) {
                        jab.add(relObject);
                    }
                }
            }

        }
        return jab.build();
    }

    private JsonObject getRelationshipObject(DatasetFieldType dft, JsonValue jval, Dataset d,
            Map<String, String> localContext) {
        String[] answers = getBestIdAndType(dft, jval);
        String id = answers[0];
        String type = answers[1];
        // Skip if we couldn't determine a valid ID
        if (id == null || type == null) {
            return null;
        }
        return Json.createObjectBuilder().add("as:object", id).add("as:relationship", type)
                .add("as:subject", d.getGlobalId().asURL().toString())
                .add("id", "urn:uuid:" + UUID.randomUUID().toString()).add("type", "Relationship").build();
    }

    HttpPost buildAnnouncementPost(JsonObject rel, JsonObject target) throws URISyntaxException {
        String body = buildAnnouncement(rel, target);
        logger.info("Body: " + body);

        HttpPost annPost = new HttpPost();
        annPost.setURI(new URI(target.getString("inbox")));
        annPost.setEntity(new StringEntity(JsonUtil.prettyPrint(body), "utf-8"));
        annPost.setHeader("Content-Type", "application/ld+json");
        return annPost;
    }

    public static String buildAnnouncement(JsonObject rel, JsonObject target) {
        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonArrayBuilder context = Json.createArrayBuilder().add("https://www.w3.org/ns/activitystreams")
                .add("https://coar-notify.net");
        job.add("@context", context);
        job.add("id", "urn:uuid:" + UUID.randomUUID().toString());
        job.add("actor", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("name", BrandingUtil.getInstallationBrandName()).add("type", "Service"));
        JsonObjectBuilder coarContextBuilder = Json.createObjectBuilder();
        coarContextBuilder.add("id", rel.getString("as:object"));
        job.add("context", coarContextBuilder.build());
        job.add("object", rel);
        job.add("origin", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("inbox", SystemConfig.getDataverseSiteUrlStatic() + "/api/inbox").add("type", "Service"));
        job.add("target", target);
        job.add("type", Json.createArrayBuilder().add("Announce").add("coar-notify:RelationshipAction"));

        return JsonUtil.prettyPrint(job.build());
    }

    private String[] getBestIdAndType(DatasetFieldType dft, JsonValue jv) {

        String type = DATACITE_URI_PREFIX + "IsSupplementTo";
        // Primitive value
        if (jv instanceof JsonString) {
            String value = ((JsonString) jv).getString();
            if (isURI(value)) {
                return new String[] { ((JsonString) jv).getString(), type };
            } else {
                return new String[] { null, null };
            }
        }
        // Compound - apply type specific logic to get best Id
        JsonObject jo = jv.asJsonObject();
        String id = null;
        switch (dft.getName()) {
        case RELATED_PUBLICATION:
            JsonLDTerm publicationIDType = null;
            JsonLDTerm publicationIDNumber = null;
            JsonLDTerm publicationURL = null;
            JsonLDTerm publicationRelationType = null;

            Collection<DatasetFieldType> childTypes = dft.getChildDatasetFieldTypes();
            for (DatasetFieldType cdft : childTypes) {
                switch (cdft.getName()) {
                case "publicationURL":
                    publicationURL = cdft.getJsonLDTerm();
                    break;
                case "publicationIDType":
                    publicationIDType = cdft.getJsonLDTerm();
                    break;
                case "publicationIDNumber":
                    publicationIDNumber = cdft.getJsonLDTerm();
                    break;
                case "publicationRelationType":
                    publicationRelationType = cdft.getJsonLDTerm();
                }
            }
            if (jo.containsKey(publicationIDType.getLabel())) {
                if ((jo.containsKey(publicationIDNumber.getLabel()))) {
                    String number = jo.getString(publicationIDNumber.getLabel());

                    switch (jo.getString(publicationIDType.getLabel())) {
                    case AbstractDOIProvider.DOI_PROTOCOL:
                        if (number.startsWith("10")) {
                            number = AbstractDOIProvider.DOI_PROTOCOL + number;
                        }
                        // Validate using GlobalId
                        try {
                            GlobalId pid = PidUtil.parseAsGlobalID(number);
                            id = pid.asURL();
                        } catch (IllegalArgumentException e) {
                            // Ignore
                        }
                        break;
                    case HandlePidProvider.HDL_PROTOCOL:
                        if (!number.startsWith(HandlePidProvider.HDL_PROTOCOL)
                                && !number.startsWith(HandlePidProvider.HDL_RESOLVER_URL)
                                && !number.startsWith(HandlePidProvider.HTTP_HDL_RESOLVER_URL)) {
                            number = "hdl:" + number;
                        }
                        // Validate using GlobalId
                        try {
                            GlobalId pid = PidUtil.parseAsGlobalID(number);
                            id = pid.asURL();
                        } catch (IllegalArgumentException e) {
                            // Ignore
                        }
                        break;
                    default:
                        // Check if the number can be interpreted as a valid URI of some sort
                        if (isURI(number)) {
                            id = number;
                        }

                        break;
                    }
                }
            } else if (jo.containsKey(publicationURL.getLabel())) {

                String value = jo.getString(publicationURL.getLabel());
                if (isURI(value)) {
                    id = value;
                }
            }
            if (jo.containsKey(publicationRelationType.getLabel())) {
                type = jo.getString(publicationRelationType.getLabel());
                type = DATACITE_URI_PREFIX + type;
            }
            break;
        default:
            Collection<DatasetFieldType> childDFTs = dft.getChildDatasetFieldTypes();
            // Loop through child fields and select one
            // The order of preference is for a field with URL in the name, followed by one
            // with 'ID',then 'Name', and as a last resort, a field.
            for (DatasetFieldType cdft : childDFTs) {
                String fieldname = cdft.getName();
                if (fieldname.contains("URL")) {
                    if (jo.containsKey(cdft.getJsonLDTerm().getLabel())) {
                        String value = jo.getString(cdft.getJsonLDTerm().getLabel());
                        if (isURI(value)) {
                            id = value;
                        }
                        break;
                    }
                }
            }
            if (id == null) {
                for (DatasetFieldType cdft : childDFTs) {
                    String fieldname = cdft.getName();

                    if ((fieldname.contains("ID") || fieldname.contains("Id")) && !fieldname.contains("Type")) {
                        if (jo.containsKey(cdft.getJsonLDTerm().getLabel())) {
                            String value = jo.getString(cdft.getJsonLDTerm().getLabel());
                            if (isURI(value)) {
                                id = value;
                            }
                            break;
                        }

                    }
                }
            }
            if (id == null) {
                for (DatasetFieldType cdft : childDFTs) {
                    String fieldname = cdft.getName();

                    if (fieldname.contains("Name")) {
                        if (jo.containsKey(cdft.getJsonLDTerm().getLabel())) {
                            String value = jo.getString(cdft.getJsonLDTerm().getLabel());
                            if (isURI(value)) {
                                id = value;
                            }
                            break;
                        }
                    }
                }
            }
        }
        return new String[] { id, type };
    }

    private boolean isURI(String number) {
        try {
            URI uri = new URI(number);
            if (uri.isAbsolute()) {
                return true;
            }
        } catch (URISyntaxException e) {
            // Not a valid URI, skip
            logger.fine("Value is not a valid URI: " + number);
        }
        return false;
    }

    /**
     * Create a new DatasetField containing only values that are new compared to the prior field.
     * This creates a detached copy to avoid modifying the managed entity.
     * 
     * @param currentField The field from the current version
     * @param priorField The field from the prior version
     * @return A new DatasetField with only new values
     */
    private DatasetField filterNewValues(DatasetField currentField, DatasetField priorField) {
        DatasetField filteredField = new DatasetField();
        filteredField.setDatasetFieldType(currentField.getDatasetFieldType());

        if (currentField.getDatasetFieldType().isCompound()) {
            // Handle compound values
            List<DatasetFieldCompoundValue> newCompoundValues = new ArrayList<>();

            for (DatasetFieldCompoundValue currentCompoundValue : currentField.getDatasetFieldCompoundValues()) {
                boolean isNew = true;

                // Check if this compound value exists in prior field
                if (priorField != null && priorField.getDatasetFieldCompoundValues() != null) {
                    for (DatasetFieldCompoundValue priorCompoundValue : priorField.getDatasetFieldCompoundValues()) {
                        if (compoundValuesEqual(currentCompoundValue, priorCompoundValue)) {
                            isNew = false;
                            break;
                        }
                    }
                }

                if (isNew) {
                    // Create a copy of the compound value
                    DatasetFieldCompoundValue newCompoundValue = copyCompoundValue(currentCompoundValue, filteredField);
                    newCompoundValues.add(newCompoundValue);
                }
            }

            filteredField.setDatasetFieldCompoundValues(newCompoundValues);

        } else if (currentField.getDatasetFieldType().isAllowMultiples()) {
            // Handle multiple simple values
            List<DatasetFieldValue> newValues = new ArrayList<>();

            for (DatasetFieldValue currentValue : currentField.getDatasetFieldValues()) {
                boolean isNew = true;

                if (priorField != null && priorField.getDatasetFieldValues() != null) {
                    for (DatasetFieldValue priorValue : priorField.getDatasetFieldValues()) {
                        if (valuesEqual(currentValue, priorValue)) {
                            isNew = false;
                            break;
                        }
                    }
                }

                if (isNew) {
                    DatasetFieldValue newValue = new DatasetFieldValue();
                    newValue.setValue(currentValue.getValue());
                    newValue.setDatasetField(filteredField);
                    newValues.add(newValue);
                }
            }

            filteredField.setDatasetFieldValues(newValues);

        } else {
            // Handle single value
            if (priorField == null || !valuesEqual(currentField.getSingleValue(), priorField.getSingleValue())) {
                filteredField.setSingleValue(currentField.getValue());
            }
        }

        return filteredField;
    }

    /**
     * Check if two compound values are equal by comparing all their child fields.
     * Since child fields are ordered, we can do a simpler comparison.
     */
    private boolean compoundValuesEqual(DatasetFieldCompoundValue cv1, DatasetFieldCompoundValue cv2) {
        if (cv1 == null && cv2 == null) {
            return true;
        }
        if (cv1 == null || cv2 == null) {
            return false;
        }

        List<DatasetField> children1 = cv1.getChildDatasetFields();
        List<DatasetField> children2 = cv2.getChildDatasetFields();

        if (children1.size() != children2.size()) {
            return false;
        }

        // Since fields are ordered, we can compare them directly by position
        for (int i = 0; i < children1.size(); i++) {
            DatasetField child1 = children1.get(i);
            DatasetField child2 = children2.get(i);
            
            // Compare field types
            if (!child1.getDatasetFieldType().equals(child2.getDatasetFieldType())) {
                return false;
            }
            
            // Compare values using Apache Commons StringUtils
            if (!Strings.CS.equals(child1.getValue(), child2.getValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create a deep copy of a compound value
     */
    private DatasetFieldCompoundValue copyCompoundValue(DatasetFieldCompoundValue source, DatasetField parentField) {
        DatasetFieldCompoundValue copy = new DatasetFieldCompoundValue();
        copy.setParentDatasetField(parentField);
        copy.setDisplayOrder(source.getDisplayOrder());

        List<DatasetField> childFieldsCopy = new ArrayList<>();
        for (DatasetField sourceChild : source.getChildDatasetFields()) {
            DatasetField childCopy = new DatasetField();
            childCopy.setDatasetFieldType(sourceChild.getDatasetFieldType());
            childCopy.setParentDatasetFieldCompoundValue(copy);
            childCopy.setSingleValue(sourceChild.getValue());
            childFieldsCopy.add(childCopy);
        }

        copy.setChildDatasetFields(childFieldsCopy);
        return copy;
    }

    private boolean valuesEqual(DatasetFieldValue v1, DatasetFieldValue v2) {
        if (v1 == null && v2 == null) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }
        return Strings.CS.equals(v1.getValue(), v2.getValue());
    }
}
