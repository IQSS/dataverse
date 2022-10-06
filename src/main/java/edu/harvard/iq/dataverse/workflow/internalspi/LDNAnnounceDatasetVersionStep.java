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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

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

    JsonLDTerm publicationIDType = null;
    JsonLDTerm publicationIDNumber = null;
    JsonLDTerm publicationURL = null;

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
                announcement = buildAnnouncement(false, context, target);
            } catch (URISyntaxException e) {
                return new Failure("LDNAnnounceDatasetVersion workflow step failed: unable to parse inbox in :LDNTarget setting.");
            }
            if(announcement==null) {
                logger.info(context.getDataset().getGlobalId().asString() + "does not have metadata required to send LDN message. Nothing sent.");
                return OK;
            }
            // execute
            try (CloseableHttpResponse response = client.execute(announcement)) {
                int code = response.getStatusLine().getStatusCode();
                if (code >= 200 && code < 300) {
                    // HTTP OK range
                    return OK;
                } else {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                            StandardCharsets.UTF_8);
                    ;
                    return new Failure("Error communicating with " + inboxUrl + ". Server response: " + responseBody
                            + " (" + response + ").");
                }

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error communicating with remote server: " + ex.getMessage(), ex);
                return new Failure("Error executing request: " + ex.getLocalizedMessage(),
                        "Cannot communicate with remote server.");
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

    HttpPost buildAnnouncement(boolean qb, WorkflowContext ctxt, JsonObject target) throws URISyntaxException {

        // First check that we have what is required
        DatasetVersion dv = ctxt.getDataset().getReleasedVersion();
        List<DatasetField> dvf = dv.getDatasetFields();
        Map<String, DatasetField> fields = new HashMap<String, DatasetField>();
        String[] requiredFields = ((String) ctxt.getSettings().getOrDefault(REQUIRED_FIELDS, "")).split(",\\s*");
        for (String field : requiredFields) {
            fields.put(field, null);
        }
        Set<String> reqFields = fields.keySet();
        for (DatasetField df : dvf) {
            if(!df.isEmpty() && reqFields.contains(df.getDatasetFieldType().getName())) {
                fields.put(df.getDatasetFieldType().getName(), df);
            }
        }
        if (fields.containsValue(null)) {
            logger.fine("DatasetVersion doesn't contain metadata required to trigger announcement");
            return null;
        }
        // We do, so construct the json-ld body and method

        Map<String, String> localContext = new HashMap<String, String>();
        JsonObjectBuilder coarContext = Json.createObjectBuilder();
        Map<Long, JsonObject> emptyCvocMap = new HashMap<Long, JsonObject>();
        boolean includeLocalContext = false;
        for (Entry<String, DatasetField> entry : fields.entrySet()) {
            DatasetField field = entry.getValue();
            DatasetFieldType dft = field.getDatasetFieldType();
            String dfTypeName = entry.getKey();
            JsonValue jv = OREMap.getJsonLDForField(field, false, emptyCvocMap, localContext);
            switch (dfTypeName) {
            case RELATED_PUBLICATION:
                JsonArrayBuilder relArrayBuilder = Json.createArrayBuilder();
                publicationIDType = null;
                publicationIDNumber = null;
                publicationURL = null;
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

                if (jv != null) {
                    if (jv instanceof JsonArray) {
                        JsonArray rels = (JsonArray) jv;
                        for (JsonObject jo : rels.getValuesAs(JsonObject.class)) {
                            String id = getBestPubId(jo);
                            relArrayBuilder.add(Json.createObjectBuilder().add("id", id).add("ietf:cite-as", id)
                                    .add("type", "sorg:ScholaryArticle").build());
                        }
                    }

                    else { // JsonObject
                        String id = getBestPubId((JsonObject) jv);
                        relArrayBuilder.add(Json.createObjectBuilder().add("id", id).add("ietf:cite-as", id)
                                .add("type", "sorg:ScholaryArticle").build());
                    }
                }
                coarContext.add("IsSupplementTo", relArrayBuilder);
                break;
            default:
                if (jv != null) {
                    includeLocalContext = true;
                    coarContext.add(dft.getJsonLDTerm().getLabel(), jv);
                }

            }
        }
        dvf.get(0).getDatasetFieldType().getName();
        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonArrayBuilder context = Json.createArrayBuilder().add("https://purl.org/coar/notify")
                .add("https://www.w3.org/ns/activitystreams");
        if (includeLocalContext && !localContext.isEmpty()) {
            JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
            for (Entry<String, String> e : localContext.entrySet()) {
                contextBuilder.add(e.getKey(), e.getValue());
            }
            context.add(contextBuilder);
        }
        job.add("@context", context);
        job.add("id", "urn:uuid:" + UUID.randomUUID().toString());
        job.add("actor", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("name", BrandingUtil.getInstallationBrandName()).add("type", "Service"));
        job.add("context", coarContext);
        Dataset d = ctxt.getDataset();
        job.add("object",
                Json.createObjectBuilder().add("id", d.getLocalURL())
                        .add("ietf:cite-as", d.getGlobalId().asURL())
                        .add("sorg:name", d.getDisplayName()).add("type", "sorg:Dataset"));
        job.add("origin", Json.createObjectBuilder().add("id", SystemConfig.getDataverseSiteUrlStatic())
                .add("inbox", SystemConfig.getDataverseSiteUrlStatic() + "/api/inbox").add("type", "Service"));
        job.add("target", target);
        job.add("type", Json.createArrayBuilder().add("Announce").add("coar-notify:ReleaseAction"));

        HttpPost annPost = new HttpPost();
        annPost.setURI(new URI(target.getString("inbox")));
        String body = JsonUtil.prettyPrint(job.build());
        logger.fine("Body: " + body);
        annPost.setEntity(new StringEntity(JsonUtil.prettyPrint(body), "utf-8"));
        annPost.setHeader("Content-Type", "application/ld+json");
        return annPost;
    }

    private String getBestPubId(JsonObject jo) {
        String id = null;
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
