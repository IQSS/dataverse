
package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
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
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;

/**
 * This exporter is for the "full" DDI, that includes the file-level, <data> and
 * <var> metadata.
 *
 * @author Leonid Andreev (based on the original DDIExporter by
 * @author skraffmi - renamed OAI_DDIExporter)
 */
@AutoService(Exporter.class)
public class DDIExporter implements XMLExporter {
    public static String DEFAULT_XML_NAMESPACE = "ddi:codebook:2_5";
    public static String DEFAULT_XML_SCHEMALOCATION = "https://ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd";
    public static String DEFAULT_XML_VERSION = "2.5";
    public static final String PROVIDER_NAME = "ddi";

    @Override
    public String getFormatName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.ddi", locale);
        return Optional.ofNullable(displayName).orElse("DDI");
    }

    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException {
        try {
            XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
            xmlw.writeStartDocument();
            xmlw.flush();
            DdiExportUtil.datasetJson2ddi(dataProvider.getDatasetJson(), dataProvider.getDatasetFileDetails(),
                    outputStream);
        } catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DDI export", xse);
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
    public String getXMLNameSpace() {
        return DDIExporter.DEFAULT_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return DDIExporter.DEFAULT_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DDIExporter.DEFAULT_XML_VERSION;
    }
}
