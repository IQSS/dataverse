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
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Iterator; 


// dataverse imports:
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import javax.ejb.EJB;

public class FileStoreObject extends DataStoreObject {
    @EJB 
    DatasetServiceBean datasetService;

    public FileStoreObject () throws IOException {
        this(null);
    }

    public FileStoreObject(DataFile file) throws IOException {

        super(file);


        this.setIsLocalFile(true);
        this.setIsNIOSupported(true);
    }

    public boolean canWrite (String location) throws IOException{
        return true;
    }

    
    public void open () throws IOException {

        DataFile file = this.getFile();
        OutputStream out = openLocalFileAsStream(file);

        if (out == null) {
            throw new IOException ("Failed to open local file for writing"+file.getFileSystemLocation());
        }

        this.setOutputStream(out);

        this.setMimeType(file.getContentType());

        this.setStatus(200);
    } // End of initiateLocalDownload;

    // Auxilary helper methods, filesystem access-specific:


    public OutputStream openLocalFileAsStream (DataFile datafile) {
        OutputStream out = null;

        try {
            if (datafile.getFileSystemName() != null) {
                out = new FileOutputStream(datafile.getFileSystemLocation().toFile());
            } else {
                // if the data file has no physical filename yet, we 
                // need to generate it: 
                datasetService.generateFileSystemName(datafile);
            }
        } catch (Exception ex) {
            this.setErrorMessage(ex.getMessage());
            return null;
        }

        return out;
    }
    
}