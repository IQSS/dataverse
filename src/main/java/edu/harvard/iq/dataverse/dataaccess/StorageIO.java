/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.dataaccess;


import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;


import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;


/**
 *
 * @author Leonid Andreev
 * @param <T> what it writes
 */

public abstract class StorageIO<T extends DvObject> {

    public StorageIO() {

    }

    public StorageIO(T dvObject) {
        this(dvObject, null);
    }

    public StorageIO(T dvObject, DataAccessRequest req) {
        this.dvObject = dvObject;
        this.req = req;
        if (this.req == null) {
            this.req = new DataAccessRequest();
        }
    }

    
    
    // Abstract methods to be implemented by the storage drivers:

    public abstract void open(DataAccessOption... option) throws IOException;

    protected boolean isReadAccess = false;
    protected boolean isWriteAccess = false;

    public boolean canRead() {
        return isReadAccess;
    }

    public boolean canWrite() {
        return isWriteAccess;
    }

    public abstract String getStorageLocation() throws IOException;
    // do we need this method?

    // This method will return a Path, if the storage method is a 
    // local filesystem. Otherwise should throw an IOException. 
    public abstract Path getFileSystemPath() throws IOException;
    
    public abstract boolean exists() throws IOException; 

    // This method will delete the physical file (object), if delete
    // functionality is supported by the physical driver. 
    // TODO: this method should throw something other than IOException 
    // if delete functionality is not supported by the access driver!
    // -- L.A. 4.0. beta
    // (there is now a dedicated exception for this purpose: UnsupportedDataAccessOperationException,
    // that extends IOException -- 4.6.2) 
    public abstract void delete() throws IOException;
    
    // this method for copies a local Path (for ex., a
    // temp file, into this DataAccess location):
    public abstract void savePath(Path fileSystemPath) throws IOException;
    
    // same, for an InputStream:
    /**
     * This method copies a local InputStream into this DataAccess location.
     * Note that the S3 driver implementation of this abstract method is problematic, 
     * because S3 cannot save an object of an unknown length. This effectively 
     * nullifies any benefits of streaming; as we cannot start saving until we 
     * have read the entire stream. 
     * One way of solving this would be to buffer the entire stream as byte[], 
     * in memory, then save it... Which of course would be limited by the amount 
     * of memory available, and thus would not work for streams larger than that. 
     * So we have eventually decided to save save the stream to a temp file, then 
     * save to S3. This is slower, but guaranteed to work on any size stream. 
     * An alternative we may want to consider is to not implement this method 
     * in the S3 driver, and make it throw the UnsupportedDataAccessOperationException, 
     * similarly to how we handle attempts to open OutputStreams, in this and the 
     * Swift driver. 
     * (Not an issue in either FileAccessIO or SwiftAccessIO implementations)
     * 
     * @param inputStream InputStream we want to save
     * @param auxItemTag String representing this Auxiliary type ("extension")
     * @throws IOException if anything goes wrong.
    */
    public abstract void saveInputStream(InputStream inputStream) throws IOException;
    public abstract void saveInputStream(InputStream inputStream, Long filesize) throws IOException;
    
    // Auxiliary File Management: (new as of 4.0.2!)
    
    // An "auxiliary object" is an abstraction of the traditional DVN/Dataverse
    // mechanism of storing extra files related to the man StudyFile/DataFile - 
    // such as "saved original" and cached format conversions for tabular files, 
    // thumbnails for images, etc. - in physical files with the same file 
    // name but various reserved extensions. 
   
    //This function retrieves auxiliary files related to datasets, and returns them as inputstream
    public abstract InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException ;
    
    public abstract Channel openAuxChannel(String auxItemTag, DataAccessOption... option) throws IOException;
    
    public abstract long getAuxObjectSize(String auxItemTag) throws IOException; 
    
    public abstract Path getAuxObjectAsPath(String auxItemTag) throws IOException; 
    
    public abstract boolean isAuxObjectCached(String auxItemTag) throws IOException; 
    
    public abstract void backupAsAux(String auxItemTag) throws IOException; 
    
