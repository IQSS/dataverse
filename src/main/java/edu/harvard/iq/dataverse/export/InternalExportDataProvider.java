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
import static edu.harvard.iq.dataverse.util.FileUtil.MIME_TYPE_INGESTED_FILE;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.ExportDataContext;
import java.util.List;

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
    public JsonObject getDatasetJson(ExportDataContext... context) {
        if (isOnlyDatasetLevelMetadataRequested(context)) {
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
    public JsonObject getDatasetSchemaDotOrg(ExportDataContext... context) {
        if (schemaDotOrgRepresentation == null) {
            String jsonLdAsString = dv.getJsonLd();
            schemaDotOrgRepresentation = JsonUtil.getJsonObject(jsonLdAsString);
        }
        return schemaDotOrgRepresentation;
    }

    @Override
    public JsonObject getDatasetORE(ExportDataContext... context) {
        if (oreRepresentation == null) {
            oreRepresentation = new OREMap(dv).getOREMap();
        }
        return oreRepresentation;
    }

    @Override
    public String getDataCiteXml(ExportDataContext... context) {
        return DOIDataCiteRegisterService.getMetadataFromDvObject(
                dv.getDataset().getGlobalId().asString(), new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
    }
    
    @Override
    public JsonArray getDatasetFileDetails(ExportDataContext... context) {
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
    public JsonArray getTabularDataDetails(ExportDataContext... context) throws ExportException {
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

        FileSearchCriteria fileSearchCriteria;
        try {
            fileSearchCriteria = new FileSearchCriteria(
                    MIME_TYPE_INGESTED_FILE,
                    isOnlyPublicMetadataRequested(context) ? FileSearchCriteria.FileAccessStatus.Public : null,
                    null,
                    null,
                    null
            );
        } catch (IllegalArgumentException e) {
            throw new ExportException("Failed to build a retrieval query for tabular file metadata");
        }

        fileMetadatas = datasetVersionFilesService.getFileMetadatas(dv, getLength(context), getOffset(context), fileSearchCriteria, DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ);

        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            jab.add(JsonPrinter.jsonDatafileWithDatatableForExport(dataFile, fileMetadata));
        }
        return jab.build();
    }
    
    @Override
    public Optional<InputStream> getPrerequisiteInputStream(ExportDataContext... context) {
        return Optional.ofNullable(is);
    }
    
    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }
    
    /**
     * Only one context object is supported 
     * @param contexts
     * @return 
     */
    private boolean isOnlyDatasetLevelMetadataRequested(ExportDataContext... contexts) {
        for (ExportDataContext context : contexts) {
            return context.isDatasetMetadataOnly();
        }

        // By default, if no context is supplied, we pack both the Dataset, and 
        // the File-level metadata in that Json
        return false;
    }
    
    /**
     * Only one context object is supported
     *
     * @param contexts
     * @return
     */
    private boolean isOnlyPublicMetadataRequested(ExportDataContext... contexts) {

        for (ExportDataContext context : contexts) {
            return context.isPublicFilesOnly();
        }

        // By default, if no context is supplied, we return the metadata for all 
        // files - embargoed, restricted, etc.:
        return false;
    }

    /**
     * Only one context object is supported
     *
     * @param contexts
     * @return
     */
    private Integer getOffset(ExportDataContext... contexts) {
        for (ExportDataContext context : contexts) {
            return context.getOffset();
        }
        return null;
    }

    /**
     * Only one context object is supported
     *
     * @param contexts
     * @return
     */
    private Integer getLength(ExportDataContext... contexts) {
        for (ExportDataContext context : contexts) {
            return context.getLength();
        }
        return null;
    }

}
