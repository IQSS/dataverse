/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.spi;

import javax.json.JsonObject;

import edu.harvard.iq.dataverse.DataCitation;

/**
 * Provides all data necessary to create an export
 * 
 */
public interface ExportDataProviderInterface {
    
    public JsonObject getDatasetJson();
    
    public JsonObject getDatasetSchemaDotOrg();
    
    public JsonObject getDatasetORE();

    public String getDataCiteXml();
}
