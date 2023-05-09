package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.spi.ExportDataProviderInterface;
import edu.harvard.iq.dataverse.export.spi.ExportException;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import java.io.OutputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;

@AutoService(Exporter.class)
public class OAI_OREExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

    public static final String NAME = "OAI_ORE";

    @Override
    public void exportDataset(ExportDataProviderInterface dataProvider, OutputStream outputStream)
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
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.oai_ore", locale);
        return displayName != null ? displayName : "OAI_ORE";
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
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
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
