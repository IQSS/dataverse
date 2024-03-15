package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;

@AutoService(Exporter.class)
public class OAI_OREExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

    public static final String NAME = "OAI_ORE";

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream)
            throws ExportException {
        try {
            outputStream.write(dataProvider.getDatasetORE().toString().getBytes("UTF8"));
            outputStream.flush();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public String getFormatName() {
        return NAME;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.oai_ore", locale);
        return Optional.ofNullable(displayName).orElse("OAI_ORE");
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
