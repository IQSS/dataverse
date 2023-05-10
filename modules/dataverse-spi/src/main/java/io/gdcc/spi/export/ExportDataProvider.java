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

    JsonObject getDatasetJson();

    JsonObject getDatasetSchemaDotOrg();

    JsonObject getDatasetORE();

    JsonArray getDatasetFileDetails();

    String getDataCiteXml();

    default Optional<InputStream> getPrerequisiteInputStream() {
        return Optional.empty();
    }

}
