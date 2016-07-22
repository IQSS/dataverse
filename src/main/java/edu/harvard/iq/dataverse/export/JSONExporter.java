
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
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
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.json") : "JSON";
    }

    @Override
    public void exportDataset(JsonObject json, OutputStream outputStream) throws ExportException {
        try{
            outputStream.write(json.toString().getBytes("UTF8"));
            outputStream.flush();
        } catch (Exception e){
            throw new ExportException("Unknown exception caught during JSON export.");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }
    
    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException ("JSONExporter: not an XML format.");   
    }
    
    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException ("JSONExporter: not an XML format."); 
    }
    
    @Override
    public String getXMLSchemaVersion() throws ExportException {
        throw new ExportException ("JSONExporter: not an XML format."); 
    }
    
    @Override
    public void setParam(String name, String value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }
    
}
