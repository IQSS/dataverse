/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.spi;

import edu.harvard.iq.dataverse.export.ExportException;
import java.io.OutputStream;
import javax.json.JsonObject;

/**
 *
 * @author skraffmi
 */
public interface Exporter {
    
    public void exportDataset(JsonObject json, OutputStream outputStream) throws ExportException;
    
    public String getProvider();
    
    public String getDisplayName();
    
    public Boolean isXMLFormat();
    
}
