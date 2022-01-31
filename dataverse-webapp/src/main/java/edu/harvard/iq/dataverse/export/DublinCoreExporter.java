package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.control.Try;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class DublinCoreExporter extends ExporterBase {


    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public DublinCoreExporter(SettingsServiceBean settingsService, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
    }

    // -------------------- LOGIC --------------------

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        return Try.withResources(ByteArrayOutputStream::new)
                .of(byteArrayOutputStream -> {
                    DublinCoreExportUtil.datasetJson2dublincore(createDTO(version),
                                                                byteArrayOutputStream,
                                                                DublinCoreExportUtil.DC_FLAVOR_OAI);
                    return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                })
                .getOrElseThrow(throwable -> new ExportException("There was a problem with exporting datasetVersion: " + version.toString(), throwable));
    }

    @Override
    public String getProviderName() {
        return ExporterType.DUBLINCORE.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.DUBLINCORE;
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
}
