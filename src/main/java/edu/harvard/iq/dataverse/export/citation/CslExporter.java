package edu.harvard.iq.dataverse.export.citation;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.caps.bulk.BulkDatasetContext;
import io.gdcc.spi.export.caps.bulk.BulkDatasetExporter;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Logger;

@AutoService(Exporter.class)
public class CslExporter implements Exporter, BulkDatasetExporter {
    
    Logger logger = Logger.getLogger(CslExporter.class.getName());
    
    @Override
    public void exportDataset(ExportDataProvider exportDataProvider, OutputStream outputStream) throws ExportException {
        /*
         * The idea here: grab the serialized DataCitation snapshot from the Dataverse JSON format,
         * then deserialize and let the existing code handle formatting. No duplication of citation formatting,
         * just a step forward onto removing DataCitation from the codebase.
         * In addition, this exporter can be overloaded by a plugin, allowing manipulation of the formatting per instance.
         * See also DataCitation class for the transformation plan details.
         */
        try (JsonWriter writer = JsonUtil.createWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            Jsonb jsonb = JsonbBuilder.create()) {
            JsonObject json = exportDataProvider.getDatasetJson(DatasetExportQuery.defaults());
            
            String citationJson = json.get("datasetVersion").asJsonObject().getString("datacitation");
            DataCitation dataCitation = jsonb.fromJson(citationJson, DataCitation.class);
            
            writer.write(dataCitation.getCSLJsonFormat());
        } catch (Exception e) {
            throw new ExportException(e.getMessage(), e);
        }
    }
    
    @Override
    public String getFormatName() {
        return DataCitation.Format.CSL.formatId();
    }
    
    @Override
    public String getDisplayName(Locale locale) {
        return "Citation Style Language";
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
        return "application/vnd.citationstyles.csl+json";
    }
    
    @Override
    public void exportBulk(BulkDatasetContext bulkDatasetContext, OutputStream outputStream) throws ExportException {
        try {
            outputStream.write("[".getBytes(StandardCharsets.UTF_8));
            int count = 1;
            for (BulkDatasetContext.Item item : bulkDatasetContext.items()) {
                item.writeTo(outputStream);
                if (count < bulkDatasetContext.size()) {
                    outputStream.write(",".getBytes(StandardCharsets.UTF_8));
                }
                outputStream.flush();
                count++;
            }
            outputStream.write("]".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExportException("Writing CSL JSON bulk export failed", e);
        }
    }
}
