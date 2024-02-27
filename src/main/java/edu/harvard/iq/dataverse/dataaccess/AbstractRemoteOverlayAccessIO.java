package edu.harvard.iq.dataverse.dataaccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
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
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;


/**
 * A base class for StorageIO implementations supporting remote access. At present, that includes the RemoteOverlayAccessIO store and the newer GlobusOverlayAccessIO store. It primarily includes
 * common methods for handling auxiliary files in the configured base store.
 * @param <T>
 */
public abstract class AbstractRemoteOverlayAccessIO<T extends DvObject> extends StorageIO<T> {

    protected static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.RemoteOverlayAccessIO");
    public static final String REFERENCE_ENDPOINTS_WITH_BASEPATHS = "reference-endpoints-with-basepaths";
    static final String BASE_STORE = "base-store";
    protected static final String SECRET_KEY = "secret-key";
    static final String URL_EXPIRATION_MINUTES = "url-expiration-minutes";
    protected static final String REMOTE_STORE_NAME = "remote-store-name";
    protected static final String REMOTE_STORE_URL = "remote-store-url";
    
    // Whether Dataverse can access the file bytes
    // Currently False only for the Globus store when using the S3Connector, and Remote Stores like simple web servers where the URLs resolve to the actual file bits
    static final String FILES_NOT_ACCESSIBLE_BY_DATAVERSE = "files-not-accessible-by-dataverse";

    protected StorageIO<DvObject> baseStore = null;
    protected String path = null;
    protected PoolingHttpClientConnectionManager cm = null;
    CloseableHttpClient httpclient = null;
    protected static HttpClientContext localContext = HttpClientContext.create();

