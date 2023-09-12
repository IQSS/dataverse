package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.globus.AccessToken;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.json.JsonObject;
import javax.net.ssl.SSLContext;

/**
 * @author qqmyers
 */
/*
 * Globus Overlay Driver
 * 
 * Remote: StorageIdentifier format:
 * <globusDriverId>://<baseStorageIdentifier>//<relativePath> Storage location:
 * <globusendpointId/basepath>/<relPath> Internal StorageIdentifier format:
 * <globusDriverId>://<baseStorageIdentifier> Storage location:
 * <globusEndpointId/basepath>/<dataset authority>/<dataset
 * identifier>/<baseStorageIdentifier>
 *
 * baseUrl: globus://<globusEndpointId/basePath>
 * 
 */
public class GlobusOverlayAccessIO<T extends DvObject> extends RemoteOverlayAccessIO<T> {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.GlobusOverlayAccessIO");

    String globusAccessToken = null;
    /*
     * If this is set to true, the store supports Globus transfer in and
     * Dataverse/the globus app manage file locations, access controls, deletion,
     * etc.
     */
    private boolean dataverseManaged = false;

    public GlobusOverlayAccessIO(T dvObject, DataAccessRequest req, String driverId) throws IOException {
        super(dvObject, req, driverId);
        if (dvObject instanceof DataFile) {
            globusAccessToken = retrieveGlobusAccessToken();
        }
        dataverseManaged = isDataverseManaged(this.driverId);

        logger.info("GAT3: " + globusAccessToken);
    }

    public GlobusOverlayAccessIO(String storageLocation, String driverId) throws IOException {
        this.driverId = driverId;
        this.dataverseManaged = isDataverseManaged(this.driverId);
        if (dataverseManaged) {
            String[] parts = DataAccess.getDriverIdAndStorageLocation(storageLocation);
            path = parts[1];
        } else {
            this.setIsLocalFile(false);
            configureStores(null, driverId, storageLocation);

            path = storageLocation.substring(storageLocation.lastIndexOf("//") + 2);
            validatePath(path);
            logger.fine("Relative path: " + path);
        }
//ToDo - only when needed?
        globusAccessToken = retrieveGlobusAccessToken();

    }

    private String retrieveGlobusAccessToken() {
        // String globusToken = JvmSettings.GLOBUS_TOKEN.lookup(driverId);
        String globusToken = System.getProperty("dataverse.files." + this.driverId + ".globus-token");

        AccessToken accessToken = GlobusServiceBean.getClientToken(globusToken);
        return accessToken.getOtherTokens().get(0).getAccessToken();
    }

    private void validatePath(String relPath) throws IOException {
        try {
            URI absoluteURI = new URI(baseUrl + "/" + relPath);
            if (!absoluteURI.normalize().toString().startsWith(baseUrl)) {
                throw new IOException("storageidentifier doesn't start with " + this.driverId + "'s base-url");
            }
        } catch (URISyntaxException use) {
            throw new IOException("Could not interpret storageidentifier in remote store " + this.driverId);
        }
    }

