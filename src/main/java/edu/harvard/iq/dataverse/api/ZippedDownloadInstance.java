/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Leonid Andreev
 */
public class ZippedDownloadInstance {
   
    private String Manifest = "";
    private long sizeLimit = -1; 

    private List<DataFile> dataFiles = null; 
    
    public ZippedDownloadInstance() {
        dataFiles = new ArrayList<>();
    }
    
    public List<DataFile> getDataFiles() {
        return dataFiles; 
    }
    
    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles; 
    }
    
    public void addDataFile(DataFile dataFile) {
        dataFiles.add(dataFile);
    }
    
    public String getManifest() {
        return Manifest;
    }
    
    public void setManifest(String Manifest) {
        this.Manifest = Manifest;
    }
    
    public long getSizeLimit() {
        return sizeLimit; 
    }
    
    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit; 
    }
    
}
