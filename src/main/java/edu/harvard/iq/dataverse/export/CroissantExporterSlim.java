package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.croissant.CroissantExportUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import jakarta.ws.rs.core.MediaType;
import java.io.OutputStream;
import java.util.Locale;

/**
 * See CroissantExporter for more about about Croissant. This "slim" version is purposefully small
 * for embedding into the "head" of HTML pages.
 */
@AutoService(Exporter.class)
public class CroissantExporterSlim implements Exporter {

    /*
     * The name of the format it creates. If this format is already provided by a
     * built-in exporter, this Exporter will override the built-in one. (Note that
     * exports are cached, so existing metadata export files are not updated
     * immediately.)
     */
    @Override
    public String getFormatName() {
        return "croissantSlim";
    }

    /**
     * The display name shown in the UI
     *
     * @param locale
     */
    @Override
    public String getDisplayName(Locale locale) {
        // This example includes the language in the name to demonstrate that locale is
        // available. A production exporter would instead use the locale to generate an
        // appropriate translation.
        return "Croissant Slim";
    }

    /** Whether the exported format should be available as an option for Harvesting */
    @Override
    public Boolean isHarvestable() {
        return false;
    }

    /** Whether the exported format should be available for download in the UI and API */
    @Override
    public Boolean isAvailableToUsers() {
        return false;
    }

    /**
     * Defines the mime type of the exported format - used when metadata is downloaded, i.e. to
     * trigger an appropriate viewer in the user's browser.
     */
    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

    /**
     * This method is called by Dataverse when metadata for a given dataset in this format is
     * requested.
     */
    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream)
            throws ExportException {
        CroissantExportUtil.exportDataset(dataProvider, outputStream, true);
    }
}
