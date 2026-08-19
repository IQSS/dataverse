package edu.harvard.iq.dataverse.export;

import java.io.InputStream;
import java.util.Optional;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionFilesServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileSearchCriteria;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DOIDataCiteRegisterService;
import io.gdcc.spi.export.ExportDataProvider;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlUtil;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.FileMetadataPredicates;
import io.gdcc.spi.export.PageRequest;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides all data necessary to create an export
 *
 */
public class InternalExportDataProvider implements ExportDataProvider {

    private final DatasetVersion dv;
    private JsonObject jsonRepresentation = null;
    private JsonObject jsonRepresentationNoFiles = null;
    private JsonObject schemaDotOrgRepresentation = null;
    private JsonObject oreRepresentation = null;
    private JsonArray fileAndDataDetails = null;
    private InputStream is = null;
    private DatasetVersionFilesServiceBean datasetVersionFilesService = null;

    InternalExportDataProvider(DatasetVersion dv) {
        this.dv = dv;
    }

    InternalExportDataProvider(DatasetVersion dv, InputStream is) {
        this.dv = dv;
        this.is=is;
    }
    
    /**
     * This constructor is exclusively for use in IT tests
     * @param dv
     * @param versionFilesService
     */
    InternalExportDataProvider(DatasetVersion dv, DatasetVersionFilesServiceBean versionFilesService) {
        this.dv = dv;
        this.datasetVersionFilesService = versionFilesService;
    }

    @Override
    /**
     * The legacy/deprecated version of getDatasetJson()
     * The spi now provides the default, so this implementation is not required.
     * It is here however to continue caching the generated fragment - legacy behavior.
     * Note that no attempt has been made to make this cache thread-safe. 
     * @return JsonObject representing the complete metadata record (this includes
     *         files; again, legacy behavior).
     */
    public JsonObject getDatasetJson() {
        if (jsonRepresentation == null) {
            final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.datasetAsJsonForDTO(dv);
            jsonRepresentation = datasetAsJsonBuilder.build();
        }
        return jsonRepresentation;

    }
    
    @Override
    public JsonObject getDatasetJson(DatasetExportQuery query) {
        // One can now specifically ask for that json fragment _without_ 
        // any file-level info (if the exporter has no use for it)
        if (query.fileQuery().requires(FileMetadataPredicates.SKIP_FILES)) {
           return JsonPrinter.datasetAsJsonForDTO(dv, false).build();
        }

        return JsonPrinter.datasetAsJsonForDTO(dv).build();
    }

