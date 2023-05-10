package io.gdcc.spi.export;

import java.io.OutputStream;
import java.util.Locale;

public interface Exporter {

    /*
     * When implementing exportDataset, when done writing content, please make sure
     * to flush() the outputStream, but NOT close() it! This way an exporter can be
     * used to insert the produced metadata into the body of an HTTP response, etc.
     * (for example, to insert it into the body of an OAI response, where more XML
     * needs to be written, for the outer OAI-PMH record). -- L.A. 4.5
     */

    void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException;

    String getFormatName();

    String getDisplayName(Locale locale);

    default String getPrerequisiteExporterName() {
        return null;
    }

    default Boolean isXMLFormat() {
        return false;
    }

    Boolean isHarvestable();

    Boolean isAvailableToUsers();

    String getMediaType();

}
