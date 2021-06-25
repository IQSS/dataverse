package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@AutoService(Exporter.class)
public class HtmlCodeBookExporter implements Exporter {

    @Override
    public String getProviderName() {
        return "html";
    }

    @Override
    public String getDisplayName() {
        return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.html") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.html") : "DDI html codebook";
    }

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        try {
            InputStream ddiInputStream;
            try {
                ddiInputStream = ExportService.getInstance().getExport(version.getDataset(), "ddi");
            } catch(ExportException | IOException e) {
                throw new ExportException ("Cannot open export_ddi cached file");
            }
            DdiExportUtil.datasetHtmlDDI(ddiInputStream, outputStream);
        } catch (XMLStreamException xse) {
            throw new ExportException ("Caught XMLStreamException performing DDI export");
        }
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
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
        return null;
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        return null;
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        return null;
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter does not uses or supports any parameters as of now.
    }

    @Override
    public String  getMediaType() {
        return MediaType.TEXT_HTML;
    };
}
