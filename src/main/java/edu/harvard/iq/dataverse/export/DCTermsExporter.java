
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Leonid Andreev
 */
@AutoService(Exporter.class)
public class DCTermsExporter implements Exporter {
    
    
    
    @Override
    public String getProviderName() {
        return "dcterms";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") : "Dublin Core (DCTERMS)";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        try {
            DublinCoreExportUtil.datasetJson2dublincore(json, outputStream, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        } catch (XMLStreamException xse) {
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
    public String getXMLNameSpace() throws ExportException {
        return DublinCoreExportUtil.DCTERMS_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return DublinCoreExportUtil.DCTERMS_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return DublinCoreExportUtil.DEFAULT_XML_VERSION;
    }
    
    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }
}
