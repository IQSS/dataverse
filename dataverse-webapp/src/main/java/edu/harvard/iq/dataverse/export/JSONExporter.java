package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import org.apache.commons.lang.StringUtils;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;



public class JSONExporter implements Exporter {

    private boolean excludeEmailFromExport;

    // -------------------- CONSTRUCTORS --------------------

    public JSONExporter(boolean excludeEmailFromExport) {
        this.excludeEmailFromExport = excludeEmailFromExport;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.JSON.toString();
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") : "JSON";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try {
            JsonObjectBuilder jsonObjectBuilder = JsonPrinter.jsonAsDatasetDto(version, excludeEmailFromExport);

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
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
