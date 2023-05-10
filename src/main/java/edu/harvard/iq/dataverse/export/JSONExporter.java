
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;


/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class JSONExporter implements Exporter {

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
            outputStream.write(dataProvider.getDatasetJson().toString().getBytes("UTF8"));
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
    
}
