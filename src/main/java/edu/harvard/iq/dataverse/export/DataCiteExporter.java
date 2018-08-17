
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.ejb.EJB;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author qqmyers
 */
@AutoService(Exporter.class)
public class DataCiteExporter implements Exporter {

    private static String DEFAULT_XML_NAMESPACE = "http://datacite.org/schema/kernel-3";
    private static String DEFAULT_XML_SCHEMALOCATION = "http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd";
    private static String DEFAULT_XML_VERSION = "3.0";

    @Override
    public String getProviderName() {
        return "datacite";
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.datacite") != null
                ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.datacite")
                : "DataCite";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream)
            throws ExportException {
        try {
            Map<String, String> metadata = new HashMap<>();
            // From AbstractGlobalIdServiceBean
            String authorString = version.getDataset().getAuthorString();

            if (authorString.isEmpty()) {
                authorString = ":unav";
            }
            // QDR - use institution name
            String producerString = ResourceBundle.getBundle("Bundle").getString("institution.name");

            if (producerString.isEmpty()) {
                producerString = ":unav";
            }

            metadata.put("datacite.creator", authorString);
            metadata.put("datacite.title", version.getDataset().getDisplayName());
            metadata.put("datacite.publisher", producerString);

            String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                    version.getDataset().getGlobalId().asString(), metadata, version.getDataset());
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

    @Override
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }
}
