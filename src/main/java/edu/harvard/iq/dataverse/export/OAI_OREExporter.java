package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.util.bagit.OREMap_Export;
import java.io.OutputStream;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.JsonObject;

@AutoService(Exporter.class)
public class OAI_OREExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

    public static final String NAME = "OAI_ORE";

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream)
            throws ExportException {
        try {
            new OREMap_Export(version).exportOREMap(outputStream);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.oai_ore") != null
                ? ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.oai_ore")
                : "OAI_ORE";
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        // Defer harvesting because the current effort was estimated as a "2":
        // https://github.com/IQSS/dataverse/issues/3700
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException(OAI_OREExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException(OAI_OREExporter.class.getSimpleName() + ": not an XML format.");
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
