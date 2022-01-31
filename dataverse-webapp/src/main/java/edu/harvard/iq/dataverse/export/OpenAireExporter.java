package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class OpenAireExporter extends ExporterBase {

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public OpenAireExporter(SettingsServiceBean settingsService, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
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
            OpenAireExportUtil.datasetJson2openaire(createDTO(version), byteArrayOutputStream);
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