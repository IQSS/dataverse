package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ExternalToolServiceBeanTest {

    @InjectMocks
    ExternalToolServiceBean externalToolServiceBean;

    @Mock
    private DataFileServiceBean datafileService;
    @Mock
    private AuthenticationServiceBean authService;
    @Mock
    private ExternalToolHandler externalToolHandler;
    @Mock
    private DataverseSession session;
    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DataverseRequestServiceBean dvRequestService;
    @Mock
    private WorldMapPermissionHelper worldMapPermissionHelper;
    @Mock
    private CitationFactory citationFactory;

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @DisplayName("Should show explore tools for ingested files only if they are public")
    @CsvSource({"false, false, false, 0",
                "true, true, false, 0",
                "true, false, true, 0",
                "true, false, false, 1"})
    void findExternalToolsByFileAndVersion(boolean released, boolean embargoed, boolean restricted, int expectedSize) {
        // given
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(released ? DatasetVersion.VersionState.RELEASED : DatasetVersion.VersionState.DRAFT);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);

        Calendar futureEmbargoExpirationDate = Calendar.getInstance();
        futureEmbargoExpirationDate.setTime(new Date());
        futureEmbargoExpirationDate.add(Calendar.DAY_OF_MONTH, 1);
        dataset.setEmbargoDate(embargoed ? futureEmbargoExpirationDate.getTime() : null);

        FileMetadata metadata = new FileMetadata();
        metadata.setDatasetVersion(datasetVersion);
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(metadata);
        dataFile.setOwner(dataset);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setRestrictType(restricted ? FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION : null);
        metadata.setTermsOfUse(termsOfUse);

        dataFile.setFileMetadatas(metadataList);
        dataFile.setDataTable(new DataTable());

        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");

        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        ExternalTool externalTool = new ExternalTool("displayName", "description", type, "http://foo.com", "{}", TextMimeType.TSV_ALT.getMimeValue());
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);

        // when
        List<ExternalTool> availableExternalTools
                = externalToolServiceBean.findExternalToolsByFileAndVersion(externalTools, dataFile, datasetVersion);

        // then
        assertThat(availableExternalTools).hasSize(expectedSize);
    }

    @Test
    void parseAddExternalToolManifest() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
            .add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                         .add("fileid", "{fileId}")
                         .build())
                .add(Json.createObjectBuilder()
                         .add("key", "{apiToken}")
                         .build())
                .build())
            .build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);

        // then
        assertThat(externalTool.getDisplayName()).isEqualTo("AwesomeTool");
        assertThat(externalTool.getDescription()).isEqualTo("This tool is awesome.");
        assertThat(externalTool.getType()).isEqualTo(ExternalTool.Type.EXPLORE);
        assertThat(externalTool.getToolUrl()).isEqualTo("http://awesometool.com");
        assertThat(externalTool.getToolParameters()).isEqualTo("{\"queryParameters\":[{\"fileid\":\"{fileId}\"},{\"key\":\"{apiToken}\"}]}");
        assertThat(externalTool.getContentType()).isEqualTo(TextMimeType.TSV_ALT.getMimeValue());
    }

    @Test
    void parseAddExternalToolManifest__noFileId() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder()
            .add("queryParameters", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                         .add("key", "{apiToken}")
                         .build())
                .build())
            .build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
            assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Required reserved word not found: {fileId}");
    }

    @Test
    void parseAddExternalToolManifest__null() {
        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External tool manifest was null or empty!");
    }

    @Test
    void parseAddExternalToolManifest__emptyString() {
        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External tool manifest was null or empty!");
    }

    @Test
    void parseAddExternalToolManifest__unknownReservedWord() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
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
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown reserved word: mode1");
    }

    @Test
    void parseAddExternalToolManifest__noDisplayName() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("description", "This tool is awesome.");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayName is required.");
    }

    @Test
    void parseAddExternalToolManifest__noDescription() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description is required.");
    }

    @Test
    void parseAddExternalToolManifest__noToolUrl() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("toolUrl is required.");
    }

    @Test
    void parseAddExternalToolManifest__wrongType() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "noSuchType");
        job.add("toolUrl", "http://awesometool.com");
        job.add("toolParameters", Json.createObjectBuilder().build());
        job.add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = job.build().toString();

        // when & then
        assertThatThrownBy(() -> ExternalToolServiceBean.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type must be one of these values: [explore, configure].");
    }

    @Test
    void parseAddExternalToolManifest__noContentType() {
        // given
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("displayName", "AwesomeTool");
        job.add("description", "This tool is awesome.");
        job.add("type", "explore");
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

        // when
        ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(tool);

        // then
        assertThat(externalTool.getContentType()).isEqualTo(TextMimeType.TSV_ALT.getMimeValue());
    }

}
