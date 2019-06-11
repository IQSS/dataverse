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
package edu.harvard.iq.dataverse.ingest.metadataextraction;

import java.io.File;
import edu.harvard.iq.dataverse.DataTable;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

/**
 * An object that stores the metadata that was extracted
 * from a file during ingest.
 *
 *
 */
public class FileMetadataIngest {
    private Set<String> fileTypes;
    
    private String metadataBlockName; 

    private Map<String, Set<String>>  metadataMap; 
    
    private String metadataSummary; 
    
    public FileMetadataIngest() {
        fileTypes = new HashSet<String>();
    }

    public Set<String> getFileTypes() {
        return fileTypes;
    }
    
    public void setFileTypes(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
    }
    
    public String getMetadataBlockName() {
        return metadataBlockName; 
    }
    
    public void setMetadataBlockName(String metadataBlockName) {
        this.metadataBlockName = metadataBlockName;
    }
    
    public Map<String, Set<String>> getMetadataMap() {
        return metadataMap; 
    }
    
    public void setMetadataMap(Map<String, Set<String>> metadataMap) {
        this.metadataMap = metadataMap;
    }
    
    public String getMetadataSummary() {
        return metadataSummary; 
    }
    
    public void setMetadataSummary(String metadataSummary) {
        this.metadataSummary = metadataSummary;
    }
    
}
