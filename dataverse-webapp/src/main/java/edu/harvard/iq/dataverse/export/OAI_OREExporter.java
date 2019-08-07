package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Logger;


public class OAI_OREExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

    private boolean excludeEmailFromExport;
    private String dataverseSiteUrl;
    private LocalDate modificationDate;

    // -------------------- CONSTRUCTORS --------------------

    public OAI_OREExporter(boolean excludeEmailFromExport, String dataverseSiteUrl, LocalDate modificationDate) {
        this.excludeEmailFromExport = excludeEmailFromExport;
        this.dataverseSiteUrl = dataverseSiteUrl;
        this.modificationDate = modificationDate;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String exportDataset(DatasetVersion version) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            new OREMap(version, excludeEmailFromExport, dataverseSiteUrl, modificationDate)
                    .writeOREMap(byteArrayOutputStream);

            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getProviderName() {
        return ExporterType.OAIORE.toString();
    }

    @Override
    public String getDisplayName() {
        return ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.oai_ore");
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
