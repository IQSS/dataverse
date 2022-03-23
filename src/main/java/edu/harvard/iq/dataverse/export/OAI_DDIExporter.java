
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import jakarta.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 * This exporter is for the OAI ("short") flavor of the DDI - that is, without
 * the variable/data information. The ddi export utility does not need the
 * version entity to produce that.
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class OAI_DDIExporter implements Exporter {
    
    @Override
    public String getProviderName() {
        // TODO: Consider adding this "short form" to the "Export Metadata" dropdown in the GUI.
        return "oai_ddi";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        try {
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
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }
}
