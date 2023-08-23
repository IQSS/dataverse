package edu.harvard.iq.dataverse.export;

import java.io.OutputStream;
import java.util.Locale;

import jakarta.json.JsonObject;
import javax.xml.stream.XMLStreamException;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.util.BundleUtil;

@AutoService(Exporter.class)
public class OpenAireExporter implements XMLExporter {

    public OpenAireExporter() {
    }

    @Override
    public String getFormatName() {
        return "oai_datacite";
    }

    @Override
    public String getDisplayName(Locale locale) {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dataciteOpenAIRE", locale);
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        try {
            OpenAireExportUtil.datasetJson2openaire(dataProvider.getDatasetJson(), outputStream);
        } catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DataCite OpenAIRE export", xse);
        }
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
