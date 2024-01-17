
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;

import jakarta.json.JsonObject;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Leonid Andreev
 */
@AutoService(Exporter.class)
public class DCTermsExporter implements XMLExporter {
    
    
    
    @Override
    public String getFormatName() {
        return "dcterms";
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore", locale);
        return Optional.ofNullable(displayName).orElse("Dublin Core (DCTERMS)");
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        try {
            DublinCoreExportUtil.datasetJson2dublincore(dataProvider.getDatasetJson(), outputStream, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        } catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DCTERMS export", xse);
        }
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
    public String getXMLNameSpace() {
        return DublinCoreExportUtil.DCTERMS_XML_NAMESPACE;   
    }
    
    @Override
    public String getXMLSchemaLocation() {
        return DublinCoreExportUtil.DCTERMS_XML_SCHEMALOCATION;
    }
    
    @Override
    public String getXMLSchemaVersion() {
        return DublinCoreExportUtil.DEFAULT_XML_VERSION;
    }

}
