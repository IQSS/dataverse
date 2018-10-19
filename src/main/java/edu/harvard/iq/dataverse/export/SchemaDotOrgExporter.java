package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@AutoService(Exporter.class)
public class SchemaDotOrgExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(SchemaDotOrgExporter.class.getCanonicalName());

    public static final String NAME = "schema.org";

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        String jsonLdAsString = version.getJsonLd();
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonLdAsString));) {
            JsonObject jsonLdJsonObject = jsonReader.readObject();
            try {
                outputStream.write(jsonLdJsonObject.toString().getBytes("UTF8"));
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.write: " + ex);
            }
            try {
                outputStream.flush();
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.flush: " + ex);
            }
        }
    }

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.schemaDotOrg");
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        // Defer harvesting because the current effort was estimated as a "2": https://github.com/IQSS/dataverse/issues/3700
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }

}
