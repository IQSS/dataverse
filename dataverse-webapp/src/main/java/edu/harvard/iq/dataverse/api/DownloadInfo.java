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
package edu.harvard.iq.dataverse.api;


import java.io.File; 
import java.util.List;
import java.util.ArrayList;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;


/**
 *
 * @author Leonid Andreev
 */
public class DownloadInfo {
    
    private DataFile dataFile; 
    //private String mimeType; 
    
    private String authUserName = "";
    private String authMethod = "anonymous"; 
    
    Boolean accessGranted = false; 
    
    Boolean accessPermissionsApply = false; 
    Boolean accessRestrictionsApply = false;
    Boolean passAccessPermissions = false; 
    Boolean passAccessRestrictions = false; 

    private List<OptionalAccessService> optionalServicesAvailable; 
    
    public DownloadInfo(DataFile sf) {
        dataFile = sf;
        optionalServicesAvailable = new ArrayList<OptionalAccessService>();       
    }

    public DataFile getDataFile() {
        return dataFile; 
    }
     
    public void setDataFile (DataFile sf) {
        dataFile = sf; 
    }
    
    public String getAuthUserName() {
        return authUserName; 
    }
    
    public void setAuthUserName(String un) {
        authUserName = un; 
    }
    
    public String getAuthMethod() {
        return authMethod; 
    }
    
    public void setAuthMethod(String am) {
        authMethod = am; 
    }
    
    public Boolean isPassAccessPermissions() {
        return passAccessPermissions;
    }
    
    public void setPassAccessPermissions(Boolean pass) {
        passAccessPermissions = pass; 
    }
    
    public Boolean isPassAccessRestrictions() {
        return passAccessRestrictions;
    }
    
    public void setPassAccessRestrictions(Boolean pass) {
        passAccessRestrictions = pass; 
    }
    
    public Boolean isAccessPermissionsApply() {
        return accessPermissionsApply;
    }
    
    public void setAccessPermissionsApply(Boolean pass) {
        accessPermissionsApply = pass; 
    }
    
    public Boolean isAccessRestrictionsApply() {
        return accessRestrictionsApply;
    }
    
    public void setAccessRestrictionsAply(Boolean pass) {
        accessRestrictionsApply = pass; 
    }
    
    public Boolean isAccessGranted() {
        return (passAccessPermissions && passAccessRestrictions); 
    }
    
    public String getMimeType() {
        String mType = null; 
        
        if (dataFile != null) {
            mType = dataFile.getContentType();
        }
        
        return mType; 
    }
    
    public Long getDataFileId() {
        Long sfId = null; 
        
        if (dataFile != null) {
            sfId = dataFile.getId(); 
        }
        
        return sfId; 
    }
    
    public String getFileName() {
        if (dataFile != null) {
            if (dataFile.getFileMetadata() != null) {
                return dataFile.getFileMetadata().getLabel();
            }
        }
        
        return null; 
    }
    
    public List<OptionalAccessService> getServicesAvailable() {
	return optionalServicesAvailable; 
    }
    
    public void addServiceAvailable(OptionalAccessService accessService) {
        this.optionalServicesAvailable.add(accessService);
    }
    
}