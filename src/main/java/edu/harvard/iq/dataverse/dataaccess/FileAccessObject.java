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

// java core imports:
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.Iterator; 


// DVN App imports:
import edu.harvard.iq.dataverse.DataFile;
//mport edu.harvard.iq.dataverse.TabularDataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.FileOutputStream;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileAccessObject extends DataAccessObject {

    public FileAccessObject () throws IOException {
        this(null);
    }

    public FileAccessObject(DataFile file) throws IOException {
        this (file, null);
    }

    public FileAccessObject(DataFile file, DataAccessRequest req) throws IOException {

        super(file, req);

        /*
        if (file != null && file.isRemote()) {
            //return null; 
            throw new IOException ("not a local file.");
        }
         */


        this.setIsLocalFile(true);
        this.setIsDownloadSupported(true);
        this.setIsNIOSupported(true);
    }
    
    private boolean isReadAccess = false;
    private boolean isWriteAccess = false; 
    
    @Override
    public boolean canAccess (String location) throws IOException{
        return true;
    }

    @Override
    public boolean canRead () {
        return isReadAccess;
    }
    
    @Override
    public boolean canWrite () {
        return isWriteAccess; 
    }
    //public void open (String location) throws IOException{

    //}

    //private void open (DataFile file, Object req) throws IOException {
    @Override
    public void open () throws IOException {

        DataFile file = this.getFile();
        DataAccessRequest req = this.getRequest(); 


        //if (req instanceof HttpServletRequest) {
        //    if (((HttpServletRequest)req).getParameter("noVarHeader") != null) {
        //        this.setNoVarHeader(true);
        //    }
        //}

        if (req.getParameter("noVarHeader") != null) {
            this.setNoVarHeader(true);
        }
        InputStream in = openLocalFileAsStream(file);

        if (in == null) {
            throw new IOException ("Failed to open local file "+file.getFileSystemLocation());
        }

        this.setInputStream(in);

        this.setSize(getLocalFileSize(file));

        this.setMimeType(file.getContentType());
        this.setFileName(file.getFileMetadata().getLabel());

        
        if (file.getContentType() != null &&
            file.getContentType().equals("text/tab-separated-values")  &&
            file.isTabularData() &&
            file.getDataTable() != null &&
            (!this.noVarHeader())) {

            List datavariables = file.getDataTable().getDataVariables();
            String varHeaderLine = generateVariableHeader(datavariables);
            this.setVarHeader(varHeaderLine);
        }

        // The HTTP headers for the final download (when an HTTP download
        // of the file is requested, as opposed to the object being read by
        // another part of the system). This is a TODO -- need to design this
        // carefully.
        
//        setDownloadContentHeaders (localDownload);


        this.setStatus(200);
    } // End of initiateLocalDownload;

    @Override
    public String getStorageLocation() {
        
            Path studyDirectoryPath = this.getFile().getOwner().getFileSystemDirectory();
            
            if (studyDirectoryPath == null) {
                return null;
            }
            String studyDirectory = studyDirectoryPath.toString();
 
            return Paths.get(studyDirectory, this.getFile().getFileSystemName()).toString();
        }
    
    @Override
    public Path getFileSystemPath() throws IOException {
        if (this.getFile() == null) {
            throw new IOException("No datafile defined in the Data Access Object");
        }
        
        if (this.getFile().getOwner() == null) {
            throw new IOException("Data Access: no parent dataset defined for this datafile");
        }
        
        Path studyDirectoryPath = this.getFile().getOwner().getFileSystemDirectory();

        if (studyDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the parent dataset.");
        }
        String datasetDirectory = studyDirectoryPath.toString();
        
        if (this.getFile().getFileSystemName() == null || "".equals(this.getFile().getFileSystemName())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        return Paths.get(datasetDirectory, this.getFile().getFileSystemName());

    }
    
    @Override
    public void delete() throws IOException {
        Path victim = getFileSystemPath();
        
        if (victim != null) {
            Files.delete(victim);
        } else {
            throw new IOException("Could not locate physical file location for the Filesystem object.");
        }
    }
    
    @Override
    public void openChannel (DataAccessOption... options) throws IOException {
        
        for (DataAccessOption option: options) {
            // In the future we may need to be able to open read-write 
            // Channels; no support, or use case for that as of now. 
            
            if (option == DataAccessOption.READ_ACCESS) {
                isReadAccess = true;
                continue;
            }

            if (option == DataAccessOption.WRITE_ACCESS) {
                isWriteAccess = true;
                continue;
            }
        }
        
        if (!isReadAccess && !isWriteAccess) {
            isReadAccess = true; 
        }
        
        if (isReadAccess) {
            // TODO: 
            // Make sure all the tasks performed by the old-style, InputStream-based
            // method are still taken care of. 
            openReadableChannel(); 
        } else if (isWriteAccess) {
            openWritableChannel();
        }
        
    }
    
    private void openWritableChannel () throws IOException {
        DataFile datafile = this.getFile();
        
        if (datafile == null) {
            throw new IOException ("Data Access: No Datafile defined in the DataAccessObject.");
        }
                
        channel = new FileOutputStream(getFileSystemPath().toFile()).getChannel();
  
    }
    
    private void openReadableChannel () throws IOException {
        DataFile datafile = this.getFile();
        
        if (datafile == null) {
            throw new IOException ("Data Access: No Datafile defined in the DataAccessObject.");
        }
                
        channel = openLocalFileAsStream(datafile).getChannel();
    }
     
    // Auxilary helper methods, filesystem access-specific:
    
    public long getLocalFileSize (DataFile file) {
        long fileSize = -1;
        File testFile = null;

        try {
            testFile = file.getFileSystemLocation().toFile();
            if (testFile != null) {
                fileSize = testFile.length();
            }
        } catch (Exception ex) {
            return -1;
        }

        return fileSize;
    }

    public FileInputStream openLocalFileAsStream (DataFile file) {
        FileInputStream in;

        try {
            in = new FileInputStream(file.getFileSystemLocation().toFile());
        } catch (Exception ex) {
            // We don't particularly care what the reason why we have
            // failed to access the file was.
            // From the point of view of the download subsystem, it's a
            // binary operation -- it's either successfull or not.
            // If we can't access it for whatever reason, we are saying
            // it's 404 NOT FOUND in our HTTP response.
            return null;
        }

        return in;
    }
    
    private String generateVariableHeader(List dvs) {
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