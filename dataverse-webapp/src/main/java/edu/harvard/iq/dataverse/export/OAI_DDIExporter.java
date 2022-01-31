package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.ddi.DdiConstants;
import edu.harvard.iq.dataverse.export.ddi.DdiDatasetExportService;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
public class OAI_DDIExporter extends ExporterBase {

    private DdiDatasetExportService ddiDatasetExportService;
    private VocabularyValuesIndexer vocabularyValuesIndexer;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public OAI_DDIExporter(DdiDatasetExportService ddiDatasetExportService, SettingsServiceBean settingsService,
                           VocabularyValuesIndexer vocabularyValuesIndexer, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
        this.ddiDatasetExportService = ddiDatasetExportService;
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
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Map<String, Map<String, String>> localizedValuesIndex
                    = vocabularyValuesIndexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(version, Locale.ENGLISH);

            ddiDatasetExportService.datasetJson2ddi(createDTO(version), byteArrayOutputStream, localizedValuesIndex);
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
