package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * A workflow step that generates and sends an LDN Announcement message to the
 * inbox of a configured target. THe initial use case is for Dataverse to
 * anounce new dataset versions to the Harvard DASH preprint repository so that
 * a DASH admin can create a backlink for any dataset versions that reference a
 * DASH deposit or a paper with a DOI where DASH has a preprint copy.
 * 
 * @author qqmyers
 */

public class LDNAnnounceDatasetVersionStep implements WorkflowStep {
    private static final Logger logger = Logger.getLogger(LDNAnnounceDatasetVersionStep.class.getName());
    private static final String REQUIRED_FIELDS = ":LDNAnnounceRequiredFields";
    private static final String LDN_TARGET = ":LDNTarget";
    private static final String RELATED_PUBLICATION = "publication";

    public LDNAnnounceDatasetVersionStep(Map<String, String> paramSet) {
        new HashMap<>(paramSet);
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {

        JsonObject target = JsonUtil.getJsonObject((String) context.getSettings().get(LDN_TARGET));
        if (target != null) {
            String inboxUrl = target.getString("inbox");

            CloseableHttpClient client = HttpClients.createDefault();

            // build method

            HttpPost announcement;
            try {
                // First check that we have what is required
                Dataset d = context.getDataset();
                DatasetVersion dv = d.getReleasedVersion();
                List<DatasetField> dvf = dv.getDatasetFields();
                Map<String, DatasetField> fields = new HashMap<String, DatasetField>();
                List<String> reqFields = Arrays
                        .asList(((String) context.getSettings().getOrDefault(REQUIRED_FIELDS, "")).split(",\\s*"));
                for (DatasetField df : dvf) {
                    if (!df.isEmpty() && reqFields.contains(df.getDatasetFieldType().getName())) {
                        fields.put(df.getDatasetFieldType().getName(), df);
                    }
                }
                // Loop through and send a message for each supported relationship
                boolean success = false;
                for (JsonObject rel : getObjects(context, fields).getValuesAs(JsonObject.class)) {
                    announcement = buildAnnouncement(d, rel, target);
                    // execute
                    try (CloseableHttpResponse response = client.execute(announcement)) {
                        int code = response.getStatusLine().getStatusCode();
                        if (code >= 200 && code < 300) {
                            // HTTP OK range
                            success = true;
                            logger.fine("Successfully sent message for " + rel.toString());
                        } else {
                            String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                                    StandardCharsets.UTF_8);
                            ;
                            return new Failure((success ? "Partial failure" : "") + "Error communicating with "
                                    + inboxUrl + " for relationship " + rel.toString() + ". Server response: "
                                    + responseBody + " (" + response + ").");
                        }

                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error communicating with remote server: " + ex.getMessage(), ex);
                        return new Failure((success ? "Partial failure" : "") + "Error executing request: "
                                + ex.getLocalizedMessage(), "Cannot communicate with remote server.");
                    }

                }
                // Any failure and we would have returned already.
                return OK;

            } catch (URISyntaxException e) {
                return new Failure(
                        "LDNAnnounceDatasetVersion workflow step failed: unable to parse inbox in :LDNTarget setting.");
            }
        }
        return new Failure("LDNAnnounceDatasetVersion workflow step failed: :LDNTarget setting missing or invalid.");
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
                        jab.add(getRelationshipObject(dft, jval, d, localContext));
                    }
                } else {
                    jab.add(getRelationshipObject(dft, jv, d, localContext));
                }
            }

        }
        return jab.build();
    }

    private JsonObject getRelationshipObject(DatasetFieldType dft, JsonValue jval, Dataset d,
            Map<String, String> localContext) {
        String id = getBestId(dft, jval);
        return Json.createObjectBuilder().add("object", id).add("relationship", dft.getJsonLDTerm().getUrl())
                .add("subject", d.getGlobalId().asURL().toString()).add("id", "urn:uuid:" + UUID.randomUUID().toString()).add("type","Relationship").build();
    }

    HttpPost buildAnnouncement(Dataset d, JsonObject rel, JsonObject target) throws URISyntaxException {

        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonArrayBuilder context = Json.createArrayBuilder().add("https://purl.org/coar/notify")
                .add("https://www.w3.org/ns/activitystreams");
        job.add("@context", context);
        job.add("id", "urn:uuid:" + UUID.randomUUID().toString());
        job.add("actor", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("name", BrandingUtil.getInstallationBrandName()).add("type", "Service"));
        job.add("object", rel);
        job.add("origin", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("inbox", SystemConfig.getDataverseSiteUrlStatic() + "/api/inbox").add("type", "Service"));
        job.add("target", target);
        job.add("type", Json.createArrayBuilder().add("Announce").add("coar-notify:RelationshipAction"));

        HttpPost annPost = new HttpPost();
        annPost.setURI(new URI(target.getString("inbox")));
        String body = JsonUtil.prettyPrint(job.build());
        logger.info("Body: " + body);
        annPost.setEntity(new StringEntity(JsonUtil.prettyPrint(body), "utf-8"));
        annPost.setHeader("Content-Type", "application/ld+json");
        return annPost;
    }

    private String getBestId(DatasetFieldType dft, JsonValue jv) {
        // Primitive value
        if (jv instanceof JsonString) {
            return ((JsonString) jv).getString();
        }
        // Compound - apply type specific logic to get best Id
        JsonObject jo = jv.asJsonObject();
        String id = null;
        switch (dft.getName()) {
        case RELATED_PUBLICATION:
            JsonLDTerm publicationIDType = null;
            JsonLDTerm publicationIDNumber = null;
            JsonLDTerm publicationURL = null;

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
                }
            }
            if (jo.containsKey(publicationURL.getLabel())) {
                id = jo.getString(publicationURL.getLabel());
            } else if (jo.containsKey(publicationIDType.getLabel())) {
                if ((jo.containsKey(publicationIDNumber.getLabel()))) {
                    String number = jo.getString(publicationIDNumber.getLabel());

                    switch (jo.getString(publicationIDType.getLabel())) {
                    case "doi":
                        if (number.startsWith("https://doi.org/")) {
                            id = number;
                        } else if (number.startsWith("doi:")) {
                            id = "https://doi.org/" + number.substring(4);
                        } else {
                            // Assume a raw DOI, e.g. 10.5072/FK2ABCDEF
                            id = "https://doi.org/" + number;
                        }
                        break;
                    case "DASH-URN":
                        if (number.startsWith("http")) {
                            id = number;
                        }
                        break;
                    }
                }
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
                        id = jo.getString(cdft.getJsonLDTerm().getLabel());
                        break;
                    }
                }
            }
            if (id == null) {
                for (DatasetFieldType cdft : childDFTs) {
                    String fieldname = cdft.getName();

                    if (fieldname.contains("ID") || fieldname.contains("Id")) {
                        if (jo.containsKey(cdft.getJsonLDTerm().getLabel())) {
                            id = jo.getString(cdft.getJsonLDTerm().getLabel());
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
                            id = jo.getString(cdft.getJsonLDTerm().getLabel());
                            break;
                        }
                    }
                }
            }
            id = jo.getString(jo.keySet().iterator().next());
        }
        return id;
    }

    String process(String template, Map<String, String> values) {
        String curValue = template;
        for (Map.Entry<String, String> ent : values.entrySet()) {
            String val = ent.getValue();
            if (val == null) {
                val = "";
            }
            String varRef = "${" + ent.getKey() + "}";
            while (curValue.contains(varRef)) {
                curValue = curValue.replace(varRef, val);
            }
        }

        return curValue;
    }

}
