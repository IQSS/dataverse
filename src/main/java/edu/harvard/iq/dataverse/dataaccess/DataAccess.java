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
import java.io.IOException;
import org.javaswift.joss.model.StoredObject;

/**
 *
 * @author Leonid Andreev
 */

public class DataAccess {
    public DataAccess() {

    }

    // set by the user in glassfish-setup.sh if DEFFAULT_STORAGE_DRIVER_IDENTIFIER = swift
    public static final String DEFAULT_STORAGE_DRIVER_IDENTIFIER = System.getProperty("dataverse.files.storage-driver-id");
    
    // The getDataFileIO() methods initialize DataFileIO objects for
    // datafiles that are already saved using one of the supported Dataverse
    // DataAccess IO drivers.
    public static DataFileIO getDataFileIO(DataFile df) throws IOException {
        return getDataFileIO(df, null);
    }

    public static DataFileIO getDataFileIO(DataFile df, DataAccessRequest req) throws IOException {

        if (df == null
                || df.getStorageIdentifier() == null
                || df.getStorageIdentifier().equals("")) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }

        if (df.getStorageIdentifier().startsWith("file://")
                || (!df.getStorageIdentifier().matches("^[a-z][a-z]*://.*"))) {
            return new FileAccessIO (df, req);
        } else if (df.getStorageIdentifier().startsWith("swift://")){
            return new SwiftAccessIO(df, req);
        } else if (df.getStorageIdentifier().startsWith("tmp://")) {
            throw new IOException("DataAccess IO attempted on a temporary file that hasn't been permanently saved yet.");
        }
        
        // No other storage methods are supported as of now! -- 4.0.1
        // TODO: 
        // This code will need to be extended with a system of looking up 
        // available storage plugins by the storage tag embedded in the 
        // "storage identifier". 
        // -- L.A. 4.0.2
        
        throw new IOException("getDataAccessObject: Unsupported storage method.");
    }

    // createDataAccessObject() methods create a *new*, empty DataAccess objects,
    // for saving new, not yet saved datafiles.
    public static DataFileIO createNewDataFileIO(DataFile df, String storageTag) throws IOException {

        return createNewDataFileIO(df, storageTag, DEFAULT_STORAGE_DRIVER_IDENTIFIER);
    }

    public static DataFileIO createNewDataFileIO(DataFile df, String storageTag, String driverIdentifier) throws IOException {
        if (df == null
                || storageTag == null
                || storageTag.equals("")) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }

        DataFileIO dataFileIO = null;

        df.setStorageIdentifier(storageTag);

        if (driverIdentifier == null) {
            driverIdentifier = "file";
        }

        if (driverIdentifier.equals("file")) {
            dataFileIO = new FileAccessIO(df, null);
        } else if (driverIdentifier.equals("swift")) {
            dataFileIO = new SwiftAccessIO(df, null);
        } else {
            throw new IOException("createDataAccessObject: Unsupported storage method " + driverIdentifier);
        }

        dataFileIO.open(DataAccessOption.WRITE_ACCESS);
        return dataFileIO;
    }
    

}
