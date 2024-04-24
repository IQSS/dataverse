package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.dataaccess.AbstractRemoteOverlayAccessIO;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import java.util.ArrayList;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ExternalToolServiceBeanTest {

    private final ExternalToolServiceBean externalToolService;

    public ExternalToolServiceBeanTest() {
        this.externalToolService = new ExternalToolServiceBean();
    }

    @Test
    public void testfindAll() {
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setStorageIdentifier("test://18debaa2d7c-db98ef7d9a77");
        FileMetadata fmd = new FileMetadata();
        fmd.setId(2L);
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        List<DataTable> dataTables = new ArrayList<DataTable>();
        dataTables.add(new DataTable());
        dataFile.setDataTables(dataTables);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        List<ExternalToolType> externalToolTypes = new ArrayList<>();
        ExternalToolType externalToolType = new ExternalToolType();
        externalToolType.setType(ExternalTool.Type.EXPLORE);
        externalToolTypes.add(externalToolType);
        ExternalTool.Scope scope = ExternalTool.Scope.FILE;
        ExternalTool externalTool = new ExternalTool("displayName", "toolName", "description", externalToolTypes, scope, "http://foo.com", "{}", DataFileServiceBean.MIME_TYPE_TSV_ALT);
        URLTokenUtil externalToolHandler4 = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, null);
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);
        List<ExternalTool> availableExternalTools = externalToolService.findExternalToolsByFile(externalTools, dataFile);
        assertEquals(availableExternalTools.size(), 1);
    }

    @Test
    public void testParseAddExternalToolInput() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("toolUrl", "http://awesometool.com");
        job.add("hasPreviewMode", "false");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("fileMetadataId", "{fileMetadataId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("dvLocale", "{localeCode}")
                                .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        assertEquals("explorer", externalTool.getToolName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        FileMetadata fmd = new FileMetadata();
        fmd.setId(2L);
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, "en");
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("http://awesometool.com?fileid=42&key=7196b5ce-f200-4286-8809-03ffdbc255d7&fileMetadataId=2&dvLocale=en", toolUrl);
    }

    @Test
    public void testParseAddFileToolFilePid() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("filePid", "{filePid}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("fileMetadataId", "{fileMetadataId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("dvLocale", "{localeCode}")
                                .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        assertEquals("explorer", externalTool.getToolName());
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL,"10.5072","FK2/RMQT6J/G9F1A1", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null));
        FileMetadata fmd = new FileMetadata();
        fmd.setId(2L);
        DatasetVersion dv = new DatasetVersion();
        Dataset ds = new Dataset();
        dv.setDataset(ds);
        fmd.setDatasetVersion(dv);
        List<FileMetadata> fmdl = new ArrayList<FileMetadata>();
        fmdl.add(fmd);
        dataFile.setFileMetadatas(fmdl);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
        ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, dataFile, apiToken, fmd, "fr");
        String toolUrl = externalToolHandler.getToolUrlWithQueryParams();
        System.out.println("result: " + toolUrl);
        assertEquals("http://awesometool.com?filePid=doi:10.5072/FK2/RMQT6J/G9F1A1&key=7196b5ce-f200-4286-8809-03ffdbc255d7&fileMetadataId=2&dvLocale=fr", toolUrl);
    }

    @Test
    public void testParseAddExternalToolInputNoFileId() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("One of the following reserved words is required: {fileId}, {filePid}.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNull() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(null);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputEmptyString() {
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest("");
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("External tool manifest was null or empty!", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputUnknownReservedWord() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("fileid", "{fileId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("mode", "mode1")
                                .build())
                        .build())
                .build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("Unknown reserved word: mode1", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDisplayName() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("toolName", "dct");
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("hasPreviewMode", "false");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("displayName is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoDescription() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "dct");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("description is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoToolUrl() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("toolUrl is required.", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputWrongType() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "dct");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("noSuchType"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        System.out.println("exception: " + expectedException);
        assertEquals("Type must be one of these values: [explore, configure, preview, query].", expectedException.getMessage());
    }

    @Test
    public void testParseAddExternalToolInputNoContentType() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("fileid", "{fileId}")
                        .build())
                .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals(expectedException.getMessage(), "contentType is required.");
    }

    @Test
    public void testParseAddDatasetToolNoRequiredFields() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "dataset");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);
        Exception expectedException = null;
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        assertEquals("One of the following reserved words is required: {datasetId}, {datasetPid}.", expectedException.getMessage());
    }

    @Test
    public void testParseAddDatasetToolDatasetId() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "dataset");
        job.add("toolUrl", "http://awesometool.com");
        job.add("hasPreviewMode", "true");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("datasetId", "{datasetId}")
                        .build())
                .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);

        ExternalTool externalTool = null;
        try {
            externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        assertNotNull(externalTool);
        assertNull(externalTool.getContentType());
    }

    @Test
    public void testParseAddDatasetToolDatasetPid() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "dataset");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("datasetPid", "{datasetPid}")
                        .build())
                .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);

        ExternalTool externalTool = null;
        try {
            externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        assertNotNull(externalTool);
        assertNull(externalTool.getContentType());
    }

    /**
     * Originally, "type" accepted a single value. These days "types" accepts an
     * array of multiple values.
     */
    @Test
    public void testParseAddToolWithLegacyType() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("scope", "dataset");
        job.add("toolUrl", "http://awesometool.com");
        job.add("hasPreviewMode", "true");

        job.add("toolParameters", Json.createObjectBuilder().add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("datasetId", "{datasetId}")
                        .build())
                .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                .build())
                .build());
        String tool = job.build().toString();
        System.out.println("tool: " + tool);

        ExternalTool externalTool = null;
        try {
            externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        assertNotNull(externalTool);
        assertNull(externalTool.getContentType());
    }

    @Test
    public void testParseAddDatasetToolAllowedApiCalls() {
 
        ExternalTool externalTool = null;
        try {
            externalTool = getAllowedApiCallsTool();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        assertNotNull(externalTool);
        assertNull(externalTool.getContentType());
    }

    protected static ExternalTool getAllowedApiCallsTool() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "dataset");
        job.add("toolUrl", "http://awesometool.com");
        job.add("hasPreviewMode", "true");

        job.add("toolParameters", Json.createObjectBuilder()
                .add("httpMethod", "GET")
                .add("queryParameters",
                        Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("datasetId", "{datasetId}")
                    )
                )
            ).add("allowedApiCalls", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                .add("name", "getDataset")
                .add("httpMethod", "GET")
                .add("urlTemplate", "/api/v1/datasets/{datasetId}")
                .add("timeOut", 10))
            );
        String tool = job.build().toString();
        System.out.println("tool: " + tool);

        return ExternalToolServiceBean.parseAddExternalToolManifest(tool);
    }

    @Test
    public void testParseAddFileToolRequireAuxFile() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolName", "explorer");
        job.add("description", "This tool is awesome.");
        job.add("types", Json.createArrayBuilder().add("explore"));
        job.add("scope", "file");
        job.add("hasPreviewMode", "false");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("filePid", "{filePid}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("key", "{apiToken}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("fileMetadataId", "{fileMetadataId}")
                                .build())
                        .add(Json.createObjectBuilder()
                                .add("dvLocale", "{localeCode}")
                                .build())
                        .build())
                .build());
        job.add("requirements", Json.createObjectBuilder()
                .add("auxFilesExist", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("formatTag", "NcML")
                                .add("formatVersion", "0.1")
                        )
                )
        );
        job.add(ExternalTool.CONTENT_TYPE, DataFileServiceBean.MIME_TYPE_TSV_ALT);
        String tool = job.build().toString();
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);
        assertEquals("AwesomeTool", externalTool.getDisplayName());
        assertEquals("explorer", externalTool.getToolName());
        assertEquals("{\"auxFilesExist\":[{\"formatTag\":\"NcML\",\"formatVersion\":\"0.1\"}]}", externalTool.getRequirements());
    }

}
