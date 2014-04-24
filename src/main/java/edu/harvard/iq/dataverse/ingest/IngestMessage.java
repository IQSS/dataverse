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

import edu.harvard.iq.dataverse.DataFile;
import java.io.Serializable;
import java.io.File; 
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
    public static final int INGEST_MESAGE_LEVEL_ERROR = 1; 
    public static final int INGEST_MESAGE_LEVEL_INFO = 2;

    /** Creates a new instance of IngestMessage */
    public IngestMessage()  {
        this(INGEST_MESAGE_LEVEL_INFO);
    }

    public IngestMessage(int messageLevel)  {
        this.messageLevel = messageLevel;
        dataFiles = new ArrayList<DataFile>();
    }
   
    private int messageLevel = INGEST_MESAGE_LEVEL_INFO;
    
    private Long datasetId;
    private Long datasetVersionId;
    private String versionNote;
    private String datasetVersionNumber;
    private List<DataFile> dataFiles; 

    public String getVersionNote() {
        return versionNote;
    }

    public void setVersionNote(String versionNote) {
        this.versionNote = versionNote;
    }

    public int getMessageLevel() {
        return messageLevel;
    }

    public void setMessageLevel(int messageLevel) {
        this.messageLevel = messageLevel;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    public boolean sendInfoMessage() {
        return messageLevel >= INGEST_MESAGE_LEVEL_INFO;
    }

    public boolean sendErrorMessage() {
        return messageLevel >= INGEST_MESAGE_LEVEL_ERROR;
    }

    public String getDatasetVersionNumber() {
        return datasetVersionNumber;
    }

    public void setDatasetVersionNumber(String datasetVersionNumber) {
        this.datasetVersionNumber = datasetVersionNumber;
    }
    
    public List<DataFile> getFiles() {
        return dataFiles; 
    }
    
    public void setFiles(List<DataFile> files) {
        dataFiles = files;
    }
    
    public void addFile(DataFile file) {
        dataFiles.add(file);
    }
    
}
