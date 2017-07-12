/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import java.io.IOException;

/**
 *
 * @author rohitb
 */
public class StorageIO {
    
    public StorageIO(){
        
    }
    
    public static DataFileIO getDvObject(DvObject dvObject, DataAccessRequest req) throws IOException    {
        
     
        if(dvObject == null) {
            throw new IOException("Invalid Dataverse Object");
        }

        if(dvObject.isInstanceofDataFile())
        {
            DataFileIO dataFileIO;
              DataFile df= (DataFile) dvObject;
              
              if (df.getStorageIdentifier().startsWith("file://")
                || (!df.getStorageIdentifier().matches("^[a-z][a-z]*://.*"))) {
            return new FileAccessIO (df, req);
        } else if (df.getStorageIdentifier().startsWith("swift://")){
            return new SwiftAccessIO(df, req);
        } else if (df.getStorageIdentifier().startsWith("tmp://")) {
            throw new IOException("DataAccess IO attempted on a temporary file that hasn't been permanently saved yet.");
        }
              
            
            
        }
        else if(dvObject.isInstanceofDataset())
        {
            Dataset ds= (Dataset) dvObject;
            
            
        }
        
        
         throw new IOException("getDataAccessObject: Unsupported storage method.");
                
    }
    
    
    
    
    
//    public StorageIO()
//    private DvObject dvObject;
    
}
