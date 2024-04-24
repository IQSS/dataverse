package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import org.erdtman.jcs.JsonCanonicalizer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

@RequiredPermissions(Permission.PublishDataset)
public class DRSSubmitToArchiveCommand extends S3SubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DRSSubmitToArchiveCommand.class.getName());
    private static final String DRS_CONFIG = ":DRSArchiverConfig";
    private static final String ADMIN_METADATA = "admin_metadata";
    private static final String S3_BUCKET_NAME = "s3_bucket_name";
    private static final String S3_PATH = "s3_path";
    private static final String COLLECTIONS = "collections";
    private static final String PACKAGE_ID = "package_id";
    private static final String SINGLE_VERSION = "single_version";
    private static final String DRS_ENDPOINT = "DRS_endpoint";
    

    private static final String RSA_KEY = "dataverse.archiver.drs.rsa_key";

    private static final String TRUST_CERT = "trust_cert";
    private static final String TIMEOUT = "timeout";

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
            JsonObject adminMetadata = drsConfigObject.getJsonObject(ADMIN_METADATA);
            Set<String> collections = adminMetadata.getJsonObject(COLLECTIONS).keySet();
            Dataset dataset = dv.getDataset();
            Dataverse ancestor = dataset.getOwner();
            String alias = getArchivableAncestor(ancestor, collections);
            String spaceName = getSpaceName(dataset);
            String packageId = getFileName(spaceName, dv);

            if (alias != null) {
                if (drsConfigObject.getBoolean(SINGLE_VERSION, false)) {
                    for (DatasetVersion version : dataset.getVersions()) {
                        if (version.getArchivalCopyLocation() != null) {
                            return new Failure("DRS Archiver fail: version " + version.getFriendlyVersionNumber()
                                    + " already archived.");
                        }
                    }
                }

                JsonObject collectionConfig = adminMetadata.getJsonObject(COLLECTIONS).getJsonObject(alias);

                WorkflowStepResult s3Result = super.performArchiveSubmission(dv, token, requestedSettings);

                JsonObjectBuilder statusObject = Json.createObjectBuilder();
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");

                if (s3Result == WorkflowStepResult.OK) {
                    //This will be overwritten if the further steps are successful
                    statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
                    statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag transferred, DRS ingest call failed");

                    // Now contact DRS
                    boolean trustCert = drsConfigObject.getBoolean(TRUST_CERT, false);
                    int jwtTimeout = drsConfigObject.getInt(TIMEOUT, 5);
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
                        ingestPost.setURI(new URI(drsConfigObject.getString(DRS_ENDPOINT)));

                        byte[] encoded = Base64.getDecoder().decode(System.getProperty(RSA_KEY).replaceAll("[\\r\\n]", ""));

                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
                        RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
                        //RSAPublicKey publicKey;
                        /*
                         * If public key is needed: encoded = Base64.decodeBase64(publicKeyPEM);
                         * 
                         * KeyFactory keyFactory = KeyFactory.getInstance("RS256"); X509EncodedKeySpec
                         * keySpec = new X509EncodedKeySpec(encoded); return (RSAPublicKey)
                         * keyFactory.generatePublic(keySpec); RSAPublicKey publicKey = new
                         * RSAPublicKey(System.getProperty(RS256_KEY));
                         */
                        Algorithm algorithmRSA = Algorithm.RSA256(null, privKey);
                        
                        String body = drsConfigString;
                        String jwtString = createJWTString(algorithmRSA, BrandingUtil.getInstallationBrandName(), body, jwtTimeout);
                        logger.fine("JWT: " + jwtString);

                        ingestPost.setHeader("Authorization", "Bearer " + jwtString);

                        logger.fine("Body: " + body);
                        ingestPost.setEntity(new StringEntity(body, "utf-8"));
                        ingestPost.setHeader("Content-Type", "application/json");

                        try (CloseableHttpResponse response = client.execute(ingestPost)) {
                            int code = response.getStatusLine().getStatusCode();
                            String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                                    StandardCharsets.UTF_8);
                            if (code == 202) {
                                logger.fine("Status: " + code);
                                logger.fine("Response" + responseBody);
                                JsonObject responseObject = JsonUtil.getJsonObject(responseBody);
                                if (responseObject.containsKey(DatasetVersion.ARCHIVAL_STATUS)
                                        && responseObject.containsKey(DatasetVersion.ARCHIVAL_STATUS_MESSAGE)) {
                                    String status = responseObject.getString(DatasetVersion.ARCHIVAL_STATUS);
                                    if (status.equals(DatasetVersion.ARCHIVAL_STATUS_PENDING) || status.equals(DatasetVersion.ARCHIVAL_STATUS_FAILURE)
                                            || status.equals(DatasetVersion.ARCHIVAL_STATUS_SUCCESS)) {
                                        statusObject.addAll(Json.createObjectBuilder(responseObject));
                                        switch (status) {
                                        case DatasetVersion.ARCHIVAL_STATUS_PENDING:
                                            logger.info("DRS Ingest successfully started for: " + packageId + " : "
                                                    + responseObject.toString());
                                            break;
                                        case DatasetVersion.ARCHIVAL_STATUS_FAILURE:
                                            logger.severe("DRS Ingest Failed for: " + packageId + " : "
                                                    + responseObject.toString());
                                            return new Failure("DRS Archiver fail in Ingest call");
                                        case DatasetVersion.ARCHIVAL_STATUS_SUCCESS:
                                            // We don't expect this from DRS
                                            logger.warning("Unexpected Status: " + status);
                                        }
                                    } else {
                                        logger.severe("DRS Ingest Failed for: " + packageId + " with returned status: "
                                                + status);
                                        return new Failure(
                                                "DRS Archiver fail in Ingest call with returned status: " + status);
                                    }
                                } else {
                                    logger.severe("DRS Ingest Failed for: " + packageId
                                            + " - response does not include status and message");
                                    return new Failure(
                                            "DRS Archiver fail in Ingest call \" - response does not include status and message");
                                }
                            } else {
                                logger.severe("DRS Ingest Failed for: " + packageId + " with status code: " + code);
                                logger.fine("Response" + responseBody);
                                return new Failure("DRS Archiver fail in Ingest call with status code: " + code);
                            }
                        } catch (ClientProtocolException e2) {
                            e2.printStackTrace();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    } catch (URISyntaxException e) {
                        return new Failure(
                                "DRS Archiver workflow step failed: unable to parse " + DRS_ENDPOINT );
                    } catch (JWTCreationException exception) {
                        // Invalid Signing configuration / Couldn't convert Claims.
                        return new Failure(
                                "DRS Archiver JWT Creation failure: " + exception.getMessage() );

                    }
                    // execute
                    catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        //Set status after success or failure
                        dv.setArchivalCopyLocation(statusObject.build().toString());
                    }
                } else {
                    logger.warning("DRS: S3 archiving failed - will not call ingest: " + packageId);
                    dv.setArchivalCopyLocation(statusObject.build().toString());
                    return new Failure("DRS Archiver fail in initial S3 Archiver transfer");
                }
                
            } else {
                logger.fine("DRS Archiver: No matching collection found - will not archive: " + packageId);
                return WorkflowStepResult.OK;
            }
        } else {
            logger.warning(DRS_CONFIG + " not found");
            return new Failure("DRS Submission not configured - no " + DRS_CONFIG + " found.");
        }
        return WorkflowStepResult.OK;
    }

    @Override
    protected String getFileName(String spaceName, DatasetVersion dv) {
        return spaceName + (".v" + dv.getFriendlyVersionNumber()).replace('.', '_');
    }

    @Override
    protected String getDataCiteFileName(String spaceName, DatasetVersion dv) {
        return spaceName + ("_datacite.v" + dv.getFriendlyVersionNumber()).replace('.','_');
    }

    
    public static String createJWTString(Algorithm algorithmRSA, String installationBrandName, String body, int expirationInMinutes) throws IOException {
        String canonicalBody = new JsonCanonicalizer(body).getEncodedString();
        logger.fine("Canonical body: " + canonicalBody);
        String digest = DigestUtils.sha256Hex(canonicalBody);
        if(installationBrandName==null) {
            installationBrandName = BrandingUtil.getInstallationBrandName();
        }
        return JWT.create().withIssuer(installationBrandName).withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60 * expirationInMinutes)))
                .withKeyId("defaultDataverse").withClaim("bodySHA256Hash", digest).sign(algorithmRSA);
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

    //Overrides inherited method to also check whether the dataset is in a collection for which the DRS Archiver is configured
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
    
    // DRS Archiver supports single-version semantics if the SINGLE_VERSION key in
    // the DRS_CONFIG is true
    // These methods make that choices visible on the page (cached via
    // SettingsWrapper) or in the API (using SettingServiceBean), both using the
    // same underlying logic
    
    public static boolean isSingleVersion(SettingsWrapper sw) {
            String config = sw.get(DRS_CONFIG, null);
            return isSingleVersion(config);
    }

    public static boolean isSingleVersion(SettingsServiceBean ss) {
        String config = ss.get(DRS_CONFIG, null);
        return isSingleVersion(config);
    }

    private static boolean isSingleVersion(String config) {
        JsonObject drsConfigObject = null;
        try {
            if (config != null) {
                drsConfigObject = JsonUtil.getJsonObject(config);
            }
        } catch (Exception e) {
            logger.warning("Unable to parse " + DRS_CONFIG + " setting as a Json object");
        }
        if (drsConfigObject != null) {
            return drsConfigObject.getBoolean(SINGLE_VERSION, false);
        }
        return false;
    }
}
