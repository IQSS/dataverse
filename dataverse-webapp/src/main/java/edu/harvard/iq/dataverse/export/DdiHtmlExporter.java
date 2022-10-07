package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.ddi.DdiToHtmlTransformer;
import edu.harvard.iq.dataverse.export.ddi.DdiDatasetExportService;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class DdiHtmlExporter extends ExporterBase {

    private static final Logger logger = LoggerFactory.getLogger(DdiHtmlExporter.class);

    private DdiDatasetExportService ddiDatasetExportService;
    private VocabularyValuesIndexer vocabularyValuesIndexer;
    private DdiToHtmlTransformer ddiToHtmlTransformer;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public DdiHtmlExporter(CitationFactory citationFactory, SettingsServiceBean settingsService,
                           DdiDatasetExportService ddiDatasetExportService, VocabularyValuesIndexer vocabularyValuesIndexer,
                           DdiToHtmlTransformer ddiToHtmlTransformer) {
        super(citationFactory, settingsService);
        this.ddiDatasetExportService = ddiDatasetExportService;
        this.vocabularyValuesIndexer = vocabularyValuesIndexer;
        this.ddiToHtmlTransformer = ddiToHtmlTransformer;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // needn't be closed
            Map<String, Map<String, String>> vocabulary
                    = vocabularyValuesIndexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(version, Locale.ENGLISH);
            ddiDatasetExportService.datasetJson2ddi(createDTO(version), version, outputStream, vocabulary);
            ByteArrayInputStream input = new ByteArrayInputStream(outputStream.toByteArray()); // needn't be closed
            StringWriter output = new StringWriter(); // needn't be closed
            ddiToHtmlTransformer.transform(input, output);
            return output.toString();
        } catch (XMLStreamException e) {
            logger.warn("Exception encountered: ", e);
        }
        return "<html><body>Error encountered during dataset export.</body></html>";
    }

    @Override
    public String getProviderName() {
        return ExporterType.DDI_HTML.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.DDI_HTML;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.isNotBlank(BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi.html"))
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi.html")
                : "DDI Html Codebook";
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
        return null;
    }

    @Override
    public String getXMLSchemaLocation() {
        return null;
    }

    @Override
    public String getXMLSchemaVersion() {
        return null;
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_HTML;
    }
}
