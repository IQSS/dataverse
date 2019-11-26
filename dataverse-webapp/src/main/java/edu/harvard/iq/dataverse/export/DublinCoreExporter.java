package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import io.vavr.control.Try;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class DublinCoreExporter implements Exporter {

    private boolean excludeEmailFromExport;

    public DublinCoreExporter(boolean excludeEmailFromExport) {
        this.excludeEmailFromExport = excludeEmailFromExport;
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        JsonObjectBuilder jsonObjectBuilder = JsonPrinter.jsonAsDatasetDto(version, excludeEmailFromExport);

        JsonObject jsonDatasetVersion = jsonObjectBuilder
                .build();


        return Try.withResources(ByteArrayOutputStream::new)
                .of(byteArrayOutputStream -> {
                    DublinCoreExportUtil.datasetJson2dublincore(jsonDatasetVersion,
                                                                byteArrayOutputStream,
                                                                DublinCoreExportUtil.DC_FLAVOR_OAI);
                    return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                })
                .getOrElseThrow(throwable -> new ExportException("There was a problem with exporting datasetVersion: " + version.toString(), throwable));
    }

    @Override
    public String getProviderName() {
        return ExporterType.DUBLINCORE.name();
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") != null ?
                BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") : "Dublin Core";
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
        return DublinCoreExportUtil.OAI_DC_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return DublinCoreExportUtil.OAI_DC_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DublinCoreExportUtil.DEFAULT_XML_VERSION;
    }

    @Override
    public void setParam(String name, Object value) {

    }
}
