package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * @author qqmyers
 */
/*
 * Remote Overlay Driver
 * 
 * StorageIdentifier format:
 * <remoteDriverId>://<baseStorageIdentifier>//<relativePath>
 * 
 * baseUrl: http(s)://<host(:port)/basePath>
 */
public class RemoteOverlayAccessIO<T extends DvObject> extends AbstractRemoteOverlayAccessIO<T> {

    // A single baseUrl of the form http(s)://<host(:port)/basePath> where this store can reference data
    static final String BASE_URL = "base-url";
    String baseUrl = null;

    public RemoteOverlayAccessIO() {
        super();
    }
    
    public RemoteOverlayAccessIO(T dvObject, DataAccessRequest req, String driverId) throws IOException {
        super(dvObject, req, driverId);
        this.setIsLocalFile(false);
        configureRemoteEndpoints();
        configureStores(req, driverId, null);
        logger.fine("Parsing storageidentifier: " + dvObject.getStorageIdentifier());
        path = dvObject.getStorageIdentifier().substring(dvObject.getStorageIdentifier().lastIndexOf("//") + 2);
        validatePath(path);

        logger.fine("Relative path: " + path);
    }

    public RemoteOverlayAccessIO(String storageLocation, String driverId) throws IOException {
        super(null, null, driverId);
        this.setIsLocalFile(false);
        configureRemoteEndpoints();
        configureStores(null, driverId, storageLocation);

        path = storageLocation.substring(storageLocation.lastIndexOf("//") + 2);
        validatePath(path);
        logger.fine("Relative path: " + path);
    }

    protected void validatePath(String relPath) throws IOException {
        try {
            URI absoluteURI = new URI(baseUrl + "/" + relPath);
            if (!absoluteURI.normalize().toString().startsWith(baseUrl)) {
                throw new IOException("storageidentifier doesn't start with " + this.driverId + "'s base-url");
            }
        } catch (URISyntaxException use) {
            throw new IOException("Could not interpret storageidentifier in remote store " + this.driverId);
        }
    }

    @Override
    public void open(DataAccessOption... options) throws IOException {

        baseStore.open(options);

        DataAccessRequest req = this.getRequest();

        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }

