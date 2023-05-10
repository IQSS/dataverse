package io.gdcc.spi.export;

import java.io.InputStream;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Provides all data necessary to create an export
 * 
 */
public interface ExportDataProvider {

    public JsonObject getDatasetJson();

    public JsonObject getDatasetSchemaDotOrg();

    public JsonObject getDatasetORE();

    public JsonArray getDatasetFileDetails();

    public String getDataCiteXml();

    public default Optional<InputStream> getPrerequisiteInputStream() {
        return Optional.empty();
    }

}
