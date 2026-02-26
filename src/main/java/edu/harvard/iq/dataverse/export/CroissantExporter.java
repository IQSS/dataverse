package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;

/** https://github.com/mlcommons/croissant */
@AutoService(Exporter.class)
public class CroissantExporter implements Exporter {

    /*
     * The name of the format it creates. If this format is already provided by a
     * built-in exporter, this Exporter will override the built-in one. (Note that
     * exports are cached, so existing metadata export files are not updated
     * immediately.)
     */
    @Override
    public String getFormatName() {
        return "croissant";
    }

    /**
     * The display name shown in the UI
     *
     * @param locale
     */
    @Override
    public String getDisplayName(Locale locale) {
        // This example includes the language in the name to demonstrate that locale is
        // available. A production exporter would instead use the locale to generate an
        // appropriate translation.
        return "Croissant";
    }

    /** Whether the exported format should be available as an option for Harvesting */
    @Override
    public Boolean isHarvestable() {
        return false;
    }

    /** Whether the exported format should be available for download in the UI and API */
    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    /**
     * Defines the mime type of the exported format - used when metadata is downloaded, i.e. to
     * trigger an appropriate viewer in the user's browser.
     */
    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

    /**
     * This method is called by Dataverse when metadata for a given dataset in this format is
     * requested.
     */
    @Override
    public void exportDataset(ExportDataProvider dataProvider, OutputStream outputStream)
            throws ExportException {
        try {
            // Start building the output format.
            JsonObjectBuilder job = Json.createObjectBuilder();
            String contextString =
                    """
            {
                "@context": {
                    "@language": "en",
                    "@vocab": "https://schema.org/",
                    "citeAs": "cr:citeAs",
                    "column": "cr:column",
                    "conformsTo": "dct:conformsTo",
                    "cr": "http://mlcommons.org/croissant/",
                    "rai": "http://mlcommons.org/croissant/RAI/",
                    "data": {
                      "@id": "cr:data",
                      "@type": "@json"
                    },
                    "dataType": {
                      "@id": "cr:dataType",
                      "@type": "@vocab"
                    },
                    "dct": "http://purl.org/dc/terms/",
                    "examples": {
                    "@id": "cr:examples",
                      "@type": "@json"
                    },
                    "extract": "cr:extract",
                    "field": "cr:field",
                    "fileProperty": "cr:fileProperty",
                    "fileObject": "cr:fileObject",
                    "fileSet": "cr:fileSet",
                    "format": "cr:format",
                    "includes": "cr:includes",
                    "isLiveDataset": "cr:isLiveDataset",
                    "jsonPath": "cr:jsonPath",
                    "key": "cr:key",
                    "md5": "cr:md5",
                    "parentField": "cr:parentField",
                    "path": "cr:path",
                    "recordSet": "cr:recordSet",
                    "references": "cr:references",
                    "regex": "cr:regex",
                    "repeated": "cr:repeated",
                    "replace": "cr:replace",
                    "samplingRate": "cr:samplingRate",
                    "sc": "https://schema.org/",
                    "separator": "cr:separator",
                    "source": "cr:source",
                    "subField": "cr:subField",
                    "transform": "cr:transform",
                    "wd": "https://www.wikidata.org/wiki/"
                }
            }
            """;
            try (JsonReader jsonReader = Json.createReader(new StringReader(contextString))) {
                JsonObject contextObject = jsonReader.readObject();
                job.add("@context", contextObject.getJsonObject("@context"));
            }

            job.add("@type", "sc:Dataset");
            job.add("conformsTo", "http://mlcommons.org/croissant/1.0");

            JsonObject datasetJson = dataProvider.getDatasetJson();

            JsonObject datasetORE = dataProvider.getDatasetORE();
            JsonObject describes = datasetORE.getJsonObject("ore:describes");
            job.add("name", StringEscapeUtils.escapeHtml4(describes.getString("title")));
            job.add("url", describes.getJsonString("@id"));
            JsonObject datasetSchemaDotOrg = dataProvider.getDatasetSchemaDotOrg();
            // We don't escape DatasetSchemaDotOrg fields like creator, description, etc. because
            // they are already escaped.
            job.add("creator", datasetSchemaDotOrg.getJsonArray("creator"));
            job.add("description", datasetSchemaDotOrg.getJsonString("description"));
            job.add("keywords", datasetSchemaDotOrg.getJsonArray("keywords"));
            job.add("license", datasetSchemaDotOrg.getString("license"));
            String datePublished = datasetSchemaDotOrg.getString("datePublished", null);
            if (datePublished != null) {
                job.add("datePublished", datasetSchemaDotOrg.getString("datePublished"));
            }
            job.add("dateModified", datasetSchemaDotOrg.getString("dateModified"));
            job.add(
                    "includedInDataCatalog",
                    datasetSchemaDotOrg.getJsonObject("includedInDataCatalog"));
            job.add("publisher", datasetSchemaDotOrg.getJsonObject("publisher"));

            /**
             * For "version", we are knowingly sending "1.0" rather than "1.0.0", even though
             * MAJOR.MINOR.PATCH is recommended by the Croissant spec. We are aware that the
             * Croissant validator throws a warning for anything other than MAJOR.MINOR.PATCH. See
             * the README for a detailed explanation and the following issues:
             * https://github.com/mlcommons/croissant/issues/609
             * https://github.com/mlcommons/croissant/issues/643
             */
            job.add("version", describes.getString("schema:version"));
            /**
             * We have been told that it's fine and appropriate to put the citation to the dataset
             * itself into "citeAs". However, the spec says "citeAs" is "A citation for a
             * publication that describes the dataset" so we have asked for clarification here:
             * https://github.com/mlcommons/croissant/issues/638
             */
            job.add("citeAs", getBibtex(datasetORE, datasetJson, datasetSchemaDotOrg));

            JsonArray funder = datasetSchemaDotOrg.getJsonArray("funder");
            if (funder != null) {
                job.add("funder", funder);
            }

            JsonArray spatialCoverage = datasetSchemaDotOrg.getJsonArray("spatialCoverage");
            if (spatialCoverage != null) {
                job.add("spatialCoverage", spatialCoverage);
            }

            JsonArray oreFiles = describes.getJsonArray("ore:aggregates");

            // Create a map so that later we can use the storageIdentifier to lookup
            // the position of the file in the array of files in the datasetORE format.
            // We don't use checksum because it's possible for a dataset to have the
            // same checksum for multiple files.
            Map<String, Integer> storageIdentifierToPositionInOre = new HashMap<>();
            for (int i = 0; i < oreFiles.size(); i++) {
                JsonObject aggregate = oreFiles.getJsonObject(i);
                String storageIdentifier = aggregate.getString("dvcore:storageIdentifier", null);
                if (storageIdentifier != null) {
                    storageIdentifierToPositionInOre.put(storageIdentifier, i);
                }
            }

            JsonArrayBuilder distribution = Json.createArrayBuilder();
            JsonArrayBuilder recordSet = Json.createArrayBuilder();
            JsonArray datasetFileDetails = dataProvider.getDatasetFileDetails();
            for (JsonValue jsonValue : datasetFileDetails) {

                JsonObjectBuilder recordSetContent = Json.createObjectBuilder();
                recordSetContent.add("@type", "cr:RecordSet");
                JsonObject fileDetails = jsonValue.asJsonObject();
                /**
                 * When there is an originalFileName, it means that the file has gone through ingest
                 * and that multiple files formats are available: original, tab-separated, and
                 * RData. Currently we are only showing the original file but we we could create
                 * additional cr:FileObject entries for tab-separated and RData as suggested in
                 * https://github.com/mlcommons/croissant/issues/641 . Should we? Is there interest
                 * in this? And would we duplicate all the cr:RecordSet entries (columns) with each
                 * additional format? Probably not as it would be the same.
                 */
                String filename =
                        StringEscapeUtils.escapeHtml4(
                                fileDetails.getString("originalFileName", null));
                if (filename == null) {
                    filename = StringEscapeUtils.escapeHtml4(fileDetails.getString("filename"));
                }
                String fileFormat = null;
                // Use the original file format, if available, since that's where the
                // contentUrl will point.
                String originalFileFormat = fileDetails.getString("originalFileFormat", null);
                if (originalFileFormat != null) {
                    if ("text/tsv".equals(originalFileFormat)) {
                        // "text/tsv" is an internal format used by Dataverse while
                        // "text/tab-separated-values" is the official IANA format
                        // that we present to the outside world
                        // See https://github.com/IQSS/dataverse/issues/11505 and
                        // https://www.iana.org/assignments/media-types/media-types.xhtml
                        fileFormat = "text/tab-separated-values";
                    } else {
                        fileFormat = originalFileFormat;
                    }
                }
                if (fileFormat == null) {
                    fileFormat = fileDetails.getString("contentType");
                }
                JsonNumber fileSize = fileDetails.getJsonNumber("originalFileSize");
                if (fileSize == null) {
                    fileSize = fileDetails.getJsonNumber("filesize");
                }

                /**
                 * We make contentSize a String ( https://schema.org/Text ) rather than a number
                 * (JsonNumber) to pass the Croissant validator and comply with the spec. We don't
                 * include a unit because the spec says "Defaults to bytes if a unit is not
                 * specified."
                 */
                String fileSizeInBytes = fileSize.toString();
                JsonObject checksum = fileDetails.getJsonObject("checksum");
                // Out of the box the checksum type will be md5
                String checksumType = checksum.getString("type").toLowerCase();
                String checksumValue = checksum.getString("value");
                String storageIdentifier = fileDetails.getString("storageIdentifier");
                int positionInOre = storageIdentifierToPositionInOre.get(storageIdentifier);
                String contentUrl =
                        oreFiles.getJsonObject(positionInOre).getString("schema:sameAs");
                String description =
                        StringEscapeUtils.escapeHtml4(fileDetails.getString("description", ""));
                /**
                 * See https://github.com/mlcommons/croissant/issues/639 for discussion with the
                 * Croissant spec leads on what to put in
                 *
                 * @id (path/to/file.txt).
                 *     <p>It's suboptimal that the directoryLabel isn't already included in
                 *     dataProvider.getDatasetFileDetails(). If it gets added as part of the
                 *     following issue, we can get it from there:
                 *     https://github.com/IQSS/dataverse/issues/10523
                 */
                String fileId = filename;
                // We don't escape directory label because many characters aren't allowed anyway
                String directoryLabel =
                        oreFiles.getJsonObject(positionInOre)
                                .getString("dvcore:directoryLabel", null);
                if (directoryLabel != null) {
                    fileId = directoryLabel + "/" + filename;
                }

                distribution.add(
                        Json.createObjectBuilder()
                                .add("@type", "cr:FileObject")
                                .add("@id", fileId)
                                .add("name", filename)
                                .add("encodingFormat", fileFormat)
                                .add(checksumType, checksumValue)
                                .add("contentSize", fileSizeInBytes)
                                .add("description", description)
                                .add("contentUrl", contentUrl));
                boolean fileRestricted = fileDetails.getBoolean("restricted");
                if (fileRestricted) {
                    // Don't add the recordSet items for restricted files.
                    // Go on to the next file.
                    continue;
                }
                int fileIndex = 0;
                JsonArray dataTables = fileDetails.getJsonArray("dataTables");
                if (dataTables == null) {
                    dataTables = JsonArray.EMPTY_JSON_ARRAY;
                }
                for (JsonValue dataTableValue : dataTables) {
                    JsonObject dataTableObject = dataTableValue.asJsonObject();
                    // Unused
                    int varQuantity = dataTableObject.getInt("varQuantity");
                    // Unused
                    int caseQuantity = dataTableObject.getInt("caseQuantity");
                    JsonArray dataVariables = dataTableObject.getJsonArray("dataVariables");
                    JsonArrayBuilder fieldSetArray = Json.createArrayBuilder();
                    for (JsonValue dataVariableValue : dataVariables) {
                        JsonObjectBuilder fieldSetObject = Json.createObjectBuilder();
                        fieldSetObject.add("@type", "cr:RecordSet");
                        JsonObject dataVariableObject = dataVariableValue.asJsonObject();
                        // TODO: should this be an integer?
                        Integer variableId = dataVariableObject.getInt("id");
                        String variableName =
                                StringEscapeUtils.escapeHtml4(dataVariableObject.getString("name"));
                        String variableDescription =
                                StringEscapeUtils.escapeHtml4(
                                        dataVariableObject.getString("label", ""));
                        String variableFormatType =
                                dataVariableObject.getString("variableFormatType");
                        String variableIntervalType =
                                dataVariableObject.getString("variableIntervalType");
                        String dataType = null;
                        /**
                         * There are only two variableFormatType types on the Dataverse side:
                         * CHARACTER and NUMERIC. (See VariableType in DataVariable.java.)
                         */
                        switch (variableFormatType) {
                            case "CHARACTER":
                                dataType = "sc:Text";
                                break;
                            case "NUMERIC":
                                dataType = getNumericType(variableIntervalType);
                                break;
                            default:
                                break;
                        }
                        fieldSetArray.add(
                                Json.createObjectBuilder()
                                        .add("@type", "cr:Field")
                                        .add("name", variableName)
                                        .add("description", variableDescription)
                                        .add("dataType", dataType)
                                        .add(
                                                "source",
                                                Json.createObjectBuilder()
                                                        .add("@id", variableId.toString())
                                                        .add(
                                                                "fileObject",
                                                                Json.createObjectBuilder()
                                                                        .add("@id", fileId))
                                                        .add(
                                                                "extract",
                                                                Json.createObjectBuilder()
                                                                        .add(
                                                                                "column",
                                                                                variableName))));
                    }
                    recordSetContent.add("field", fieldSetArray);
                    recordSet.add(recordSetContent);
                    fileIndex++;
                }
            }

            JsonArray citation = datasetSchemaDotOrg.getJsonArray("citation");
            if (citation != null) {
                job.add("citation", citation);
            }
            JsonArray temporalCoverage = datasetSchemaDotOrg.getJsonArray("temporalCoverage");
            if (temporalCoverage != null) {
                job.add("temporalCoverage", temporalCoverage);
            }
            JsonArray distributionArray = distribution.build();
            if (!distributionArray.isEmpty()) {
                job.add("distribution", distributionArray);
            }
            JsonArray recordSetArray = recordSet.build();
            if (!recordSetArray.isEmpty()) {
                job.add("recordSet", recordSetArray);
            }

            // TODO: Do we need DataCite XML?
            String dataCiteXml = dataProvider.getDataCiteXml();

            // Write the output format to the output stream.
            outputStream.write(job.build().toString().getBytes("UTF8"));
            // Flush the output stream - The output stream is automatically closed by
            // Dataverse and should not be closed in the Exporter.
            outputStream.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught in Croissant exporter. Printing stacktrace...");
            ex.printStackTrace();
            // If anything goes wrong, an Exporter should throw an ExportException.
            throw new ExportException("Unknown exception caught during export: " + ex);
        }
    }

