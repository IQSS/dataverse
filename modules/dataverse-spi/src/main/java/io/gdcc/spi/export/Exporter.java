package io.gdcc.spi.export;

import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;


/**
 * Dataverse allows new metadata export formats to be dynamically added a running instance. This is done by
 * deploying new classes that implement this Exporter interface.
 */

public interface Exporter {


    /**
     * When this method is called, the Exporter should write the metadata to the given OutputStream.
     * 
     * @apiNote When implementing exportDataset, when done writing content, please make sure
     * to flush() the outputStream, but NOT close() it! This way an exporter can be
     * used to insert the produced metadata into the body of an HTTP response, etc.
     * (for example, to insert it into the body of an OAI response, where more XML
     * needs to be written, for the outer OAI-PMH record). -- L.A. 4.5
     * 
     * @param dataProvider - the @see ExportDataProvider interface includes several methods that can be used to retrieve the dataset metadata in different formats. An Exporter should use one or more of these to obtain the values needed to generate metadata in the format it supports. 
     * @param outputStream - the OutputStream to write the metadata to
     * @throws ExportException - if there is an error writing the metadata
     */
    void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream) throws ExportException;

    /**
     * This method should return the name of the metadata format this Exporter
     * provides.
     * 
     * @apiNote Format names are unique identifiers for the formats supported in
     *          Dataverse. Reusing the same format name as another Exporter will
     *          result only one implementation being available. Exporters packaged
     *          as an external Jar file have precedence over the default
     *          implementations in Dataverse. Hence re-using one of the existing
     *          format names will result in the Exporter replacing the internal one
     *          with the same name. The precedence between two external Exporters
     *          using the same format name is not defined.
     *          Current format names used internally by Dataverse are:
     *          Datacite
     *          dcterms
     *          ddi
     *          oai_dc
     *          html
     *          dataverse_json
     *          oai_ddi
     *          OAI_ORE
     *          oai_datacite
     *          schema.org
     *          
     * @return - the unique name of the metadata format this Exporter
     */
    String getFormatName();

    /**
     * This method should return the display name of the metadata format this
     * Exporter provides. Display names are used in the UI, specifically in the menu
     * of avaiable Metadata Exports on the dataset page/metadata tab to identify the
     * format.
     */
    String getDisplayName(Locale locale);

    /**
     * Exporters can specify that they require, as input, the output of another
     * exporter. This is done by providing the name of that format in response to a
     * call to this method.
     * 
     * @implNote The one current example where this is done is with the html(display
     *           name "DDI html codebook") exporter which starts from the XML-based
     *           ddi format produced by that exporter.
     * @apiNote - The Exporter can expect that the metadata produced by its
     *          prerequisite exporter (as defined with this method) will be
     *          available via the ExportDataProvider.getPrerequisiteInputStream()
     *          method. The default implementation of this method returns an empty
     *          value which means the getPrerequisiteInputStream() method of the
     *          ExportDataProvider sent in the exportDataset method will return an
     *          empty Optional<InputStream>.
     * 
     */
    default Optional<String> getPrerequisiteFormatName() {
        return Optional.empty();
    }


    /**
     * Harvestable Exporters will be available as options in Dataverse's Harvesting mechanism.
     * @return true to make this exporter available as a harvesting option.
     */
    Boolean isHarvestable();

    /**
     * If an Exporter is available to users, its format will be generated for every
     * published dataset and made available via the dataset page/metadata
     * tab/Metadata Exports menu item and via the API.
     * @return true to make this exporter available to users.
     */
    Boolean isAvailableToUsers();

    /**
     * To support effective downloads of metadata in this Exporter's format, the Exporter should specify an appropriate mime type.
     * @apiNote - It is recommended to used the @see javax.ws.rs.core.MediaType enum to specify the mime type.
     * @return The mime type, e.g. "application/json", "text/plain", etc.
     */
    String getMediaType();

}
