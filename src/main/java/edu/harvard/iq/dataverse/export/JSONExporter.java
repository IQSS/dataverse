
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.json.JsonObject;


/**
 *
 * @author skraffmi
 */
@AutoService(Exporter.class)
public class JSONExporter implements Exporter {

    @Override
    public String getProvider() {
        return "json";
    }

    @Override
    public String getButtonLabel() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") : "JSON";
    }

    @Override
    public OutputStream exportDataset(JsonObject json) {
        OutputStream outputStream = new ByteArrayOutputStream();
        try{
            Writer w = new OutputStreamWriter(outputStream, "UTF-8");
            w.write(json.toString());
            w.close();
        } catch (Exception e){
            //just return waht we have...
        }
        return outputStream;
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }
    
}
