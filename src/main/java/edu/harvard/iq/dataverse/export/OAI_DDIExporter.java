
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
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
public class OAI_DDIExporter implements Exporter {
    // TODO: move these to the export utility:
    private static String DEFAULT_XML_NAMESPACE = "ddi:codebook:2_5"; 
    private static String DEFAULT_XML_SCHEMALOCATION = "http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd";
    private static String DEFAULT_XML_VERSION = "2.5";
    
    @Override
    public String getProviderName() {
        return "oai_ddi";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        try {
            // This exporter is for the OAI ("short") flavor of the DDI - 
            // that is, without the variable/data information. The ddi export 
            // utility does not need the version entity to produce that. 
            DdiExportUtil.datasetJson2ddi(json, outputStream);
        } catch (XMLStreamException xse) {
            throw new ExportException ("Caught XMLStreamException performing DDI export");
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
        return false;
    }
    
    @Override
    public String getXMLNameSpace() throws ExportException {
        return OAI_DDIExporter.DEFAULT_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return OAI_DDIExporter.DEFAULT_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return OAI_DDIExporter.DEFAULT_XML_VERSION;
    }
    
    @Override
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }
}
