package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class OpenAireExporter implements Exporter {

    private SettingsServiceBean settingsService;
    private JsonPrinter jsonPrinter;

    // -------------------- CONSTRUCTORS --------------------

    public OpenAireExporter(SettingsServiceBean settingsService, JsonPrinter jsonPrinter) {
        this.settingsService = settingsService;
        this.jsonPrinter = jsonPrinter;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.OPENAIRE.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.OPENAIRE;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dataciteOpenAIRE");
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            JsonObject datasetAsJson = jsonPrinter.jsonAsDatasetDto(version, settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport))
                    .build();

            OpenAireExportUtil.datasetJson2openaire(datasetAsJson, byteArrayOutputStream);
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | IOException xse) {
            throw new ExportException("Caught XMLStreamException performing DataCite OpenAIRE export", xse);
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
        return true;
    }

    @Override
    public String getXMLNameSpace() {
        return OpenAireExportUtil.RESOURCE_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return OpenAireExportUtil.RESOURCE_SCHEMA_LOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return OpenAireExportUtil.SCHEMA_VERSION;
    }
}