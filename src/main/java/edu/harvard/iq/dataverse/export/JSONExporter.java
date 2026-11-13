
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import io.gdcc.spi.export.caps.bulk.BulkDatasetContext;
import io.gdcc.spi.export.caps.bulk.BulkDatasetExporter;
import jakarta.ws.rs.core.MediaType;


/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class JSONExporter implements Exporter, BulkDatasetExporter {

    @Override
    public String getFormatName() {
        return "dataverse_json";
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json", locale); 
        return Optional.ofNullable(displayName).orElse("JSON");
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        try{
            outputStream.write(dataProvider.getDatasetJson().toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception e){
            throw new ExportException("Unknown exception caught during JSON export.");
        }
    }

    @Override
    public Boolean isHarvestable() {
        return true;
    }
    
    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
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
            throw new ExportException("Writing JSON bulk export failed", e);
        }
    }
}