        if (dvObject instanceof DataFile) {
            String storageIdentifier = dvObject.getStorageIdentifier();

            DataFile dataFile = this.getDataFile();

            if (req != null && req.getParameter("noVarHeader") != null) {
                baseStore.setNoVarHeader(true);
            }

            if (storageIdentifier == null || "".equals(storageIdentifier)) {
                throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
            }

            // Fix new DataFiles: DataFiles that have not yet been saved may use this method
            // when they don't have their storageidentifier in the final form
            // So we fix it up here. ToDo: refactor so that storageidentifier is generated
            // by the appropriate StorageIO class and is final from the start.
            logger.fine("StorageIdentifier is: " + storageIdentifier);

            if (isReadAccess) {
                if (dataFile.getFilesize() >= 0) {
                    this.setSize(dataFile.getFilesize());
                } else {
                    logger.fine("Setting size");
                    this.setSize(retrieveSizeFromMedia());
                }
                if (dataFile.getContentType() != null 
                        && dataFile.getContentType().equals("text/tab-separated-values")
                        && dataFile.isTabularData() 
                        && dataFile.getDataTable() != null 
                        && (!this.noVarHeader())
                        && (!dataFile.getDataTable().isStoredWithVariableHeader())) {

                    List<DataVariable> datavariables = dataFile.getDataTable().getDataVariables();
                    String varHeaderLine = generateVariableHeader(datavariables);
                    this.setVarHeader(varHeaderLine);
                }

            }

            this.setMimeType(dataFile.getContentType());

            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }
        } else if (dvObject instanceof Dataset) {
            throw new IOException(
                    "Data Access: " + this.getClass().getName() + " does not support dvObject type Dataverse yet");
        } else if (dvObject instanceof Dataverse) {
            throw new IOException(
                    "Data Access: " + this.getClass().getName() + " does not support dvObject type Dataverse yet");
        }
    }

    @Override
    public long retrieveSizeFromMedia() {
        long size = -1;
        HttpHead head = new HttpHead(baseUrl + "/" + path);
        try {
            CloseableHttpResponse response = getSharedHttpClient().execute(head, localContext);

            try {
                int code = response.getStatusLine().getStatusCode();
                logger.fine("Response for HEAD: " + code);
                switch (code) {
                case 200:
                    Header[] headers = response.getHeaders(HTTP.CONTENT_LEN);
                    logger.fine("Num headers: " + headers.length);
                    String sizeString = response.getHeaders(HTTP.CONTENT_LEN)[0].getValue();
                    logger.fine("Content-Length: " + sizeString);
                    size = Long.parseLong(response.getHeaders(HTTP.CONTENT_LEN)[0].getValue());
                    logger.fine("Found file size: " + size);
                    break;
                default:
                    logger.warning("Response from " + head.getURI().toString() + " was " + code);
                }
            } finally {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
        return size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (super.getInputStream() == null) {
            try {
                HttpGet get = new HttpGet(generateTemporaryDownloadUrl(null, null, null));
                CloseableHttpResponse response = getSharedHttpClient().execute(get, localContext);

                int code = response.getStatusLine().getStatusCode();
                switch (code) {
                case 200:
                    setInputStream(response.getEntity().getContent());
                    break;
                default:
                    logger.warning("Response from " + get.getURI().toString() + " was " + code);
                    throw new IOException("Cannot retrieve: " + baseUrl + "/" + path + " code: " + code);
                }
            } catch (Exception e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
                throw new IOException("Error retrieving: " + baseUrl + "/" + path + " " + e.getMessage());

            }
            setChannel(Channels.newChannel(super.getInputStream()));
        }
        return super.getInputStream();
    }

    @Override
    public Channel getChannel() throws IOException {
        if (super.getChannel() == null) {
            getInputStream();
        }
        return channel;
    }

    @Override
    public ReadableByteChannel getReadChannel() throws IOException {
        // Make sure StorageIO.channel variable exists
        getChannel();
        return super.getReadChannel();
    }

    @Override
    public void delete() throws IOException {
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
    public Path getFileSystemPath() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException(
                "RemoteOverlayAccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }

    @Override
    public WritableByteChannel getWriteChannel() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException(
                "RemoteOverlayAccessIO: there are no write Channels associated with S3 objects.");
    }

    @Override
    public OutputStream getOutputStream() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException(
                "RemoteOverlayAccessIO: there are no output Streams associated with S3 objects.");
    }

    @Override
    public boolean downloadRedirectEnabled() {
        String optionValue = getConfigParam(StorageIO.DOWNLOAD_REDIRECT);
        if ("true".equalsIgnoreCase(optionValue)) {
            return true;
        }
        return false;
    }

    public boolean downloadRedirectEnabled(String auxObjectTag) {
        return baseStore.downloadRedirectEnabled(auxObjectTag);
    }

    @Override
    public String generateTemporaryDownloadUrl(String auxiliaryTag, String auxiliaryType, String auxiliaryFileName)
            throws IOException {

        // ToDo - support remote auxiliary Files
        if (auxiliaryTag == null) {
            String secretKey = getConfigParam(SECRET_KEY);
            if (secretKey == null) {
                return baseUrl + "/" + path;
            } else {
                return UrlSignerUtil.signUrl(baseUrl + "/" + path, getUrlExpirationMinutes(), null, "GET",
                        secretKey);
            }
        } else {
            return baseStore.generateTemporaryDownloadUrl(auxiliaryTag, auxiliaryType, auxiliaryFileName);
        }
    }


    /** This endpoint configures all the endpoints the store is allowed to reference data from. At present, the RemoteOverlayAccessIO only supports a single endpoint but
     * the derived GlobusOverlayAccessIO can support multiple endpoints.
     * @throws IOException
     */
    protected void configureRemoteEndpoints() throws IOException {
        baseUrl = getConfigParam(BASE_URL);
        if (baseUrl == null) {
            //Will accept the first endpoint using the newer setting
            baseUrl = getConfigParam(REFERENCE_ENDPOINTS_WITH_BASEPATHS).split("\\s*,\\s*")[0];
            if (baseUrl == null) {
                throw new IOException("dataverse.files." + this.driverId + ".base-url is required");
            }
        }
        if (baseUrl != null) {
            try {
                new URI(baseUrl);
            } catch (Exception e) {
                logger.warning(
                        "Trouble interpreting base-url for store: " + this.driverId + " : " + e.getLocalizedMessage());
                throw new IOException("Can't interpret base-url as a URI");
            }

        }
    }

    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        throw new UnsupportedDataAccessOperationException(
                this.getClass().getName() + ": savePath() not implemented in this storage driver.");

    }

    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
        throw new UnsupportedDataAccessOperationException(
                this.getClass().getName() + ": saveInputStream() not implemented in this storage driver.");

    }

    @Override
    public void saveInputStream(InputStream inputStream, Long filesize) throws IOException {
        throw new UnsupportedDataAccessOperationException(
                this.getClass().getName() + ": saveInputStream(InputStream, Long) not implemented in this storage driver.");

    }

    static boolean isValidIdentifier(String driverId, String storageId) {
        String urlPath = storageId.substring(storageId.lastIndexOf("//") + 2);
        String baseUrl = getConfigParamForDriver(driverId, BASE_URL);
        try {
            URI absoluteURI = new URI(baseUrl + "/" + urlPath);
            if (!absoluteURI.normalize().toString().startsWith(baseUrl)) {
                logger.warning("storageidentifier doesn't start with " + driverId + "'s base-url: " + storageId);
                return false;
            }
        } catch (URISyntaxException use) {
            logger.warning("Could not interpret storageidentifier in remote store " + driverId + " : " + storageId);
            return false;
        }
        return true;
    }
}
