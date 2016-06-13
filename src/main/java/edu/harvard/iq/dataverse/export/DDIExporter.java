
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.OutputStream;
import javax.json.JsonObject;

/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class DDIExporter implements Exporter {

    @Override
    public String getProvider() {
        return "DDI";
    }

    @Override
    public String getButtonLabel() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi") : "DDI";
    }

    @Override
    public OutputStream exportDataset(JsonObject json) {
        return DdiExportUtil.datasetJson2ddi(json);
    }

    @Override
    public Boolean isXMLFormat() {
        return true; 
    }
    
}
