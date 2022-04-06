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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.net.ssl.SSLContext;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

@RequiredPermissions(Permission.PublishDataset)
public class DRSSubmitToArchiveCommand extends S3SubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DRSSubmitToArchiveCommand.class.getName());
    private static final String DRS_CONFIG = ":DRSArchivalConfig";
    private static final String FAILURE = "failure";
    private static final String PENDING = "pending";
    private static final String ADMIN_METADATA = "admin_metadata";
    private static final String S3_BUCKET_NAME = "s3_bucket_name";
    private static final String S3_PATH = "s3_path";
    private static final String COLLECTIONS = "collections";
    private static final String PACKAGE_ID = "package_id";
    private static final String TRUST_CERT = "trust_cert";

    public DRSSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {
        logger.info("In DRSSubmitToArchiveCommand...");
        JsonObject drsConfigObject = null;

        try {
            drsConfigObject = JsonUtil.getJsonObject(requestedSettings.get(DRS_CONFIG));
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            JsonObject adminMetadata = drsConfigObject.getJsonObject(ADMIN_METADATA);
            Set<String> collections = adminMetadata.getJsonObject(COLLECTIONS).keySet();
            Dataset dataset = dv.getDataset();
            Dataverse ancestor = dataset.getOwner();
            String alias = getArchivableAncestor(ancestor, collections);
            String spaceName = getSpaceName(dataset);
            String packageId = spaceName + ".v" + dv.getFriendlyVersionNumber();

            if (alias != null) {
                JsonObject collectionConfig = adminMetadata.getJsonObject(COLLECTIONS).getJsonObject(alias);

                WorkflowStepResult s3Result = super.performArchiveSubmission(dv, token, requestedSettings);

                JsonObjectBuilder statusObject = Json.createObjectBuilder();
                statusObject.add("status", "Failure");
                statusObject.add("message", "Bag not transferred");

                if (s3Result == WorkflowStepResult.OK) {
                    statusObject.add("status", "Attempted");
                    statusObject.add("message", "Bag transferred");

                    // Now contact DRS
                    boolean trustCert = drsConfigObject.getBoolean(TRUST_CERT, false);

                    JsonObjectBuilder job = Json.createObjectBuilder();

                    job.add(S3_BUCKET_NAME, adminMetadata.getString(S3_BUCKET_NAME));

                    job.add(PACKAGE_ID, packageId);
                    job.add(S3_PATH, spaceName);

                    // We start with the default admin_metadata
                    JsonObjectBuilder amob = Json.createObjectBuilder(adminMetadata);
                    // Remove collections and then override any params for the given alias
                    amob.remove(COLLECTIONS);
                    // Allow override of bucket name
                    if (collectionConfig.containsKey(S3_BUCKET_NAME)) {
                        job.add(S3_BUCKET_NAME, collectionConfig.get(S3_BUCKET_NAME));
                    }

                    for (Entry<String, JsonValue> entry : collectionConfig.entrySet()) {
                        if (!entry.getKey().equals(S3_BUCKET_NAME)) {
                            amob.add(entry.getKey(), entry.getValue());
                        }
                    }
                    job.add(ADMIN_METADATA, amob);

                    String drsConfigString = JsonUtil.prettyPrint(job.build());

                    // TODO - ADD code to ignore self-signed cert
                    CloseableHttpClient client = null;
                    if (trustCert) {
                        // use the TrustSelfSignedStrategy to allow Self Signed Certificates
                        try {
                            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy())
                                    .build();
                            client = HttpClients.custom().setSSLContext(sslContext)
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
                        } catch (KeyManagementException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (KeyStoreException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (client == null) {
                        client = HttpClients.createDefault();
                    }
                    HttpPost ingestPost;
                    try {
                        ingestPost = new HttpPost();
                        ingestPost.setURI(new URI(drsConfigObject.getString("DRSendpoint")));
                        String body = drsConfigString;
                        logger.info("Body: " + body);
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
                        if (code == 202) {
                            logger.info("Status: " + code);
                            logger.info("Response" + responseBody);
                            JsonObject responseObject = JsonUtil.getJsonObject(responseBody);
                            String status = responseObject.getString("status");
                            switch (status) {
                            case PENDING:
                                logger.info("DRS Ingest successfully started for: " + packageId + " : "
                                        + responseObject.toString());
                                statusObject.add("status", status);
                                statusObject.add("message", responseObject.getString("message"));
                                break;
                            case FAILURE:
                                logger.severe(
                                        "DRS Ingest Failed for: " + packageId + " : " + responseObject.toString());
                                return new Failure("DRS Archiver fail in Ingest call");
                            default:
                                logger.warning("Unexpected Status: " + status);
                            }
                        } else {
                            logger.severe("DRS Ingest Failed for: " + packageId + " with status code: " + code);
                            return new Failure("DRS Archiver fail in Ingest call with status cvode: " + code);
                        }
                    } catch (ClientProtocolException e2) {
                        e2.printStackTrace();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }

                } else {

                    logger.warning("DRS: S3 archiving failed - will not call ingest: " + packageId);
                    return new Failure("DRS Archiver fail in initial S3 Archiver transfer");
                }
                dv.setArchivalCopyLocation(statusObject.build().toString());
            } else {
                logger.info("DRS Archiver: No matching collection found - will not archive: " + packageId);
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
            JsonObject adminMetadata = drsConfigObject.getJsonObject(ADMIN_METADATA);
            if (adminMetadata != null) {
                JsonObject collectionObj = adminMetadata.getJsonObject(COLLECTIONS);
                if (collectionObj != null) {
                    Set<String> collections = collectionObj.keySet();
                    return getArchivableAncestor(d.getOwner(), collections) != null;
                }
            }
        }
        return false;
    }
}
