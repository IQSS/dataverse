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
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DOIDataCiteRegisterService;
import io.gdcc.spi.export.ExportDataProvider;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.gdcc.spi.export.ExportException;
//import io.gdcc.spi.export.ExportDataContext;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.DatasetMetadataPredicates;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.FileMetadataPredicates;
import io.gdcc.spi.export.PageRequest;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
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

    InternalExportDataProvider(DatasetVersion dv) {
        this.dv = dv;
    }
    
    InternalExportDataProvider(DatasetVersion dv, InputStream is) {
        this.dv = dv;
        this.is=is;
    }

    @Override
    public JsonObject getDatasetJson(DatasetExportQuery query) {
        if (isOnlyDatasetLevelMetadataRequested(query)) {
            // If we already have the "full" Json representation (with files) 
            // generated, should we return it (potentially moving MUCH more json 
            // than the client needs, or spend extra cycles generating the short 
            // form from scratch? - I'm choosing to go with latter. 
            if (jsonRepresentationNoFiles == null) {
                final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.datasetAsJsonForDTO(dv, false);
                jsonRepresentationNoFiles = datasetAsJsonBuilder.build();
            }
            return jsonRepresentationNoFiles;
        }
        
        if (jsonRepresentation == null) {
            final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.datasetAsJsonForDTO(dv);
            jsonRepresentation = datasetAsJsonBuilder.build();
        }
        return jsonRepresentation;
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

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
        
            return builder.parse(new InputSource(new StringReader(dataciteXmlString)));
        } catch (ParserConfigurationException | SAXException | IOException px) {
            return null;
        } 

    }
    
    @Override
    public JsonArray getDatasetFileDetails() {
        if (fileAndDataDetails == null) {
            JsonArrayBuilder jab = Json.createArrayBuilder();
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
        if (fileAndDataDetails == null) {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for (FileMetadata fileMetadata : dv.getFileMetadatas()) {
                DataFile dataFile = fileMetadata.getDataFile();
                jab.add(JsonPrinter.json(dataFile, fileMetadata, true, false, true));
            }
            fileAndDataDetails = jab.build();
        }
        return fileAndDataDetails.stream().map(jsonValue -> jsonValue.asJsonObject());
    }
    
    @Override
    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery query, PageRequest pageRequest) {
        JsonArrayBuilder jab = Json.createArrayBuilder();

        List<FileMetadata> fileMetadatas;
        DatasetVersionFilesServiceBean datasetVersionFilesService = null;
        try {
            datasetVersionFilesService = CDI.current().select(DatasetVersionFilesServiceBean.class).get();
        } catch (java.lang.IllegalArgumentException | IllegalStateException ie) {
            throw new ExportException("EJB DatasetVersionFilesService is not available; " + ie.getMessage());
        }

        if (datasetVersionFilesService == null) {
            throw new ExportException("EJB DatasetVersionFilesService is not available");
        }

        /* 
         * Defaulting to retrieving tabular/DataVariable-level metadata, for now;
         * we will want to honor the related predicates in the long run. 
        */
        fileMetadatas = datasetVersionFilesService.getTabularDataFileMetadatas(dv, 
                pageRequest.getOffset(), 
                pageRequest.getOffset(),
                isOnlyPublicMetadataRequested(query));
        
        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            jab.add(JsonPrinter.jsonDatafileWithDatatableForExport(dataFile, fileMetadata));
        }
        
        return jab.build().stream().map(jsonValue -> jsonValue.asJsonObject());
    }
    
    //@Override
    // This method, specifically for tabular files only, was in my initial 
    // implementation of 2.1.0, but later dropped in favor of a more flexible 
    // getDatasetFileDetails(...) method
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
    /*public JsonArray getTabularDataDetails(ExportDataContext... context) throws ExportException {
        JsonArrayBuilder jab = Json.createArrayBuilder();

        List<FileMetadata> fileMetadatas;
        DatasetVersionFilesServiceBean datasetVersionFilesService = null;
        try {
            datasetVersionFilesService = CDI.current().select(DatasetVersionFilesServiceBean.class).get();
        } catch (java.lang.IllegalArgumentException | IllegalStateException ie) {
            throw new ExportException("EJB DatasetVersionFilesService is not available; " + ie.getMessage());
        }

        if (datasetVersionFilesService == null) {
            throw new ExportException("EJB DatasetVersionFilesService is not available");
        }

        fileMetadatas = datasetVersionFilesService.getTabularDataFileMetadatas(dv, 
                getLength(context), 
                getOffset(context),
                isOnlyPublicMetadataRequested(context));
        
        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            jab.add(JsonPrinter.jsonDatafileWithDatatableForExport(dataFile, fileMetadata));
        }
        return jab.build();
    }*/
    
    @Override
    public Optional<InputStream> getPrerequisiteInputStream(DatasetExportQuery query) {
        return Optional.ofNullable(is);
    }
    
    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }
    
    /**
     * Only one context object is supported 
     * @param DatasetExportQuery
     * @return 
     */
    private boolean isOnlyDatasetLevelMetadataRequested(DatasetExportQuery query) {

        Set<DatasetMetadataPredicates> predicates = query.getDatasetPredicates();
        
        for (DatasetMetadataPredicates p : predicates) {
            // @todo This is pending on adding a dedicated DATASET_LEVEL_ONLY predicate
            // to the enum
            //if (p.equals(DatasetMetadataPredicates.DATASET_LEVEL_ONLY)) return true;
        }

        // The default assumption is we pack both the Dataset, and the File-level 
        // metadata in the Json
        return false;
    }
    
    /**
     * Are we skipping non-public, restricted and/or embargoed files?
     *
     * @param FileExportQuery
     * @return yes or no
     */
    private boolean isOnlyPublicMetadataRequested(FileExportQuery query) {

        Set<FileMetadataPredicates> predicates = query.getFilePredicates();
        
        for (FileMetadataPredicates p : predicates) {
            if (p.equals(FileMetadataPredicates.ONLY_PUBLIC_FILES)) return true;
        }

        return false;
    }
}
