package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Leonid Andreev
 */

public class DCTermsExporter implements Exporter {

    private boolean excludeEmailFromExport;

    // -------------------- CONSTRUCTORS --------------------

    DCTermsExporter(boolean excludeEmailFromExport) {
        this.excludeEmailFromExport = excludeEmailFromExport;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.DCTERMS.toString();
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") : "Dublin Core (DCTERMS)";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            JsonObject datasetAsJson = JsonPrinter.jsonAsDatasetDto(version, excludeEmailFromExport)
                    .build();

            DublinCoreExportUtil.datasetJson2dublincore(datasetAsJson, byteArrayOutputStream, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | IOException xse) {
            throw new ExportException("Caught XMLStreamException performing DCTERMS export");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
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
        return DublinCoreExportUtil.DCTERMS_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return DublinCoreExportUtil.DCTERMS_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DublinCoreExportUtil.DEFAULT_XML_VERSION;
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }
}
