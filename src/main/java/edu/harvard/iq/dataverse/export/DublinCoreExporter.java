
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import javax.json.JsonObject;

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
    public OutputStream exportDataset(JsonObject json) {
        return DublinCoreExportUtil.datasetJson2dublincore(json);
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
    }
    
}
