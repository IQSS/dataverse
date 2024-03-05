package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DatasetFieldServiceApiTest {

    @Mock
    private ActionLogServiceBean actionLogSvc;

    @Mock
    private MetadataBlockServiceBean metadataBlockService;

    @Mock
    private DataverseServiceBean dataverseService;

    @Mock
    private DatasetFieldServiceBean datasetFieldService;

    @Mock
    private ControlledVocabularyValueServiceBean controlledVocabularyValueService;

    private DatasetFieldServiceApi api;

    @BeforeEach
    public void setup(){
        api = new DatasetFieldServiceApi();
        api.actionLogSvc = actionLogSvc;
        api.metadataBlockService = metadataBlockService;
        api.dataverseService = dataverseService;
        api.datasetFieldService = datasetFieldService;
        api.controlledVocabularyValueService = controlledVocabularyValueService;
    }

    @Test
    public void testArrayIndexOutOfBoundMessageBundle() {
        List<String> arguments = new ArrayList<>();
        arguments.add("DATASETFIELD");
        arguments.add(String.valueOf(5));
        arguments.add("watermark");
        arguments.add(String.valueOf(4 + 1));

        String bundle = "api.admin.datasetfield.load.ArrayIndexOutOfBoundMessage";
        String message = BundleUtil.getStringFromBundle(bundle, arguments);
        assertEquals(
            "Error parsing metadata block in DATASETFIELD part, line #5: missing 'watermark' column (#5)",
            message
        );
    }

    @Test
    public void testGeneralErrorMessageBundle() {
        List<String> arguments = new ArrayList<>();
        arguments.add("DATASETFIELD");
        arguments.add(String.valueOf(5));
        arguments.add("some error message");
        String bundle = "api.admin.datasetfield.load.GeneralErrorMessage";
        String message = BundleUtil.getStringFromBundle(bundle, arguments);
        assertEquals(
            "Error parsing metadata block in DATASETFIELD part, line #5: some error message",
            message
        );
    }

    @Test
    public void testGetArrayIndexOutOfBoundMessage() {
        DatasetFieldServiceApi api = new DatasetFieldServiceApi();
        String message = api.getArrayIndexOutOfBoundMessage(DatasetFieldServiceApi.HeaderType.DATASETFIELD, 5, 4);
        assertEquals(
            "Error parsing metadata block in DATASETFIELD part, line #5: missing 'watermark' column (#5)",
            message
        );
    }

    @Test
    public void testGetGeneralErrorMessage() {
        DatasetFieldServiceApi api = new DatasetFieldServiceApi();
        String message = api.getGeneralErrorMessage(DatasetFieldServiceApi.HeaderType.DATASETFIELD, 5, "some error");
        assertEquals(
            "Error parsing metadata block in DATASETFIELD part, line #5: some error",
            message
        );
    }

    @Test
    public void testGetGeneralErrorMessageEmptyHeader() {
        DatasetFieldServiceApi api = new DatasetFieldServiceApi();
        String message = api.getGeneralErrorMessage(null, 5, "some error");
        assertEquals(
                "Error parsing metadata block in unknown part, line #5: some error",
                message
        );
    }

    @Test
    public void testLoadDatasetFieldsWhitespaceTrimming() {

        Path resourceDirectory = Paths.get("src", "test", "resources", "tsv", "whitespace-test.tsv");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();

        File testfile = new File(absolutePath);
        Response response = api.loadDatasetFields(testfile);
        assertEquals(200, response.getStatus());

        JsonReader jsonReader = Json.createReader(new StringReader(response.getEntity().toString()));
        JsonObject jsonObject = jsonReader.readObject();

        final List<String> metadataNames = jsonObject.getJsonObject("data").getJsonArray("added")
                .getValuesAs(e -> e.asJsonObject().getString("name"));
        assertThat(metadataNames).contains("crc990time")
                .contains("crc990time_when")
                .contains("crc990time_standardFormat")
                .contains("crc990time_startDate")
                .contains("crc990time_endDate");
        assertThat(metadataNames).doesNotContain("  crc990time  ")
                .doesNotContain("crc990time_when  ")
                .doesNotContain("  crc990time_endDate");
    }
}
