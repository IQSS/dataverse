
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.ExportDataProviderInterface;
import edu.harvard.iq.dataverse.export.spi.ExportException;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import java.util.Locale;

import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class DublinCoreExporter implements Exporter {
    
    
   
    @Override
    public String getProviderName() {
        return "oai_dc";
    }

    @Override
    public String getDisplayName(Locale locale) {
        //ToDo: dataset.exportBtn.itemLabel.dublinCore is shared with the DCTermsExporter
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore", locale);
        return displayName != null ? displayName : "Dublin Core";
    }

    @Override
    public void exportDataset(ExportDataProviderInterface dataProvider, OutputStream outputStream) throws ExportException {
        try {
            DublinCoreExportUtil.datasetJson2dublincore(dataProvider.getDatasetJson(), outputStream, DublinCoreExportUtil.DC_FLAVOR_OAI);
        } catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DC export");
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
        return DublinCoreExportUtil.OAI_DC_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return DublinCoreExportUtil.OAI_DC_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return DublinCoreExportUtil.DEFAULT_XML_VERSION;
    }
}
