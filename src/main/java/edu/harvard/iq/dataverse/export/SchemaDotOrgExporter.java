package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@AutoService(Exporter.class)
public class SchemaDotOrgExporter implements Exporter {

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
//        JsonObject json2 = Json.createObjectBuilder().add("foo", "bar").build();
        String jsonLdAsString = version.getJsonLd();
        StringReader foo = new StringReader(jsonLdAsString);
        JsonReader bar = Json.createReader(foo);
        JsonObject json2 = bar.readObject();
        try {
            outputStream.write(json2.toString().getBytes("UTF8"));
        } catch (IOException ex) {
            Logger.getLogger(SchemaDotOrgExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            outputStream.flush();
        } catch (IOException ex) {
            Logger.getLogger(SchemaDotOrgExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getProviderName() {
        return "schema.org";
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.schemaDotOrg");
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        // Defer harvesting because the current effort was estimated as a "2": https://github.com/IQSS/dataverse/issues/3700
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }

}