    // Call the Globus API to get the file size
    @Override
    long retrieveSize() {
        logger.info("GAT2: " + globusAccessToken);
        // Construct Globus URL
        URI absoluteURI = null;
        try {
            int filenameStart = path.lastIndexOf("/") + 1;
            String endpointWithBasePath = baseUrl.substring(baseUrl.lastIndexOf("://") + 3);
            int pathStart = endpointWithBasePath.indexOf("/");
            logger.info("endpointWithBasePath: " + endpointWithBasePath);
            String directoryPath = "/" + (pathStart > 0 ? endpointWithBasePath.substring(pathStart + 1) : "");
            logger.info("directoryPath: " + directoryPath);

            if (dataverseManaged && (dvObject!=null)) {
                Dataset ds = ((DataFile) dvObject).getOwner();
                directoryPath = directoryPath + "/" + ds.getAuthority() + "/" + ds.getIdentifier();
                logger.info("directoryPath now: " + directoryPath);

            }
            if (filenameStart > 0) {
                directoryPath = directoryPath + path.substring(0, filenameStart);
            }
            logger.info("directoryPath finally: " + directoryPath);
            String filename = path.substring(filenameStart);
            String endpoint = pathStart > 0 ? endpointWithBasePath.substring(0, pathStart) : endpointWithBasePath;

            absoluteURI = new URI("https://transfer.api.globusonline.org/v0.10/operation/endpoint/" + endpoint
                    + "/ls?path=" + directoryPath + "&filter=name:" + filename);
            HttpGet get = new HttpGet(absoluteURI);

            logger.info("Token is " + globusAccessToken);
            get.addHeader("Authorization", "Bearer " + globusAccessToken);
            CloseableHttpResponse response = getSharedHttpClient().execute(get, localContext);
            if (response.getStatusLine().getStatusCode() == 200) {
                // Get reponse as string
                String responseString = EntityUtils.toString(response.getEntity());
                logger.info("Response from " + get.getURI().toString() + " is: " + responseString);
                JsonObject responseJson = JsonUtil.getJsonObject(responseString);
                return (long) responseJson.getJsonArray("DATA").getJsonObject(0).getInt("size");
            } else {
                logger.warning("Response from " + get.getURI().toString() + " was "
                        + response.getStatusLine().getStatusCode());
                logger.info(EntityUtils.toString(response.getEntity()));
            }
        } catch (URISyntaxException e) {
            // Should have been caught in validatePath
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    
    @Override
    public InputStream getInputStream() throws IOException {
        throw new IOException("Not implemented");
    }
    
    @Override
    public void delete() throws IOException {

// Fix
        // Delete is best-effort - we tell the remote server and it may or may not
        // implement this call
        if (!isDirectAccess()) {
            throw new IOException("Direct Access IO must be used to permanently delete stored file objects");
        }
        try {
            HttpDelete del = new HttpDelete(baseUrl + "/" + path);
            CloseableHttpResponse response = getSharedHttpClient().execute(del, localContext);
            try {
                int code = response.getStatusLine().getStatusCode();
                switch (code) {
                case 200:
                    logger.fine("Sent DELETE for " + baseUrl + "/" + path);
                default:
                    logger.fine("Response from DELETE on " + del.getURI().toString() + " was " + code);
                }
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            throw new IOException("Error deleting: " + baseUrl + "/" + path);

        }

        // Delete all the cached aux files as well:
        deleteAllAuxObjects();

    }

    @Override
    public String generateTemporaryDownloadUrl(String auxiliaryTag, String auxiliaryType, String auxiliaryFileName)
            throws IOException {
//Fix
        // ToDo - support remote auxiliary Files
        if (auxiliaryTag == null) {
            String secretKey = System.getProperty("dataverse.files." + this.driverId + ".secret-key");
            if (secretKey == null) {
                return baseUrl + "/" + path;
            } else {
                return UrlSignerUtil.signUrl(baseUrl + "/" + path, getUrlExpirationMinutes(), null, "GET", secretKey);
            }
        } else {
            return baseStore.generateTemporaryDownloadUrl(auxiliaryTag, auxiliaryType, auxiliaryFileName);
        }
    }

    private static boolean isDataverseManaged(String driverId) {
        return Boolean.getBoolean("dataverse.files." + driverId + ".managed");
    }

    static boolean isValidIdentifier(String driverId, String storageId) {
        String baseIdentifier = storageId.substring(storageId.lastIndexOf("//") + 2);
        String baseUrl = System.getProperty("dataverse.files." + driverId + ".base-url");
        if (baseUrl == null) {
            return false;
        }
        // Internally managed endpoints require standard name pattern (submitted via
        // /addFile(s) api)
        if (isDataverseManaged(driverId)) {
            boolean hasStandardName = usesStandardNamePattern(baseIdentifier);
            if (hasStandardName) {
                return true;
            } else {
                logger.warning("Unacceptable identifier pattern in submitted identifier: " + baseIdentifier);
                return false;
            }
        }
        // Remote endpoints require a valid URI within the baseUrl
        try {
            URI absoluteURI = new URI(baseUrl + "/" + baseIdentifier);
            if (!absoluteURI.normalize().toString().startsWith(baseUrl)) {
                logger.warning("storageidentifier doesn't start with " + driverId + "'s base-url: " + storageId);
                return false;
            }
        } catch (URISyntaxException use) {
            logger.warning("Could not interpret storageidentifier in remote store " + driverId + " : " + storageId);
            logger.warning(use.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println("Running the main method");
        if (args.length > 0) {
            System.out.printf("List of arguments: {}", Arrays.toString(args));
        }
        // System.setProperty("dataverse.files.globus.globus_client_id",
        // "2791b83e-b989-47c5-a7fa-ce65fd949522");
        System.setProperty("dataverse.files.globus.base-url", "globus://d8c42580-6528-4605-9ad8-116a61982644");
        System.out.println("NotValid: " + isValidIdentifier("globus", "globus://localid//../of/the/hill"));
        System.out.println("ValidRemote: " + isValidIdentifier("globus", "globus://localid//of/the/hill"));
        System.setProperty("dataverse.files.globus.managed", "true");

        System.out.println("ValidLocal: " + isValidIdentifier("globus", "globus://176e28068b0-1c3f80357c42"));
        // System.setProperty("dataverse.files.globus.globus-token","Mjc5MWI4M2UtYjk4OS00N2M1LWE3ZmEtY2U2NWZkOTQ5NTIyOkt4ZEdndFVDUDVZZG5sRG4rRHEzaVMxTHBtTVRGNlB3RjlwWm9kRTBWNVE9");
        System.setProperty("dataverse.files.globus.globus-token",
                "YTVlNzFjNzItYWVkYi00Mzg4LTkzNWQtY2NhM2IyODI2MzdmOnErQXRBeWNEMVM3amFWVnB0RlFnRk5zMTc3OFdDa3lGeVZPT3k0RDFpaXM9");
        System.setProperty("dataverse.files.globus.base-store", "file");
        System.setProperty("dataverse.files.file.type", DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
        System.setProperty("dataverse.files.file.directory", "/tmp/files");
        // logger.info(JvmSettings.BASE_URL.lookup("globus"));
        // logger.info(JvmSettings.GLOBUS_TOKEN.lookup("globus"));

        try {
            GlobusOverlayAccessIO<DvObject> gsio = new GlobusOverlayAccessIO<DvObject>(
                    "globus://1234///hdc1/image001.mrc", "globus");
            logger.info("Size is " + gsio.retrieveSize());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataFile df = new DataFile();
            Dataset ds = new Dataset();
            ds.setAuthority("10.5072");
            ds.setIdentifier("FK21234");
            df.setOwner(ds);
            df.setStorageIdentifier("globus://1234///hdc1/image001.mrc");
            GlobusOverlayAccessIO<DvObject> gsio = new GlobusOverlayAccessIO<DvObject>(df, null, "globus");
            logger.info("Size2 is " + gsio.retrieveSize());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
