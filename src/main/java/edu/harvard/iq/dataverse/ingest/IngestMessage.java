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
package edu.harvard.iq.dataverse.ingest;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * This is an experimental, JMS-based implementation of asynchronous 
 * ingest. (experimental is the key! it may go away!)
 * 
 * @author Leonid Andreev
 */
public class IngestMessage implements Serializable {
    /** Creates a new instance of IngestMessage */

    public IngestMessage()  {
        datafile_ids = new ArrayList<Long>();
    }

    public IngestMessage(Long authenticatedUserId) {
        this.authenticatedUserId = authenticatedUserId;
        datafile_ids = new ArrayList<Long>();
    }
    
    private Long datasetId;
    private List<Long> datafile_ids;
    private Long authenticatedUserId;
    private String info;

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }
    
    public List<Long> getFileIds() {
        return datafile_ids; 
    }
    
    public void setFileIds(List<Long> file_ids) {
        datafile_ids = file_ids;
    }
    
    public void addFileId(Long file_id) {
        datafile_ids.add(file_id);
    }

    public Long getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }
}
