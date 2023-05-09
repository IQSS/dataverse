/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.gdcc.export.spi;

import java.io.InputStream;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Provides all data necessary to create an export
 * 
 */
public interface ExportDataProviderInterface {

    public JsonObject getDatasetJson();

    public JsonObject getDatasetSchemaDotOrg();

    public JsonObject getDatasetORE();

    public JsonArray getDatasetFileDetails();

    public String getDataCiteXml();

    public default InputStream getPrerequisiteInputStream() {
        return null;
    }

}
