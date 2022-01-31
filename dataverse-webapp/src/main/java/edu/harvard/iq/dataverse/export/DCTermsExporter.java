package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Leonid Andreev
 */
@ApplicationScoped
public class DCTermsExporter extends ExporterBase {


    // -------------------- CONSTRUCTORS --------------------

    @Inject
    DCTermsExporter(SettingsServiceBean settingsService, CitationFactory citationFactory) {
        super(citationFactory, settingsService);
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getProviderName() {
        return ExporterType.DCTERMS.getPrefix();
    }

    @Override
    public ExporterType getExporterType() {
        return ExporterType.DCTERMS;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") != null
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore")
                : "Dublin Core (DCTERMS)";
    }

    @Override
    public String exportDataset(DatasetVersion version) throws ExportException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            DublinCoreExportUtil.datasetJson2dublincore(createDTO(version), byteArrayOutputStream, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | IOException xse) {
            throw new ExportException("Caught XMLStreamException performing DCTERMS export", xse);
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
}
