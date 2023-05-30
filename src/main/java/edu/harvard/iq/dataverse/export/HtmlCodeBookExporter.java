package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
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
import java.util.Locale;
import java.util.Optional;

@AutoService(Exporter.class)
public class HtmlCodeBookExporter implements Exporter {

    @Override
    public String getFormatName() {
        return "html";
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.html", locale);
        return Optional.ofNullable(displayName).orElse("DDI html codebook");
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        Optional<InputStream> ddiInputStreamOptional = dataProvider.getPrerequisiteInputStream();
        if (ddiInputStreamOptional.isPresent()) {
            try (InputStream ddiInputStream = ddiInputStreamOptional.get()) {
                DdiExportUtil.datasetHtmlDDI(ddiInputStream, outputStream);
            } catch (IOException e) {
                throw new ExportException("Cannot open export_ddi cached file");
            } catch (XMLStreamException xse) {
                throw new ExportException("Caught XMLStreamException performing DDI export");
            }
        } else {
            throw new ExportException("No prerequisite input stream found");
        }
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
    public  Optional<String> getPrerequisiteFormatName() {
        //This exporter relies on being able to get the output of the ddi exporter
        return Optional.of("ddi");
    }

    @Override
    public String  getMediaType() {
        return MediaType.TEXT_HTML;
    };
}
