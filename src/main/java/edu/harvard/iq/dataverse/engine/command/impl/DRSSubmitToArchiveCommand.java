package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@RequiredPermissions(Permission.PublishDataset)
public class DRSSubmitToArchiveCommand extends S3SubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DRSSubmitToArchiveCommand.class.getName());
    private static final String DRS_CONFIG = ":DRSArchivalConfig";
    private static String PENDING = "Pending";

    public DRSSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {
        logger.fine("In DRSSubmitToArchiveCommand...");
        JsonObject drsConfigObject = null;

        try {
            drsConfigObject = JsonUtil.getJsonObject(requestedSettings.get(DRS_CONFIG));
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            Set<String> collections = drsConfigObject.getJsonObject("collections").keySet();
            Dataset dataset = dv.getDataset();
            Dataverse ancestor = dataset.getOwner();
            String alias = getArchivableAncestor(ancestor, collections);

            if (alias != null) {
                JsonObject collectionConfig = drsConfigObject.getJsonObject("collections").getJsonObject(alias);

                WorkflowStepResult s3Result = super.performArchiveSubmission(dv, token, requestedSettings);

                JsonObjectBuilder statusObject = Json.createObjectBuilder();
                statusObject.add("status", "Failure");
                statusObject.add("message", "Bag not transferred");

                if (s3Result == WorkflowStepResult.OK) {
                    statusObject.add("status", "Attempted");
                    statusObject.add("message", "Bag transferred");

                    // Now contact DRS
                    JsonObjectBuilder job = Json.createObjectBuilder(drsConfigObject);
                    job.remove("collections");
                    job.remove("DRSendpoint");
                    String spaceName = getSpaceName(dataset);
                    job.add("package_id", spaceName + ".v" + dv.getFriendlyVersionNumber());

                    job.add("s3_path", spaceName);
                    for (Entry<String, JsonValue> entry : collectionConfig.entrySet()) {
                        job.add(entry.getKey(), entry.getValue());
                    }

                    String drsConfigString = JsonUtil.prettyPrint(job.build());

                    CloseableHttpClient client = HttpClients.createDefault();
                    HttpPost ingestPost;
                    try {
                        ingestPost = new HttpPost();
                        ingestPost.setURI(new URI(drsConfigObject.getString("DRSendpoint")));
                        String body = drsConfigString;
                        logger.fine("Body: " + body);
                        ingestPost.setEntity(new StringEntity(body, "utf-8"));
                        ingestPost.setHeader("Content-Type", "application/json");

                    } catch (URISyntaxException e) {
                        return new Failure(
                                "LDNAnnounceDatasetVersion workflow step failed: unable to parse inbox in :LDNTarget setting.");
                    }
                    // execute

                    try (CloseableHttpResponse response = client.execute(ingestPost)) {
                        int code = response.getStatusLine().getStatusCode();
                        String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                                StandardCharsets.UTF_8);
                        if (code >= 200 && code < 300) {
                            logger.fine("Status: " + code);
                            logger.fine("Response" + responseBody);
                            JsonObject responseObject = JsonUtil.getJsonObject(responseBody);
                            String status = responseObject.getString("status");
                            if (!PENDING.equals(status)) {
                                logger.warning("Unexpected Status: " + status);
                            } else {
                                logger.fine("DRS Ingest succeded: " + responseObject.toString());
                                statusObject.add("status", status);
                                statusObject.add("message", responseObject.getString("message"));
                            }
                        }
                    } catch (ClientProtocolException e2) {
                        e2.printStackTrace();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }

                } else {

                    logger.warning("DRS: S3 archiving failed - will not call ingest: " + getSpaceName(dataset) + "_v"
                            + dv.getFriendlyVersionNumber());
                    return new Failure("DRS Archiver fail in initial S3 Archiver transfer");
                }
                dv.setArchivalCopyLocation(statusObject.build().toString());
            } else {
                logger.fine("DRS Archiver: No matching collection found - will not archive: " + getSpaceName(dataset)
                        + "_v" + dv.getFriendlyVersionNumber());
                return WorkflowStepResult.OK;
            }
        } else {
            logger.warning(DRS_CONFIG + " not found");
            return new Failure("DRS Submission not configured - no " + DRS_CONFIG + " found.");
        }
        return WorkflowStepResult.OK;
    }

    private static String getArchivableAncestor(Dataverse ancestor, Set<String> collections) {
        String alias = ancestor.getAlias();
        while (ancestor != null && !collections.contains(alias)) {
            ancestor = ancestor.getOwner();
            if (ancestor != null) {
                alias = ancestor.getAlias();
            } else {
                alias = null;
            }
        }
        return alias;
    }

    public static boolean isArchivable(Dataset d, SettingsWrapper sw) {
        JsonObject drsConfigObject = null;

        try {
            String config = sw.get(DRS_CONFIG, null);
            if (config != null) {
                drsConfigObject = JsonUtil.getJsonObject(config);
            }
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            Set<String> collections = drsConfigObject.getJsonObject("collections").keySet();
            return getArchivableAncestor(d.getOwner(), collections) != null;
        }
        return false;
    }
}
