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
 * This exporter is for the "full" DDI, that includes the file-level,
 * <data> and <var> metadata.
 *
 * @author Leonid Andreev
 * (based on the original DDIExporter by
 * @author skraffmi
 * - renamed OAI_DDIExporter)
 */
@ApplicationScoped
public class DDIExporter extends ExporterBase {

    private DdiDatasetExportService ddiDatasetExportService;
    private VocabularyValuesIndexer vocabularyValuesIndexer;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    DDIExporter(DdiDatasetExportService ddiDatasetExportService, SettingsServiceBean settingsService,
                VocabularyValuesIndexer vocabularyValuesIndexer, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
        this.ddiDatasetExportService = ddiDatasetExportService;
        this.vocabularyValuesIndexer = vocabularyValuesIndexer;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.DDI.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.DDI;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi")
                : "DDI";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Map<String, Map<String, String>> localizedVocabularyIndex
                    = vocabularyValuesIndexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(version, Locale.ENGLISH);
            ddiDatasetExportService.datasetJson2ddi(createDTO(version), version, byteArrayOutputStream, localizedVocabularyIndex);
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
        // No, we don't want this format to be harvested!
        // For datasets with tabular data the <data> portions of the DDIs
        // become huge and expensive to parse; even as they don't contain any
        // metadata useful to remote harvesters. -- L.A. 4.5
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
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

