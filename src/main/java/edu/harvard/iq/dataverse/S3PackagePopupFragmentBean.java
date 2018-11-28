/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//MAD: This and s3importer should probably be in their own package
package edu.harvard.iq.dataverse;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author matthew
 */

@ViewScoped
@Named
public class S3PackagePopupFragmentBean implements java.io.Serializable {
    
    FileMetadata fm;
    
    public void setFileMetadata(FileMetadata fileMetadata) {
        fm = fileMetadata;
    }
    
    public FileMetadata getFileMetadata() {
        return fm;
    }
    
//    public String provideS3OneTimeUrl() throws IOException {
//        if(df != null ) {
//            StorageIO<DataFile> storageIO = DataAccess.getStorageIO(df);
//            return ((S3AccessIO)storageIO).generateTemporaryS3Url();
//        } else {
//            return "";
//        }
//
//    }
}
 