    public abstract void revertBackupAsAux(String auxItemTag) throws IOException; 
    
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public abstract void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException;
    
    /**
     * This method copies a local InputStream into this DataAccess Auxiliary location.
     * Note that the S3 driver implementation of this abstract method is problematic, 
     * because S3 cannot save an object of an unknown length. This effectively 
     * nullifies any benefits of streaming; as we cannot start saving until we 
     * have read the entire stream. 
     * One way of solving this would be to buffer the entire stream as byte[], 
     * in memory, then save it... Which of course would be limited by the amount 
     * of memory available, and thus would not work for streams larger than that. 
     * So we have eventually decided to save save the stream to a temp file, then 
     * save to S3. This is slower, but guaranteed to work on any size stream. 
     * An alternative we may want to consider is to not implement this method 
     * in the S3 driver, and make it throw the UnsupportedDataAccessOperationException, 
     * similarly to how we handle attempts to open OutputStreams, in this and the 
     * Swift driver. 
     * (Not an issue in either FileAccessIO or SwiftAccessIO implementations)
     * 
     * @param inputStream InputStream we want to save
     * @param auxItemTag String representing this Auxiliary type ("extension")
     * @throws IOException if anything goes wrong.
    */
    public abstract void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException; 
    public abstract void saveInputStreamAsAux(InputStream inputStream, String auxItemTag, Long filesize) throws IOException;
    
    public abstract List<String>listAuxObjects() throws IOException;
    
    public abstract void deleteAuxObject(String auxItemTag) throws IOException; 
    
    public abstract void deleteAllAuxObjects() throws IOException;

    private DataAccessRequest req;
    private InputStream in;
    private OutputStream out; 
    protected Channel channel;
    protected DvObject dvObject;

    private int status;
    private long size;
    
    private String mimeType;
    private String fileName;
    private String varHeader;
    private String errorMessage;

    private String temporarySwiftUrl;
    private String tempUrlExpiry;
    private String tempUrlSignature;

    private String swiftContainerName;

    private boolean isLocalFile = false;
    private boolean isRemoteAccess = false;
    private boolean isHttpAccess = false;
    private boolean noVarHeader = false;

    // For remote downloads:
    private boolean isZippedStream = false;
    private boolean isDownloadSupported = true;
    private boolean isSubsetSupported = false;

    private String swiftFileName;


    
    // For HTTP-based downloads:
    private String remoteUrl;
    private GetMethod method = null;
    private Header[] responseHeaders;

    // getters:
    
    public Channel getChannel() {
        return channel;
    }

    public WritableByteChannel getWriteChannel() throws IOException {
        if (canWrite() && channel != null && channel instanceof WritableByteChannel) {
            return (WritableByteChannel) channel;
        }

        throw new IOException("No NIO write access in this DataAccessObject.");
    }

    public ReadableByteChannel getReadChannel() throws IOException {
        if (!canRead() || channel == null || !(channel instanceof ReadableByteChannel)) {
            throw new IOException("No NIO read access in this DataAccessObject.");
        }

        return (ReadableByteChannel) channel;
    }
    
    public DvObject getDvObject()
    {
        return dvObject;
    }
    
    public DataFile getDataFile() {
        return (DataFile) dvObject;
    }
    
    public Dataset getDataset() {
        return (Dataset) dvObject;
    }

    public Dataverse getDataverse() {
        return (Dataverse) dvObject;
    }

    public DataAccessRequest getRequest() {
        return req;
    }

    public int getStatus() {
        return status;
    }

    public long getSize() {
        return size;
    }

    public InputStream getInputStream() {
        return in;
    }
    
