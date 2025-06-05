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
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DOIDataCiteRegisterService;
import io.gdcc.spi.export.ExportDataProvider;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

/**
 * Provides all data necessary to create an export
 * 
 */
public class InternalExportDataProvider implements ExportDataProvider {

    private DatasetVersion dv;
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
    public JsonObject getDatasetJson() {
        if (jsonRepresentation == null) {
            final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.jsonAsDatasetDto(dv);
            jsonRepresentation = datasetAsJsonBuilder.build();
        }
        return jsonRepresentation;
    }
    
    @Override
    public JsonObject getDatasetOnlyJson() {
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
    public Optional<InputStream> getPrerequisiteInputStream() {
        return Optional.ofNullable(is);
    }

    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }
}