    /*
    Here's how a BibTeX export looks in Dataverse:
    @data{DVN/TJCLKP_2017,
    author = {Durbin, Philip},
    publisher = {Harvard Dataverse},
    title = {{Open Source at Harvard}},
    UNF = {UNF:6:e9+1ZqpZtjCuBzTDSrsHgA==},
    year = {2017},
    version = {DRAFT VERSION},
    doi = {10.7910/DVN/TJCLKP},
    url = {https://doi.org/10.7910/DVN/TJCLKP}
    }
     */
    /**
     * The code is inspired by DataCitation.java upstream. However, Croissant does not want
     * newlines, so we omit them. Some notes about this example:
     *
     * <p>- DVN/TJCLKP_2017 seems strange as an identifier. This is probably a bug upstream.
     *
     * <p>- "DRAFT VERSION" is an artifact from a bug that was probably fixed in
     * https://github.com/IQSS/dataverse/pull/9705
     */
    private String getBibtex(
            JsonObject datasetORE, JsonObject datasetJson, JsonObject datasetSchemaDotOrg) {
        String identifier = datasetJson.getString("identifier");

        JsonObject oreDescribes = datasetORE.getJsonObject("ore:describes");
        String publicationYear = null;
        String publicationDate = oreDescribes.getString("schema:datePublished", null);
        if (publicationDate != null) {
            publicationYear = publicationDate.substring(0, 4);
        }

        JsonArray creatorArray = datasetSchemaDotOrg.getJsonArray("creator");
        List<String> creators = new ArrayList<>();
        for (JsonValue creator : creatorArray) {
            creators.add(creator.asJsonObject().getString("name"));
        }
        String creatorsFormatted = String.join(" and ", creators);

        String publisher = datasetSchemaDotOrg.getJsonObject("publisher").getString("name");
        String title = datasetSchemaDotOrg.getString("name");

        String pidAsUrl = oreDescribes.getString("@id");

        StringBuilder sb = new StringBuilder();
        if (publicationYear != null) {
            sb.append("@data{").append(identifier).append("_").append(publicationYear).append(",");
        } else {
            sb.append("@data{").append(identifier).append(",");
        }
        sb.append("author = {").append(creatorsFormatted).append("},");
        sb.append("publisher = {").append(publisher).append("},");
        sb.append("title = {").append(title).append("},");
        if (publicationYear != null) {
            sb.append("year = {").append(publicationYear).append("},");
        }
        sb.append("url = {").append(pidAsUrl).append("}");
        sb.append("}");
        return sb.toString();
    }

    private String getNumericType(String variableIntervalType) {
        /**
         * According to DataVariable.java in Dataverse, the four possibilities are: discrete, contin
         * (continuous), nominal, and dichotomous.
         */
        return switch (variableIntervalType) {
            case "discrete" -> "sc:Integer";
            case "contin" -> "sc:Float";
            default -> "sc:Text";
        };
    }
}
