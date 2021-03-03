package edu.harvard.iq.dataverse.export;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.ddi.DdiConstants;
import edu.harvard.iq.dataverse.export.ddi.DdiDatasetExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;


/**
 * This exporter is for the OAI ("short") flavor of the DDI -
 * that is, without the variable/data information. The ddi export
 * utility does not need the version entity to produce that.
 */
@ApplicationScoped
public class OAI_DDIExporter implements Exporter {

    private DdiDatasetExportService ddiDatasetExportService;
    private SettingsServiceBean settingsService;
    private VocabularyValuesIndexer vocabularyValuesIndexer;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public OAI_DDIExporter(DdiDatasetExportService ddiDatasetExportService, SettingsServiceBean settingsService, VocabularyValuesIndexer vocabularyValuesIndexer) {
        this.ddiDatasetExportService = ddiDatasetExportService;
        this.settingsService = settingsService;
        this.vocabularyValuesIndexer = vocabularyValuesIndexer;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.OAIDDI.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.OAIDDI;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            JsonObject datasetAsJson = JsonPrinter.jsonAsDatasetDto(version, settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport))
                    .build();
            Map<String, Map<String, String>> localizedValuesIndex
                    = vocabularyValuesIndexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(version, Locale.ENGLISH);
            
            Gson gson = new Gson();
            DatasetDTO datasetDto = gson.fromJson(datasetAsJson.toString(), DatasetDTO.class);
            
            ddiDatasetExportService.datasetJson2ddi(datasetDto, byteArrayOutputStream, localizedValuesIndex);
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | IOException xse) {
            throw new ExportException("Caught XMLStreamException performing DDI export");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
    }

    @Override
    public Boolean isHarvestable() {
        return true;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return false;
    }

    @Override
    public String getXMLNameSpace() {
        return DdiConstants.DDI_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return DdiConstants.DDI_SCHEMA_LOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DdiConstants.DDI_VERSION;
    }
}
