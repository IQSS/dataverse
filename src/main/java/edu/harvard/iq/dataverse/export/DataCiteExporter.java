
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.export.spi.ExportDataProviderInterface;
import edu.harvard.iq.dataverse.export.spi.ExportException;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 *
 * @author qqmyers
 */
@AutoService(Exporter.class)
public class DataCiteExporter implements Exporter {

    private static String DEFAULT_XML_NAMESPACE = "http://datacite.org/schema/kernel-3";
    private static String DEFAULT_XML_SCHEMALOCATION = "http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd";
    private static String DEFAULT_XML_VERSION = "3.0";

    public static final String NAME = "Datacite";
    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.datacite", locale);
        return displayName != null ? displayName : "DataCite";
    }

    @Override
    public void exportDataset(ExportDataProviderInterface dataProvider, OutputStream outputStream)
            throws ExportException {
        try {
            String xml = dataProvider.getDataCiteXml();
            outputStream.write(xml.getBytes(Charset.forName("utf-8")));
        } catch (IOException e) {
            throw new ExportException("Caught IOException performing DataCite export");
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
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        return DataCiteExporter.DEFAULT_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return DataCiteExporter.DEFAULT_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return DataCiteExporter.DEFAULT_XML_VERSION;
    }

}