    @Override
    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery query) {
        if (schemaDotOrgRepresentation == null) {
            String jsonLdAsString = dv.getJsonLd();
            schemaDotOrgRepresentation = JsonUtil.getJsonObject(jsonLdAsString);
        }
        return schemaDotOrgRepresentation;
    }

    @Override
    public JsonObject getDatasetORE(DatasetExportQuery query) {
        if (oreRepresentation == null) {
            oreRepresentation = new OREMap(dv).getOREMap();
        }
        return oreRepresentation;
    }

    @Override
    public String getDataCiteXml() {
        // @todo Is this the best way to obtain the metadata? - as opposed to
        // going through the normal Export framework? (it may be, if it needs
        // to be version-specific - ?)
        return DOIDataCiteRegisterService.getMetadataFromDvObject(
                dv.getDataset().getGlobalId().asString(), new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
    }

    @Override
    public Document getDataCiteXml(DatasetExportQuery query) {
        // Note that the query parameter is ignored, for now
        String dataciteXmlString = getDataCiteXml();

        DocumentBuilderFactory factory = XmlUtil.getSecureDocumentBuilderFactory();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new InputSource(new StringReader(dataciteXmlString)));
        } catch (ParserConfigurationException | SAXException | IOException px) {
            throw new ExportException("Failed to parse the DataCite metadata fragment as valid XML");
        }

    }

    @Override
    public JsonArray getDatasetFileDetails() {
        if (fileAndDataDetails == null) {
            JsonArrayBuilder jab = JsonUtil.createArrayBuilder();
            for (FileMetadata fileMetadata : dv.getFileMetadatas()) {
                DataFile dataFile = fileMetadata.getDataFile();
                jab.add(JsonPrinter.json(dataFile, fileMetadata, true, false, true));
            }
            fileAndDataDetails = jab.build();
        }
        return fileAndDataDetails;
    }

    @Override
    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery query) {        
        // We may have a prettier implementation going forward, but for now 
        // we are just calling the paginated version of the method with a null
        // pageRequest - i.e., no limit/no offset. 
        return getDatasetFileDetails(query, null); 
    }

    @Override
    /**
     * This new (as of dataverse-spi 2.1.0) method will attempt to retrieve
     * the requested tabular metadata more efficiently, by calling the
     * DatasetVersionFilesServiceBean method directly. Which, among other things,
     * allows to retrieve this information in batches. If for whatever reason
     * that fails - if, for example, the EJB is not available in this context,
     * we will throw an ExportException, giving the exporter a chance to try and
     * retrieve this information using the traditional all-at-once method via
     * getDatasetFileDetails();
     *
     */
    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery query, PageRequest pageRequest) {
        JsonArrayBuilder jab = JsonUtil.createArrayBuilder();

        Integer limit = null; 
        Integer offset = null; 
        
        if (pageRequest != null) {
            limit = pageRequest.getLimit();
            offset = pageRequest.getOffset();
        }
        
        if (datasetVersionFilesService == null) {
            try {
                datasetVersionFilesService = CDI.current().select(DatasetVersionFilesServiceBean.class).get();
            } catch (java.lang.IllegalArgumentException | IllegalStateException ie) {
                throw new ExportException("EJB DatasetVersionFilesService is not available; " + ie.getMessage());
            }
        }

        if (datasetVersionFilesService == null) {
            throw new ExportException("EJB DatasetVersionFilesService is not available");
        }

        // These if/else blocks below are distinctly un-pretty. 
        // My brain hurts having worked on thie PR for 7 years and I cannot think
        // of how to prettify it without obfuscating what each case actually serves. 
        if (query.requires(FileMetadataPredicates.INCLUDE_TABULAR_DATA_VARIABLES)) {
            if (query.requires(FileMetadataPredicates.ONLY_TABULAR_FILES)) {

                return datasetVersionFilesService.getTabularDataFileMetadatas(dv,
                        limit,
                        offset,
                        query.requires(FileMetadataPredicates.ONLY_PUBLIC_FILES)).stream()
                        .map(fileMetadata -> JsonPrinter.jsonDatafileWithDatatableForExport(fileMetadata.getDataFile(), fileMetadata))
                        .map(JsonObjectBuilder::build);
            } else {
                return datasetVersionFilesService.getFileMetadatas(dv,
                        limit,
                        offset,
                        createFileSearchCriteria(query.requires(FileMetadataPredicates.ONLY_PUBLIC_FILES)),
                        DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ).stream()
                        .map(fileMetadata -> JsonPrinter.jsonDatafileWithDatatableForExport(fileMetadata.getDataFile(), fileMetadata))
                        .map(JsonObjectBuilder::build);
            }
        } else {
            // First is a weird, but possible case of only the filemetadatas for 
            // the files that have datavariable metadata requested; but without
            // the actual datavariable metadata requested:
            if (query.requires(FileMetadataPredicates.ONLY_TABULAR_FILES)) {

                return datasetVersionFilesService.getTabularDataFileMetadatas(dv,
                        limit,
                        offset,
                        query.requires(FileMetadataPredicates.ONLY_PUBLIC_FILES)).stream()
                        .map(fileMetadata -> JsonPrinter.json(fileMetadata.getDataFile(), fileMetadata, true))
                        .map(JsonObjectBuilder::build);
            } else {
                return datasetVersionFilesService.getFileMetadatas(dv,
                        limit,
                        offset,
                        createFileSearchCriteria(query.requires(FileMetadataPredicates.ONLY_PUBLIC_FILES)),
                        DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ).stream()
                        .map(fileMetadata -> JsonPrinter.json(fileMetadata.getDataFile(), fileMetadata, true))
                        .map(JsonObjectBuilder::build);
            }
        }
    }

    @Override
    public Optional<InputStream> getPrerequisiteInputStream(DatasetExportQuery query) {
        return Optional.ofNullable(is);
    }

    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }

    /**
     * Service method for creating a FileSearchCriteria that the paginated 
     * getFileMetadatas() method in the DatasetVersionFilesServiceBean understands.
     * Only used for "all files" vs "public only" at present.
     * 
     * @param publicOnly
     * @return FileSearchCriteria
     */
    private FileSearchCriteria createFileSearchCriteria(boolean publicOnly) {
        try {
            return new FileSearchCriteria(
                    null,
                    publicOnly ? FileSearchCriteria.FileAccessStatus.valueOf("Public") : null,
                    null,
                    null,
                    null
            );
        } catch (IllegalArgumentException e) {
            throw new ExportException("Failed to create FileSearchCriteria");
        }
    }
}
