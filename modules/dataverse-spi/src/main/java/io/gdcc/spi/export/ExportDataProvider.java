package io.gdcc.spi.export;

import java.io.InputStream;
import java.util.Optional;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Provides all the metadata Dataverse has about a given dataset that can then
 * be used by an @see Exporter to create a new metadata export format.
 * 
 */
public interface ExportDataProvider {

    /**
     * @return - dataset metadata in the standard Dataverse JSON format used in the
     *         API and available as the JSON metadata export via the user interface.
     * @apiNote - there is no JSON schema defining this output, but the format is
     *          well documented in the Dataverse online guides. This, and the
     *          OAI_ORE export are the only two that provide 'complete'
     *          dataset-level metadata along with basic file metadata for each file
     *          in the dataset.
     * @param options - optional argument(s). needs to support ExportDataOption.DatasetMetadataOnly: 
     *               in a situation where we need to generate a format like DC,
     *               that has no use for file-level metadata, it makes sense to
     *               skip retrieving and formatting it, since there can be quite a few
     *               files in a dataset.
     */
    JsonObject getDatasetJson(ExportDataOption... options);    

    /**
     * 
     * @return - dataset metadata in the JSON-LD based OAI_ORE format used in
     *         Dataverse's archival bag export mechanism and as available in the
     *         user interface and by API.
     * @apiNote - THis, and the JSON format are the only two that provide complete
     *          dataset-level metadata along with basic file metadata for each file
     *          in the dataset.
     */
    JsonObject getDatasetORE(ExportDataOption... options);

    /**
     * Dataverse is capable of extracting DDI-centric metadata from tabular
     * datafiles. This detailed metadata, which is only available for successfully
     * "ingested" tabular files, is not included in the output of any other methods
     * in this interface. 
     * 
     * @return - a JSONArray with one entry per ingested tabular dataset file.
     * @apiNote - there is no JSON schema available for this output and the format
     *          is not well documented. Implementers may wish to expore the @see
     *          edu.harvard.iq.dataverse.export.DDIExporter and the @see
     *          edu.harvard.iq.dataverse.util.json.JSONPrinter classes where this
     *          output is used/generated (respectively).
     */
    JsonArray getDatasetFileDetails(ExportDataOption... options);

    /**
     * Similar to the above, but 
     * a) retrieves the information for the ingested/tabular data files _only_
     * b) provides an option for retrieving this stuff in batches 
     * c) provides an option for skipping restricted/embargoed etc. files.
     * Intended for datasets with massive numbers of tabular files and datavariables. 
     * @param offset (can be null)
     * @param length (can be null)
     * @param options (optional) current use case is ExportDataOption.PublicFilesOnly;
     * @return json array containing the datafile/filemetadata->datatable->datavariable metadata
     * @throws ExportException
     */
    JsonArray getTabularDataDetails(Integer offset, Integer length, ExportDataOption ... options) throws ExportException;
    
    /**
     * 
     * @return - the subset of metadata conforming to the schema.org standard as
     *         available in the user interface and as included as header metadata in
     *         dataset pages (for use by search engines)
     * @apiNote - as this metadata export is not complete, it should only be used as
     *          a starting point for an Exporter if it simplifies your exporter
     *          relative to using the JSON or OAI_ORE exports.
     */
    JsonObject getDatasetSchemaDotOrg(ExportDataOption... options);

    /**
     * 
     * @return - the subset of metadata conforming to the DataCite standard as
     *         available in the Dataverse user interface and as sent to DataCite when DataCite DOIs are used.
     * @apiNote - as this metadata export is not complete, it should only be used as
     *          a starting point for an Exporter if it simplifies your exporter
     *          relative to using the JSON or OAI_ORE exports.
     */
    String getDataCiteXml(ExportDataOption... options);

    /**
     * If an Exporter has specified a prerequisite format name via the
     * getPrerequisiteFormatName() method, it can call this method to retrieve
     * metadata in that format.
     * 
     * @return - metadata in the specified prerequisite format (if available from
     *         another internal or added Exporter) as an Optional<InputStream>
     * @apiNote - This functionality is intended as way to easily generate alternate
     *          formats of the ~same metadata, e.g. to support download as XML,
     *          HTML, PDF for a specific metadata standard (e.g. DDI). It can be
     *          particularly useful, reative to starting from the output of one of
     *          the getDataset* methods above, if there are existing libraries that
     *          can convert between these formats. Note that, since Exporters can be
     *          replaced, relying on this method could cause your Exporter to
     *          malfunction, e.g. if you depend on format "ddi" and a third party
     *          Exporter is configured to replace the internal ddi Exporter in
     *          Dataverse.
     */
    default Optional<InputStream> getPrerequisiteInputStream(ExportDataOption... options) {
        return Optional.empty();
    }
   
 }
