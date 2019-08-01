package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.Record;
import com.lyncode.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ExporterType;
import io.vavr.control.Either;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.lyncode.xoai.xml.XmlWriter.defaultContext;

/**
 * @author Leonid Andreev
 * <p>
 * This is the Dataverse extension of XOAI Record,
 * optimized to directly output a pre-exported metadata record to the
 * output stream, thus by-passing expensive parsing and writing by
 * an XML writer, as in the original XOAI implementation.
 */

public class Xrecord extends Record {
    private static final String METADATA_FIELD = "metadata";
    private static final String METADATA_START_ELEMENT = "<" + METADATA_FIELD + ">";
    private static final String METADATA_END_ELEMENT = "</" + METADATA_FIELD + ">";
    private static final String HEADER_FIELD = "header";
    private static final String STATUS_ATTRIBUTE = "status";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String DATESTAMP_FIELD = "datestamp";
    private static final String SETSPEC_FIELD = "setSpec";
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final String DATAVERSE_EXTENDED_METADATA_API = "/api/datasets/export";

    protected Dataset dataset;
    protected String formatName;


    public Dataset getDataset() {
        return dataset;
    }

    public Xrecord withDataset(Dataset dataset) {
        this.dataset = dataset;
        return this;
    }


    public String getFormatName() {
        return formatName;
    }


    public Xrecord withFormatName(String formatName) {
        this.formatName = formatName;
        return this;
    }

    public void writeToStream(OutputStream outputStream, ExportService exportService, String dataverseUrl)
            throws IOException {
        outputStream.flush();

        String headerString = itemHeaderToString(this.header);

        if (headerString == null) {
            throw new IOException("Xrecord: failed to stream item header.");
        }

        outputStream.write(headerString.getBytes());

        // header.getStatus() is only non-null when it's indicating "deleted".
        if (header.getStatus() == null) { // Deleted records should not show metadata
            if (!isExtendedDataverseMetadataMode(formatName)) {
                outputStream.write(METADATA_START_ELEMENT.getBytes());

                outputStream.flush();

                if (dataset != null && formatName != null) {
                    Either<DataverseError, String> exportedDataset =
                            exportService.exportDatasetVersionAsString(dataset.getReleasedVersion(),
                                                                       ExporterType.valueOf(formatName));

                    if (exportedDataset.isLeft()) {
                        throw new RuntimeException(exportedDataset.getLeft().getErrorMsg());
                    }

                    outputStream.write(exportedDataset.get().getBytes());
                }
                outputStream.write(METADATA_END_ELEMENT.getBytes());
            } else {
                outputStream.write(customMetadataExtensionRef(this.dataset.getGlobalIdString(), dataverseUrl).getBytes());
            }
        }
        outputStream.flush();

    }

    private String itemHeaderToString(Header header) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            XmlWriter writer = new XmlWriter(byteOutputStream, defaultContext());

            writer.writeStartElement(HEADER_FIELD);

            if (header.getStatus() != null) {
                writer.writeAttribute(STATUS_ATTRIBUTE, header.getStatus().value());
            }
            writer.writeElement(IDENTIFIER_FIELD, header.getIdentifier());
            writer.writeElement(DATESTAMP_FIELD, header.getDatestamp());
            for (String setSpec : header.getSetSpecs()) {
                writer.writeElement(SETSPEC_FIELD, setSpec);
            }
            writer.writeEndElement(); // header
            writer.flush();
            writer.close();

            String ret = byteOutputStream.toString();

            return ret;
        } catch (Exception ex) {
            return null;
        }
    }

    private String customMetadataExtensionRef(String identifier, String dataverseUrl) {
        String ret = "<" + METADATA_FIELD
                + " directApiCall=\""
                + dataverseUrl
                + DATAVERSE_EXTENDED_METADATA_API
                + "?exporter="
                + DATAVERSE_EXTENDED_METADATA_FORMAT
                + "&amp;persistentId="
                + identifier
                + "\""
                + "/>";

        return ret;
    }

    private boolean isExtendedDataverseMetadataMode(String formatName) {
        return DATAVERSE_EXTENDED_METADATA_FORMAT.equals(formatName);
    }
}
