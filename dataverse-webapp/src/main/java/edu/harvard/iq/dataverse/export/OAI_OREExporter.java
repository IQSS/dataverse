package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import org.apache.commons.lang.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ResourceBundle;
import java.util.logging.Logger;

@ApplicationScoped
public class OAI_OREExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;

    private Clock clock = Clock.systemUTC();

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public OAI_OREExporter(SettingsServiceBean settingsService, SystemConfig systemConfig) {
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
    }

    public OAI_OREExporter(SettingsServiceBean settingsService, SystemConfig systemConfig, Clock clock) {
        this(settingsService, systemConfig);
        this.clock = clock;
    }
    // -------------------- LOGIC --------------------

    @Override
    public String exportDataset(DatasetVersion version) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            new OREMap(version,
                        settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport),
                        systemConfig.getDataverseSiteUrl(), clock)
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
        return ExporterType.OAIORE.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.OAIORE;
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
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
