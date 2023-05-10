package io.gdcc.spi.export;

import javax.ws.rs.core.MediaType;

public interface XMLExporter extends Exporter {

    default Boolean isXMLFormat() {
        return true;
    };

    String getXMLNameSpace();

    String getXMLSchemaLocation();

    String getXMLSchemaVersion();

    public default String getMediaType() {
        return MediaType.APPLICATION_XML;
    };

}
