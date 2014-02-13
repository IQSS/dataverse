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
 * @author landreev
 */

public class DataAccess {
    public DataAccess() {

    }

    public static DataAccessObject createDataAccessObject (DataFile df) throws IOException {
        return createDataAccessObject (df, null);
    }

    public static DataAccessObject createDataAccessObject (DataFile df, DataAccessRequest req) throws IOException {

        if (df == null ||
                df.getFileSystemLocation() == null ||
                df.getFileSystemLocation().equals("")) {
            throw new IOException ("createDataAccessObject: null or invalid study file.");
        }

        /*if (!sf.isRemote()) {*/
            return new FileAccessObject (df, req);
        /*} else if (sf.getFileSystemLocation().matches(".*census\\.gov.*")) {
                return new CensusAccessObject (sf, req);
        }

        return new HttpAccessObject (sf, req);
                */
    }
}