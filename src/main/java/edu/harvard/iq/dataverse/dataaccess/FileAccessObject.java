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

    public boolean canAccess (String location) throws IOException{
        return true;
    }

    //public void open (String location) throws IOException{

    //}

    //private void open (DataFile file, Object req) throws IOException {
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
        this.setFileName(file.getName());

        
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

    public InputStream openLocalFileAsStream (DataFile file) {
        InputStream in;

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