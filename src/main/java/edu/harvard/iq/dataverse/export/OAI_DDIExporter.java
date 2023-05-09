
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import io.gdcc.export.spi.ExportDataProviderInterface;
import io.gdcc.export.spi.ExportException;
import io.gdcc.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import java.util.Locale;

import javax.json.JsonObject;
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
    public String getDisplayName(Locale locale) {
        // dataset.exportBtn.itemLabel.ddi is shared with the DDIExporter
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi",locale);
        return displayName != null ? displayName : "DDI";
    }

    @Override
    public void exportDataset(ExportDataProviderInterface dataProvider, OutputStream outputStream) throws ExportException {
        try {
            DdiExportUtil.datasetJson2ddi(dataProvider.getDatasetJson(), outputStream);
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
}
