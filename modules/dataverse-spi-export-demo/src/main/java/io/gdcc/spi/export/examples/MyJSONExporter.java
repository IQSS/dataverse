
package io.gdcc.spi.export.examples;

import com.google.auto.service.AutoService;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import java.io.OutputStream;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author qqmyers
 */
@AutoService(Exporter.class)
public class MyJSONExporter implements Exporter {

    @Override
    public String getFormatName() {
        return "dataverse_json";
    }

    @Override
    public String getDisplayName(Locale locale) {
        return "My JSON in " + locale.getLanguage();
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream)
            throws ExportException {
        try {
            JsonObjectBuilder datasetJsonBuilder = Json.createObjectBuilder();
            datasetJsonBuilder.add("name", getFormatName());
            datasetJsonBuilder.add("inputJson", dataProvider.getDatasetJson());
            outputStream.write(datasetJsonBuilder.build().toString().getBytes("UTF8"));
            outputStream.flush();
        } catch (Exception e) {
            throw new ExportException("Unknown exception caught during JSON export.");
        }
    }

    @Override
    public Boolean isHarvestable() {
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
