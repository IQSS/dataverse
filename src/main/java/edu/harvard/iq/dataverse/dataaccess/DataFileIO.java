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
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;


import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;


/**
 *
 * @author Leonid Andreev
 */

public abstract class DataFileIO {

    public DataFileIO() {

    }

    public DataFileIO(DataFile dataFile) {
        this(dataFile, null);
    }

    public DataFileIO(DataFile dataFile, DataAccessRequest req) {
        this.dataFile = dataFile;
        this.req = req;

        if (this.req == null) {
            this.req = new DataAccessRequest();
        }
    }

    // Abstract methods to be implemented by the storage drivers:

    public abstract void open(DataAccessOption... option) throws IOException;
    
    public abstract boolean canRead();

    public abstract boolean canWrite();

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
    public abstract void saveInputStream(InputStream inputStream) throws IOException;
    
    // Auxiliary File Management: (new as of 4.0.2!)
    
    // An "auxiliary object" is an abstraction of the traditional DVN/Dataverse
    // mechanism of storing extra files related to the man StudyFile/DataFile - 
    // such as "saved original" and cached format conversions for tabular files, 
    // thumbnails for images, etc. - in physical files with the same file 
    // name but various reserved extensions. 
    
    public abstract Channel openAuxChannel(String auxItemTag, DataAccessOption... option) throws IOException;
    
    public abstract long getAuxObjectSize(String auxItemTag) throws IOException; 
    
    public abstract Path getAuxObjectAsPath(String auxItemTag) throws IOException; 
    
    public abstract boolean isAuxObjectCached(String auxItemTag) throws IOException; 
    
    public abstract void backupAsAux(String auxItemTag) throws IOException; 
    
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public abstract void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException;
    
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    public abstract void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException; 
    
    public abstract List<String>listAuxObjects() throws IOException;
    
    public abstract void deleteAuxObject(String auxItemTag) throws IOException; 
    
    public abstract void deleteAllAuxObjects() throws IOException;
    

    private DataFile dataFile;
    private DataAccessRequest req;

    private InputStream in;
    private OutputStream out; 
    protected Channel channel;

    private int status;
    private long size;
    
    private String mimeType;
    private String fileName;
    private String varHeader;
    private String errorMessage;

    //private String swiftContainerName;

    private Boolean isLocalFile = false;
    private Boolean isRemoteAccess = false;
    private Boolean isHttpAccess = false;
    private Boolean noVarHeader = false;

    // For remote downloads:
    private Boolean isZippedStream = false;
    private Boolean isDownloadSupported = true;
    private Boolean isSubsetSupported = false;

    
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
    
    public DataFile getDataFile() {
        return dataFile;
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

    // public String getSwiftContainerName(){
    //     return swiftContainerName;
    // }

    public GetMethod getHTTPMethod() {
        return method;
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }

    public Boolean isLocalFile() {
        return isLocalFile;
    }

    public Boolean isRemoteAccess() {
        return isRemoteAccess;
    }

    public Boolean isHttpAccess() {
        return isHttpAccess;
    }

    public Boolean isDownloadSupported() {
        return isDownloadSupported;
    }

    public Boolean isSubsetSupported() {
        return isSubsetSupported;
    }

    public Boolean isZippedStream() {
        return isZippedStream;
    }

    public Boolean noVarHeader() {
        return noVarHeader;
    }

        // setters:
    public void setDataFile(DataFile f) {
        dataFile = f;
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

    // public void setSwiftContainerName(String u){
    //     swiftContainerName = u;
    // }

    public void setHTTPMethod(GetMethod hm) {
        method = hm;
    }

    public void setResponseHeaders(Header[] headers) {
        responseHeaders = headers;
    }

    public void setIsLocalFile(Boolean f) {
        isLocalFile = f;
    }

    public void setIsRemoteAccess(Boolean r) {
        isRemoteAccess = r;
    }

    public void setIsHttpAccess(Boolean h) {
        isHttpAccess = h;
    }

    public void setIsDownloadSupported(Boolean d) {
        isDownloadSupported = d;
    }

    public void setIsSubsetSupported(Boolean s) {
        isSubsetSupported = s;
    }

    public void setIsZippedStream(Boolean zs) {
        isZippedStream = zs;
    }

    public void setNoVarHeader(Boolean nvh) {
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
    
    public String generateVariableHeader(List dvs) {
        String varHeader = null;

        if (dvs != null) {
            Iterator iter = dvs.iterator();
            DataVariable dv;

            if (iter.hasNext()) {
                dv = (DataVariable) iter.next();
                varHeader = dv.getName();
            }

            while (iter.hasNext()) {
                dv = (DataVariable) iter.next();
                varHeader = varHeader + "\t" + dv.getName();
            }

            varHeader = varHeader + "\n";
        }

        return varHeader;
    }
}
