package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExternalToolHandlerTest {

    @InjectMocks
    private ExternalToolHandler externalToolHandler;

    @Mock
    private SystemConfig systemConfig;


    private ExternalTool externalTool;
    private DataFile dataFile;
    private ApiToken apiToken;


    @BeforeEach
    public void beforeEach() {
        when(systemConfig.getDataverseSiteUrl()).thenReturn("");

        ExternalTool.Type type = ExternalTool.Type.EXPLORE;
        String toolUrl = "http://example.com";
        externalTool = new ExternalTool("displayName", "description", type, toolUrl, "{}", TextMimeType.TSV_ALT.getMimeValue());

        dataFile = new DataFile();
        dataFile.setId(42l);
        FileMetadata fileMetadata = new FileMetadata();
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);
        fileMetadata.setDatasetVersion(datasetVersion);
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(fileMetadata);
        dataFile.setFileMetadatas(metadataList);

        apiToken = new ApiToken();
        apiToken.setTokenString("7196b5ce-f200-4286-8809-03ffdbc255d7");
    }

    // -------------------- TESTS --------------------

    @Test
    public void buildToolUrlWithQueryParams() {
        // given

        // Three query parameters, all reserved words, one is {fileId} which is required.
        externalTool.setToolParameters(Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("key1", "{fileId}"))
                        .add(Json.createObjectBuilder()
                                .add("key2", "{localeCode}"))
                        .add(Json.createObjectBuilder()
                                .add("key3", "{apiToken}")))
                .build().toString());

        // when
        String result = externalToolHandler.buildToolUrlWithQueryParams(externalTool, dataFile, apiToken, "en");

        // then
        assertThat(result).isEqualTo("http://example.com?key1=42&key2=en&key3=7196b5ce-f200-4286-8809-03ffdbc255d7");
    }

    @Test
    public void buildToolUrlWithQueryParams__NULL_EXTERNAL_TOOL() {
        // when & then
        assertThatThrownBy(() -> externalToolHandler.buildToolUrlWithQueryParams(null, dataFile, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildToolUrlWithQueryParams__NULL_DATAFILE() {
        // given
        // Two query parameters.
        externalTool.setToolParameters(Json.createObjectBuilder()
           .add("queryParameters", Json.createArrayBuilder()
               .add(Json.createObjectBuilder()
                    .add("mode", "mode1"))
               .add(Json.createObjectBuilder()
                    .add("key2", "value2")))
           .build().toString());

        // when & then
        assertThatThrownBy(() -> externalToolHandler.buildToolUrlWithQueryParams(externalTool, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildToolUrlWithQueryParams__UNKNOWN_PARAM() {
        // given

        // Two query parameters, attempt to use a reserved word that doesn't exist.
        externalTool.setToolParameters(Json.createObjectBuilder()
           .add("queryParameters", Json.createArrayBuilder()
               .add(Json.createObjectBuilder()
                    .add("key1", "{junk}"))
               .add(Json.createObjectBuilder()
                    .add("key2", "{apiToken}")))
           .build().toString());

        // when & then
        assertThatThrownBy(() -> externalToolHandler.buildToolUrlWithQueryParams(externalTool, dataFile, apiToken, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown reserved word: {junk}");
    }

    @Test
    public void buildToolUrlWithQueryParams__WITHOUT_API_KEY() {
        // given
        // Two query parameters, both reserved words, no apiToken
        externalTool.setToolParameters(Json.createObjectBuilder()
           .add("queryParameters", Json.createArrayBuilder()
               .add(Json.createObjectBuilder()
                    .add("key1", "{fileId}"))
               .add(Json.createObjectBuilder()
                    .add("key2", "{apiToken}")))
           .build().toString());

        // when
        String result = externalToolHandler.buildToolUrlWithQueryParams(externalTool, dataFile, null, null);

        // then
        assertThat(result).isEqualTo("http://example.com?key1=42");
    }
}