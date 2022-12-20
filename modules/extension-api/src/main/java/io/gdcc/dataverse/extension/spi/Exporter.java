/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.gdcc.dataverse.extension.spi;

import io.gdcc.dataverse.extension.exceptions.ExportException;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

public interface Exporter {
    
    void exportDataset(JsonObject json, OutputStream outputStream) throws ExportException;
    
    String getProviderName();
    
    String getDisplayName();
    
    Boolean isXMLFormat();
    
    Boolean isHarvestable();
    
    Boolean isAvailableToUsers();
    
    /* These should throw an ExportException if called on an Exporter that is not isXMLFormat(): */
    String getXMLNameSpace() throws ExportException;
    
    String getXMLSchemaLocation() throws ExportException;
    
    String getXMLSchemaVersion() throws ExportException;
    
    void setParam(String name, Object value);
    
    default String getMediaType() {
        return MediaType.APPLICATION_XML;
    }
    
}
