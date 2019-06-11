/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author matthew
 */

@ViewScoped
@Named
public class PackagePopupFragmentBean implements java.io.Serializable {
    
    FileMetadata fm;
    
    public void setFileMetadata(FileMetadata fileMetadata) {
        fm = fileMetadata;
    }
    
    public FileMetadata getFileMetadata() {
        return fm;
    }
    
}
 