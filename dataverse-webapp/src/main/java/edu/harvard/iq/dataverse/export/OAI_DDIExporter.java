package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

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

public class OAI_DDIExporter implements Exporter {

    private static String DEFAULT_XML_NAMESPACE = "ddi:codebook:2_5";
    private static String DEFAULT_XML_SCHEMALOCATION = "http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd";
    private static String DEFAULT_XML_VERSION = "2.5";

    private boolean excludeEmailFromExport;
    private String dataverseUrl;
    private VocabularyValuesIndexer vocabularyValuesIndexer;

    // -------------------- CONSTRUCTORS --------------------

    public OAI_DDIExporter(boolean excludeEmailFromExport, String dataverseUrl) {
        this.excludeEmailFromExport = excludeEmailFromExport;
        this.dataverseUrl = dataverseUrl;
        this.vocabularyValuesIndexer = new VocabularyValuesIndexer();
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.OAIDDI.toString();
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            JsonObject datasetAsJson = JsonPrinter.jsonAsDatasetDto(version, excludeEmailFromExport)
                    .build();
            Map<String, Map<String, String>> localizedValuesIndex
                    = vocabularyValuesIndexer.indexLocalizedNamesOfUsedKeysByTypeAndValue(version, Locale.ENGLISH);
            DdiExportUtil.datasetJson2ddi(datasetAsJson, byteArrayOutputStream, dataverseUrl, localizedValuesIndex);
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
        return OAI_DDIExporter.DEFAULT_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return OAI_DDIExporter.DEFAULT_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return OAI_DDIExporter.DEFAULT_XML_VERSION;
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }
}
