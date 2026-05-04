package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.PageRequest;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides all data necessary to create an export
 * 
 */
public class InternalExportDataProvider implements ExportDataProvider {

    private DatasetVersion dv;
    private JsonObject jsonRepresentation = null;
    private JsonObject schemaDotOrgRepresentation = null;
    private JsonObject oreRepresentation = null;
    private InputStream is = null;

    InternalExportDataProvider(DatasetVersion dv) {
        this.dv = dv;
    }
    
    InternalExportDataProvider(DatasetVersion dv, InputStream is) {
        this.dv = dv;
        this.is=is;
    }

    @Override
    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
        return getDatasetJson();
    }
    
    @Override
    public JsonObject getDatasetJson() {
        if (jsonRepresentation == null) {
            final JsonObjectBuilder datasetAsJsonBuilder = JsonPrinter.jsonAsDatasetDto(dv);
            jsonRepresentation = datasetAsJsonBuilder.build();
        }
        return jsonRepresentation;
    }
    
    /**
     * Needs a better implementation, as it should replace the deprecated method.
     */
    @Override
    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
        return getDatasetSchemaDotOrg();
    }
    
    @Override
    public JsonObject getDatasetSchemaDotOrg() {
        if (schemaDotOrgRepresentation == null) {
            String jsonLdAsString = dv.getJsonLd();
            schemaDotOrgRepresentation = JsonUtil.getJsonObject(jsonLdAsString);
        }
        return schemaDotOrgRepresentation;
    }
    
    /**
     * Needs a better implementation, as it should replace the deprecated method.
     */
    @Override
    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
        return getDatasetORE();
    }
    
    @Override
    public JsonObject getDatasetORE() {
        if (oreRepresentation == null) {
            oreRepresentation = new OREMap(dv).getOREMap();
        }
        return oreRepresentation;
    }
    
    /**
     * Needs a better implementation, as it should replace the deprecated method.
     */
    @Override
    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            return builder.parse( new InputSource( new StringReader(getDataCiteXml())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public String getDataCiteXml() {
        return DOIDataCiteRegisterService.getMetadataFromDvObject(
                dv.getDataset().getGlobalId().asString(), new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
    }
    
    /**
     * Needs a better implementation, as it should replace the deprecated method.
     * This will trigger all sorts of N+1 query expansions, it would be much better to put the
     * lookup in a factory method instead of on-demand when the exporter requests it.
     * It does not at all filter anything as may be requested.
     */
    @Override
    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
        return dv.getFileMetadatas()
            .stream()
            .map(fileMetadata -> {
                DataFile dataFile = fileMetadata.getDataFile();
                return JsonPrinter.json(dataFile, fileMetadata, true).build();
            });
    }
    
    /**
     * Needs a better implementation, as it should replace the deprecated method.
     * This will trigger all sorts of N+1 query expansions, it would be much better to put the
     * lookup in a factory method instead of on-demand when the exporter requests it.
     * It does not at all filter anything as may be requested.
     */
    @Override
    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
        return dv.getFileMetadatas().subList(pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit())
            .stream()
            .map(fileMetadata -> {
                DataFile dataFile = fileMetadata.getDataFile();
                return JsonPrinter.json(dataFile, fileMetadata, true).build();
            });
    }
    
    @Override
    public JsonArray getDatasetFileDetails() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
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

    public void setPrerequisiteInputStream(InputStream prereqStream) {
        this.is=prereqStream;
    }
}
