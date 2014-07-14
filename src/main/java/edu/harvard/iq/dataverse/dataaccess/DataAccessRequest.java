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
import java.util.Map;
import java.util.HashMap;


// DVN App imports:
import edu.harvard.iq.dataverse.DataFile;

/**
 *
 * @author Leonid Aandreev
 */

public class DataAccessRequest {
    
    public DataAccessRequest () {
        this(null);
    }

    public DataAccessRequest (DataFile file) {
        this.file = file;
        this.requestParameters = new HashMap<String, String>();
    }

    private DataFile file;
    private Map<String, String> requestParameters;

    public void setFile (DataFile file) {
        this.file = file;
    }

    public DataFile getFile () {
        return this.file; 
    }

    public void setParameter (String name, String value) {
        if (requestParameters != null) {
            requestParameters.put(name, value);
        }
    }

    public String getParameter (String name) {
        if (requestParameters != null) {
            return requestParameters.get(name);
        }
        return null; 
    }

    private DataAccessRequest(Object object) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