    public OutputStream getOutputStream() throws IOException {
        return out; 
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getVarHeader() {
        return varHeader;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getTemporarySwiftUrl(){
        return temporarySwiftUrl;
    }
    
    public String getTempUrlExpiry() {
        return tempUrlExpiry;
    }
    
    public String getTempUrlSignature() {
        return tempUrlSignature;
    }
    
    public String getSwiftFileName() {
        return swiftFileName;
    }

    public String getSwiftContainerName(){
        return swiftContainerName;
    }

    public GetMethod getHTTPMethod() {
        return method;
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }

    public boolean isLocalFile() {
        return isLocalFile;
    }

    public boolean isRemoteAccess() {
        return isRemoteAccess;
    }

    public boolean isHttpAccess() {
        return isHttpAccess;
    }

    public boolean isDownloadSupported() {
        return isDownloadSupported;
    }

    public boolean isSubsetSupported() {
        return isSubsetSupported;
    }

    public boolean isZippedStream() {
        return isZippedStream;
    }

    public boolean noVarHeader() {
        return noVarHeader;
    }

        // setters:
    public void setDvObject(T f) {
        dvObject = f;
    }

    public void setRequest(DataAccessRequest dar) {
        req = dar;
    }

    public void setStatus(int s) {
        status = s;
    }

    public void setSize(long s) {
        size = s;
    }

    public void setInputStream(InputStream is) {
        in = is;
    }
    
    public void setOutputStream(OutputStream os) {
        out = os; 
    } 
    
    public void setChannel(Channel c) {
        channel = c;
    }

    public void setMimeType(String mt) {
        mimeType = mt;
    }

    public void setFileName(String fn) {
        fileName = fn;
    }

    public void setVarHeader(String vh) {
        varHeader = vh;
    }

    public void setErrorMessage(String em) {
        errorMessage = em;
    }

    public void setRemoteUrl(String u) {
        remoteUrl = u;
    }

    public void setTemporarySwiftUrl(String u){
        temporarySwiftUrl = u;
    }
    
    public void setTempUrlExpiry(Long u){
        tempUrlExpiry = String.valueOf(u);
    }
    
    public void setSwiftFileName(String u) {
        swiftFileName = u;
    }
    
    public void setTempUrlSignature(String u){
        tempUrlSignature = u;
    }

    public void setSwiftContainerName(String u){
        swiftContainerName = u;
    }

    public void setHTTPMethod(GetMethod hm) {
        method = hm;
    }

    public void setResponseHeaders(Header[] headers) {
        responseHeaders = headers;
    }

    public void setIsLocalFile(boolean f) {
        isLocalFile = f;
    }

    public void setIsRemoteAccess(boolean r) {
        isRemoteAccess = r;
    }

    public void setIsHttpAccess(boolean h) {
        isHttpAccess = h;
    }

    public void setIsDownloadSupported(boolean d) {
        isDownloadSupported = d;
    }

    public void setIsSubsetSupported(boolean s) {
        isSubsetSupported = s;
    }

    public void setIsZippedStream(boolean zs) {
        isZippedStream = zs;
    }

    public void setNoVarHeader(boolean nvh) {
        noVarHeader = nvh;
    }

        // connection management methods:
    public void releaseConnection() {
        if (method != null) {
            method.releaseConnection();
        }
    }

    public void closeInputStream() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ex) {
                // we really don't care.
                String eMsg = "Warning: IO exception closing input stream.";
                if (errorMessage == null) {
                    errorMessage = eMsg;
                } else {
                    errorMessage = eMsg + "; " + errorMessage;
                }
            }
        }
    }
    
    public String generateVariableHeader(List<DataVariable> dvs) {
        String varHeader = null;

        if (dvs != null) {
            Iterator<DataVariable> iter = dvs.iterator();
            DataVariable dv;

            if (iter.hasNext()) {
                dv = iter.next();
                varHeader = dv.getName();
            }

            while (iter.hasNext()) {
                dv = iter.next();
                varHeader = varHeader + "\t" + dv.getName();
            }

            varHeader = varHeader + "\n";
        }

        return varHeader;
    }

    protected boolean isWriteAccessRequested(DataAccessOption... options) throws IOException {

        for (DataAccessOption option : options) {
            // In the future we may need to be able to open read-write
            // Channels; no support, or use case for that as of now.

            if (option == DataAccessOption.READ_ACCESS) {
                return false;
            }

            if (option == DataAccessOption.WRITE_ACCESS) {
                return true;
            }
        }

        // By default, we open the file in read mode:
        return false;
    }
}
