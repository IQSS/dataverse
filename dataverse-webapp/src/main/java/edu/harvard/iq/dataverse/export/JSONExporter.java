package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import org.apache.commons.lang.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;



@ApplicationScoped
public class JSONExporter implements Exporter {

    private SettingsServiceBean settingsService;
    private JsonPrinter jsonPrinter;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public JSONExporter(SettingsServiceBean settingsService, JsonPrinter jsonPrinter) {
        this.settingsService = settingsService;
        this.jsonPrinter = jsonPrinter;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.JSON.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.JSON;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") : "JSON";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try {
            JsonObjectBuilder jsonObjectBuilder = jsonPrinter.jsonAsDatasetDto(version, settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport));

            return jsonObjectBuilder
                    .build()
                    .toString();

        } catch (Exception e) {
            throw new ExportException("Unknown exception caught during JSON export.", e);
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
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
    public String getXMLNameSpace() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getXMLSchemaLocation() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getXMLSchemaVersion() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
