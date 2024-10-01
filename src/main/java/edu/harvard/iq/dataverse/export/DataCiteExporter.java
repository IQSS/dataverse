
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;

import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 *
 * @author qqmyers
 */
@AutoService(Exporter.class)
public class DataCiteExporter implements XMLExporter {
    
    public static final String NAME = "Datacite";

    @Override
    public String getFormatName() {
        return NAME;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.datacite", locale);
        return Optional.ofNullable(displayName).orElse("DataCite");
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        try {
            String xml = dataProvider.getDataCiteXml();
            outputStream.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExportException("Caught IOException performing DataCite export", e);
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
        return XmlMetadataTemplate.XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return XmlMetadataTemplate.XML_SCHEMA_LOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return XmlMetadataTemplate.XML_SCHEMA_VERSION;
    }

}
