package io.gdcc.spi.export;

import jakarta.ws.rs.core.MediaType;

/**
 * XML Exporter is an extension of the base Exporter interface that adds the
 * additional methods needed for generating XML metadata export formats.
 */
public interface XMLExporter extends Exporter {

    /**
     * @implNote for the ddi exporter, this method returns "ddi:codebook:2_5"
     * @return - the name space of the XML schema
     */
    String getXMLNameSpace();

    /**
     * @apiNote According to the XML specification, the value must be a URI
     * @implNote for the ddi exporter, this method returns
     *           "https://ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd"
     * @return - the location of the XML schema as a String (must be a valid URI)
     */
    String getXMLSchemaLocation();

    /**
     * @implNote for the ddi exporter, this method returns "2.5"
     * @return - the version of the XML schema
     */
    String getXMLSchemaVersion();

    /**
     * @return - should always be MediaType.APPLICATION_XML
     */
    public default String getMediaType() {
        return MediaType.APPLICATION_XML;
    };
}