    protected int timeout = 1200;
    protected RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000)
                .setCookieSpec(CookieSpecs.STANDARD).setExpectContinueEnabled(true).build();
    protected static boolean trustCerts = false;
    protected int httpConcurrency = 4;

    public static String getBaseStoreIdFor(String driverId) {
        return getConfigParamForDriver(driverId, BASE_STORE);
    }

    public AbstractRemoteOverlayAccessIO() {
        super();
    }

    public AbstractRemoteOverlayAccessIO(String storageLocation, String driverId) {
        super(storageLocation, driverId);
    }

    public AbstractRemoteOverlayAccessIO(T dvObject, DataAccessRequest req, String driverId) {
        super(dvObject, req, driverId);
    }

    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {
        return baseStore.openAuxChannel(auxItemTag, options);
    }

    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        return baseStore.isAuxObjectCached(auxItemTag);
    }

    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        return baseStore.getAuxObjectSize(auxItemTag);
    }

    @Override
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
        return baseStore.getAuxObjectAsPath(auxItemTag);
    }

    @Override
    public void backupAsAux(String auxItemTag) throws IOException {
        baseStore.backupAsAux(auxItemTag);
    }

    @Override
    public void revertBackupAsAux(String auxItemTag) throws IOException {
        baseStore.revertBackupAsAux(auxItemTag);
    }

    @Override
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        baseStore.savePathAsAux(fileSystemPath, auxItemTag);
    }

    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag, Long filesize) throws IOException {
        baseStore.saveInputStreamAsAux(inputStream, auxItemTag, filesize);
    }

    /**
     * @param inputStream InputStream we want to save
     * @param auxItemTag  String representing this Auxiliary type ("extension")
     * @throws IOException if anything goes wrong.
     */
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        baseStore.saveInputStreamAsAux(inputStream, auxItemTag);
    }

    @Override
    public List<String> listAuxObjects() throws IOException {
        return baseStore.listAuxObjects();
    }

    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
        baseStore.deleteAuxObject(auxItemTag);
    }

    @Override
    public void deleteAllAuxObjects() throws IOException {
        baseStore.deleteAllAuxObjects();
    }

    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {
        return baseStore.getAuxFileAsInputStream(auxItemTag);
    }

    protected int getUrlExpirationMinutes() {
        String optionValue = getConfigParam(URL_EXPIRATION_MINUTES);
        if (optionValue != null) {
            Integer num;
            try {
                num = Integer.parseInt(optionValue);
            } catch (NumberFormatException ex) {
                num = null;
            }
            if (num != null) {
                return num;
            }
        }
        return 60;
    }

    public CloseableHttpClient getSharedHttpClient() {
        if (httpclient == null) {
            try {
                initHttpPool();
                httpclient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config).build();
    
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                logger.warning(ex.getMessage());
            }
        }
        return httpclient;
    }

    private void initHttpPool() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        if (trustCerts) {
            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext;
            SSLConnectionSocketFactory connectionFactory;
    
            sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
            // create an SSL Socket Factory to use the SSLContext with the trust self signed
            // certificate strategy
            // and allow all hosts verifier.
            connectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", connectionFactory).build();
            cm = new PoolingHttpClientConnectionManager(registry);
        } else {
            cm = new PoolingHttpClientConnectionManager();
        }
        cm.setDefaultMaxPerRoute(httpConcurrency);
        cm.setMaxTotal(httpConcurrency > 20 ? httpConcurrency : 20);
    }

    @Override
    abstract public long retrieveSizeFromMedia();
    
    @Override
    public boolean exists() {
        logger.fine("Exists called");
        return (retrieveSizeFromMedia() != -1);
    }

    @Override
    public List<String> cleanUp(Predicate<String> filter, boolean dryRun) throws IOException {
        return baseStore.cleanUp(filter, dryRun);
    }
    
    @Override
    public String getStorageLocation() throws IOException {
        String fullStorageLocation = dvObject.getStorageIdentifier();
        logger.fine("storageidentifier: " + fullStorageLocation);
        int driverIndex = fullStorageLocation.lastIndexOf(DataAccess.SEPARATOR);
        if (driverIndex >= 0) {
            fullStorageLocation = fullStorageLocation
                    .substring(fullStorageLocation.lastIndexOf(DataAccess.SEPARATOR) + DataAccess.SEPARATOR.length());
        }
        if (this.getDvObject() instanceof Dataset) {
            throw new IOException("AbstractRemoteOverlayAccessIO: Datasets are not a supported dvObject");
        } else if (this.getDvObject() instanceof DataFile) {
            fullStorageLocation = StorageIO.getDriverPrefix(this.driverId) + fullStorageLocation;
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("AbstractRemoteOverlayAccessIO: Dataverses are not a supported dvObject");
        }
        logger.fine("fullStorageLocation: " + fullStorageLocation);
        return fullStorageLocation;
    }
    protected void configureStores(DataAccessRequest req, String driverId, String storageLocation) throws IOException {

        if (baseStore == null) {
            String baseDriverId = getBaseStoreIdFor(driverId);
            String fullStorageLocation = null;
            String baseDriverType = getConfigParamForDriver(baseDriverId, StorageIO.TYPE,
                    DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);

            if (dvObject instanceof Dataset) {
                baseStore = DataAccess.getStorageIO(dvObject, req, baseDriverId);
            } else {
                if (this.getDvObject() != null) {
                    fullStorageLocation = getStoragePath();

                    // S3 expects <id>://<bucketname>/<key>
                    switch (baseDriverType) {
                    case DataAccess.S3:
                        fullStorageLocation = baseDriverId + DataAccess.SEPARATOR
                                + getConfigParamForDriver(baseDriverId, S3AccessIO.BUCKET_NAME) + "/"
                                + fullStorageLocation;
                        break;
                    case DataAccess.FILE:
                        fullStorageLocation = baseDriverId + DataAccess.SEPARATOR
                                + getConfigParamForDriver(baseDriverId, FileAccessIO.DIRECTORY, "/tmp/files")
                                + "/" + fullStorageLocation;
                        break;
                    default:
                        logger.warning("Not Supported: " + this.getClass().getName() + " store with base store type: "
                                + getConfigParamForDriver(baseDriverId, StorageIO.TYPE));
                        throw new IOException("Not supported");
                    }

                } else if (storageLocation != null) {
                    // <remoteDriverId>://<baseStorageIdentifier>//<baseUrlPath>
                    // remoteDriverId:// is removed if coming through directStorageIO
                    int index = storageLocation.indexOf(DataAccess.SEPARATOR);
                    if (index > 0) {
                        storageLocation = storageLocation.substring(index + DataAccess.SEPARATOR.length());
                    }
                    // The base store needs the baseStoreIdentifier and not the relative URL (if it exists)
                    int endOfId = storageLocation.indexOf("//");
                    fullStorageLocation = (endOfId>-1) ? storageLocation.substring(0, endOfId) : storageLocation;

                    switch (baseDriverType) {
                    case DataAccess.S3:
                        fullStorageLocation = baseDriverId + DataAccess.SEPARATOR
                                + getConfigParamForDriver(baseDriverId, S3AccessIO.BUCKET_NAME) + "/"
                                + fullStorageLocation;
                        break;
                    case DataAccess.FILE:
                        fullStorageLocation = baseDriverId + DataAccess.SEPARATOR
                                + getConfigParamForDriver(baseDriverId, FileAccessIO.DIRECTORY, "/tmp/files")
                                + "/" + fullStorageLocation;
                        break;
                    default:
                        logger.warning("Not Supported: " + this.getClass().getName() + " store with base store type: "
                                + getConfigParamForDriver(baseDriverId, StorageIO.TYPE));
                        throw new IOException("Not supported");
                    }
                }
                baseStore = DataAccess.getDirectStorageIO(fullStorageLocation);
            }
            if (baseDriverType.contentEquals(DataAccess.S3)) {
                ((S3AccessIO<?>) baseStore).setMainDriver(false);
            }
        }
        remoteStoreName = getConfigParam(REMOTE_STORE_NAME);
        try {
            remoteStoreUrl = new URL(getConfigParam(REMOTE_STORE_URL));
        } catch (MalformedURLException mfue) {
            logger.fine("Unable to read remoteStoreUrl for driver: " + this.driverId);
        }
    }

    protected String getStoragePath() throws IOException {
        String fullStoragePath = dvObject.getStorageIdentifier();
        logger.fine("storageidentifier: " + fullStoragePath);
        int driverIndex = fullStoragePath.lastIndexOf(DataAccess.SEPARATOR);
        if (driverIndex >= 0) {
            fullStoragePath = fullStoragePath
                    .substring(fullStoragePath.lastIndexOf(DataAccess.SEPARATOR) + DataAccess.SEPARATOR.length());
        }
        int suffixIndex = fullStoragePath.indexOf("//");
        if (suffixIndex >= 0) {
            fullStoragePath = fullStoragePath.substring(0, suffixIndex);
        }
        if (getDvObject() instanceof Dataset) {
            fullStoragePath = getDataset().getAuthorityForFileStorage() + "/"
                    + getDataset().getIdentifierForFileStorage() + "/" + fullStoragePath;
        } else if (getDvObject() instanceof DataFile) {
            fullStoragePath = getDataFile().getOwner().getAuthorityForFileStorage() + "/"
                    + getDataFile().getOwner().getIdentifierForFileStorage() + "/" + fullStoragePath;
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("RemoteOverlayAccessIO: Dataverses are not a supported dvObject");
        }
        logger.fine("fullStoragePath: " + fullStoragePath);
        return fullStoragePath;
    }

}