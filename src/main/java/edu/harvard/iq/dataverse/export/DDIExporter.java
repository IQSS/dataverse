
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.OutputStream;
import javax.ejb.EJB;
import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class DDIExporter implements Exporter {
    private static String DEFAULT_XML_NAMESPACE = "ddi:codebook:2_5"; 
    private static String DEFAULT_XML_SCHEMALOCATION = "http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd";
    private static String DEFAULT_XML_VERSION = "2.5";

    private String dataverse_site_url = "http://localhost:8080";
    
    @Override
    public String getProvider() {
        return "DDI";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public void exportDataset(JsonObject json, OutputStream outputStream) throws ExportException {
        try {
            DdiExportUtil.datasetJson2ddi(json, outputStream, dataverse_site_url);
        } catch (XMLStreamException xse) {
            throw new ExportException ("Caught XMLStreamException performing DDI export");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return true; 
    }
    
    @Override
    public String getXMLNameSpace() throws ExportException {
        return DDIExporter.DEFAULT_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return DDIExporter.DEFAULT_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return DDIExporter.DEFAULT_XML_VERSION;
    }
    
    @Override
    public void setParam(String name, String value) {
        // this exporter currently uses a single parameter - the url that it 
        // needs in order to cook the file access URLs in the otherMat and fileDscr
        // sections. 
        if ("dataverse_site_url".equals(name)) {
            this.dataverse_site_url = value;
        }
    }
}
