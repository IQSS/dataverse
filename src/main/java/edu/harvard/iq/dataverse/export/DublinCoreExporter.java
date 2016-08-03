
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class DublinCoreExporter implements Exporter {
    
    
    
    @Override
    public String getProvider() {
        return "DublinCore";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore") : "Dublin Core";
    }

    @Override
    public void exportDataset(JsonObject json, OutputStream outputStream) throws ExportException {
        try {
            DublinCoreExportUtil.datasetJson2dublincore(json, outputStream);
        } catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DDI export");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
    }
    
    @Override
    public String getXMLNameSpace() throws ExportException {
        return DublinCoreExportUtil.DEFAULT_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return DublinCoreExportUtil.DEFAULT_XML_SCHEMALOCATION;
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
