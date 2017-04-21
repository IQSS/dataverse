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

/**
 *
 * @author Leonid Andreev
 */

public class DataAccess {
    public DataAccess() {

    }

    public static DataFileIO createDataAccessObject (DataFile df) throws IOException {
        return createDataAccessObject (df, null);
    }

    public static DataFileIO createDataAccessObject (DataFile df, DataAccessRequest req) throws IOException {

        if (df == null ||
                df.getStorageIdentifier() == null ||
                df.getStorageIdentifier().equals("")) {
            throw new IOException ("createDataAccessObject: null or invalid datafile.");
        }

        if (df.getStorageIdentifier().startsWith("file://")
                || (!df.getStorageIdentifier().matches("^[a-z][a-z]*://.*"))) {
            return new FileAccessIO (df, req);
        }
        
        // No other storage methods are supported as of now! -- 4.0.1
        // TODO: 
        // This code will need to be extended with a system of looking up 
        // available storage plugins by the storage tag embedded in the 
        // "storage identifier". 
        // -- L.A. 4.0.2
        
        throw new IOException ("createDataAccessObject: Unsupported storage method.");
    }
}