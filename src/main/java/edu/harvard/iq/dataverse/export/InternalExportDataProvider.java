package edu.harvard.iq.dataverse.export;

import java.io.InputStream;
import java.util.Optional;

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
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.gdcc.spi.export.ExportDataProvider;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import static edu.harvard.iq.dataverse.util.FileUtil.MIME_TYPE_INGESTED_FILE;
import io.gdcc.spi.export.ExportException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private DatasetVersionFilesServiceBean datasetVersionFilesService;

    InternalExportDataProvider(DatasetVersion dv) {
        this.dv = dv;
    }
    
    InternalExportDataProvider(DatasetVersion dv, InputStream is) {
        this.dv = dv;
        this.is=is;
    }
    
    InternalExportDataProvider(DatasetVersion dv, DatasetVersionFilesServiceBean datasetVersionFilesService) {
        this.dv = dv;
        this.datasetVersionFilesService = datasetVersionFilesService;
    }

    @Override
    public JsonObject getDatasetJson(ExportDataOption... options) {
        if (isOnlyDatasetLevelMetadataRequested(options)) {
            // If we already have the "full" Json representation (with files) 
            // generated, should we return it (potentially moving MUCH more json 
            // than the client needs, or spend extra cycles generating the short 
            // form from scratch? - I'm choosing to go with latter. 
            if (jsonRepresentationNoFiles == null) {
                final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.jsonAsDatasetDto(dv, false);
                jsonRepresentationNoFiles = datasetAsJsonBuilder.build();
            }
            return jsonRepresentationNoFiles;
        }
        
        if (jsonRepresentation == null) {
            final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.jsonAsDatasetDto(dv);
            jsonRepresentation = datasetAsJsonBuilder.build();
        }
        return jsonRepresentation;
    }
    
    @Override
    public JsonObject getDatasetSchemaDotOrg() {
        if (schemaDotOrgRepresentation == null) {
            String jsonLdAsString = dv.getJsonLd();
            schemaDotOrgRepresentation = JsonUtil.getJsonObject(jsonLdAsString);
        }
        return schemaDotOrgRepresentation;
    }

    @Override
    public JsonObject getDatasetORE() {
        if (oreRepresentation == null) {
            oreRepresentation = new OREMap(dv).getOREMap();
        }
        return oreRepresentation;
    }

    @Override
    public String getDataCiteXml() {
        return DOIDataCiteRegisterService.getMetadataFromDvObject(
                dv.getDataset().getGlobalId().asString(), new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
    }
    
    @Override
    public JsonArray getDatasetFileDetails() {
        if (fileAndDataDetails == null) {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for (FileMetadata fileMetadata : dv.getFileMetadatas()) {
                DataFile dataFile = fileMetadata.getDataFile();
                jab.add(JsonPrinter.json(dataFile, fileMetadata, true));
            }
            fileAndDataDetails = jab.build();
        }
        return fileAndDataDetails;
    }
    
    @Override
    public JsonArray getTabularDataDetails(Integer offset, Integer length, ExportDataOption... options) throws ExportException {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        
        List<FileMetadata> fileMetadatas = null; 
        Optional<DatasetVersionFilesServiceBean> datasetVersionFilesServiceOptional = getDatasetVersionFilesService(); 
        
        if (datasetVersionFilesServiceOptional.isPresent()) {
            
            FileSearchCriteria fileSearchCriteria;
            try {
                fileSearchCriteria = new FileSearchCriteria(
                        MIME_TYPE_INGESTED_FILE,
                        isOnlyPublicMetadataRequested(options) ? FileSearchCriteria.FileAccessStatus.Public : null, // this is optional
                        null,
                        null,
                        null
                );
            } catch (IllegalArgumentException e) {
                throw new ExportException("Failed to build a retrieval query for tabular file metadata");
            }
            
            
            fileMetadatas = datasetVersionFilesServiceOptional.get().getFileMetadatas(dv, length, offset, fileSearchCriteria, DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ);

        } else {
            throw new ExportException("EJB DatasetVersionFilesService is not available"); 
        }
        
        for (FileMetadata fileMetadata : dv.getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            jab.add(JsonPrinter.json(dataFile, fileMetadata, true));
        }
        return jab.build();
    }
    
    @Override
    public Optional<InputStream> getPrerequisiteInputStream() {
        return Optional.ofNullable(is);
    }
    
    public Optional<DatasetVersionFilesServiceBean> getDatasetVersionFilesService() {
        return Optional.ofNullable(datasetVersionFilesService);
    }

    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }
    
    public void setDatasetVersionFilesService(DatasetVersionFilesServiceBean datasetVersionFilesService) {
        this.datasetVersionFilesService = datasetVersionFilesService; 
    }
    
    private boolean isOnlyDatasetLevelMetadataRequested(ExportDataOption... options) {
        for (ExportDataOption option : options) {

            if (option == ExportDataOption.DatasetOnly) {
                return true;
            } 
        }

        // By default, we pack both the Dataset, and the File-level metadata in that Json
        return false;
    }
    
    private boolean isOnlyPublicMetadataRequested(ExportDataOption... options) throws ExportException {

        for (ExportDataOption option : options) {

            if (option == ExportDataOption.PublicFilesOnly) {
                return true;
            } else {
                throw new ExportException("Unsupported data export option");
            }
        }

        // By default, we return the metadata for all files - embargoed, restricted, etc.:
        return false;
    }
}
