
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
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;

/**
 *
 * @author Leonid Andreev
 * (based on the original DDIExporter by
 * @author skraffmi
 * - renamed OAI_DDIExporter)
 */
@AutoService(Exporter.class)
public class DDIExporter implements Exporter {
    // TODO: 
    // move these into the ddi export utility
    private static String DEFAULT_XML_NAMESPACE = "ddi:codebook:2_5"; 
    private static String DEFAULT_XML_SCHEMALOCATION = "http://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd";
    private static String DEFAULT_XML_VERSION = "2.5";
    
    // This exporter is for the "full" DDI, that includes the file-level, 
    // <data> and <var> metadata.
    @Override
    public String getProviderName() {
        return "ddi";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        try {
		XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
		xmlw.writeStartDocument();
		xmlw.flush();
            DdiExportUtil.datasetJson2ddi(json, version, outputStream);
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
        // No, we don't want this format to be harvested!
        // For datasets with tabular data the <data> portions of the DDIs 
        // become huge and expensive to parse; even as they don't contain any 
        // metadata useful to remote harvesters. -- L.A. 4.5
        return false;
    }
    
    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }
    
    @Override
    public String getXMLNameSpace() throws ExportException {
        return this.DEFAULT_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return this.DEFAULT_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return this.DEFAULT_XML_VERSION;
    }
    
    @Override
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }
}

