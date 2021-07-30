package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HarvestedJsonParserTest {

    @InjectMocks
    private HarvestedJsonParser harvestedJsonParser = new HarvestedJsonParser();

    @Mock
    private DatasetFieldServiceBean datasetFieldSvc;

    @BeforeEach
    public void setUp() {
        ControlledVocabularyValue value = new ControlledVocabularyValue();
        value.setStrValue("Medicine, Health and Life Sciences");
        value.setId(10L);
        ControlledVocabularyValue value2 = new ControlledVocabularyValue();
        value2.setStrValue("Engineering");
        value2.setId(9L);

        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(value, value2);
    }

    @Test
    public void parseCVValue_properMultiValue() throws JsonParseException {
        // given

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Medicine, Health and Life Sciences\", \n" +
                "              \"Engineering\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(true);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(2 , values.size());
        assertTrue(values.stream()
                .map(ControlledVocabularyValue::getStrValue)
                .collect(Collectors.toList()).containsAll(Arrays.asList("Medicine, Health and Life Sciences", "Engineering")));
    }

    @Test
    public void parseCVValue_duplicatedValues() throws JsonParseException {
        // given
        ControlledVocabularyValue value2 = new ControlledVocabularyValue();
        value2.setStrValue("Engineering");
        value2.setId(9L);

        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(value2);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Engineering\", \n" +
                "              \"Engineering\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(true);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(1 , values.size());
        assertEquals("Engineering", values.get(0).getStrValue());
    }

    @Test
    public void parseCVValue_multiFieldExpectedButSingleValueReceived() throws JsonParseException {
        // given

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\": \"Medicine, Health and Life Sciences\"\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(true);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(1 , values.size());
        assertEquals("Medicine, Health and Life Sciences", values.get(0).getStrValue());
    }

    @Test
    public void parseCVValue_multiFieldExpectedButSingleValueReceived_nonExistingValue() throws JsonParseException {
        // given
        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(null);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\": \"Some non existing value\"\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(true);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(0 , values.size());
    }

    @Test
    public void parseCVValue_properSingleValue() throws JsonParseException {
        // given

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\": \"Medicine, Health and Life Sciences\"\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(1 , values.size());
        assertEquals("Medicine, Health and Life Sciences", values.get(0).getStrValue());
    }

    @Test
    public void parseCVValue_singleValueExpected_multiValuesPassed() throws JsonParseException {
        // given

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Medicine, Health and Life Sciences\", \n" +
                "              \"Engineering\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(1 , values.size());
        //NOTE: since first found value is being taken we are never sure which one it will be
        assertTrue(Arrays.asList("Medicine, Health and Life Sciences", "Engineering").contains(values.stream()
                .map(ControlledVocabularyValue::getStrValue)
                .collect(Collectors.toList()).get(0)));
    }

    @Test
    public void parseCVValue_singleValueExpected_nonExistingValues() throws JsonParseException {
        // given
        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(null);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Non existing value 1\", \n" +
                "              \"Non existing value 2\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(0 , values.size());
    }

    @Test
    public void parseCVValue_singleValueExpected_nonExistingValuesOrOther() throws JsonParseException {
        // given
        ControlledVocabularyValue value = new ControlledVocabularyValue();
        value.setStrValue("Other");
        value.setId(15L);

        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(null, null, value);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Non existing value 1\", \n" +
                "              \"Non existing value 1\", \n" +
                "              \"Other\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        // NOTE: if there are no values matching except for 'other', then import 'other'
        assertEquals(1 , values.size());
        assertEquals("Other", values.get(0).getStrValue());
    }

    @Test
    public void parseCVValue_singleValueExpected_ExistingValuesOrOther() throws JsonParseException {
        // given
        ControlledVocabularyValue value = new ControlledVocabularyValue();
        value.setStrValue("Other");
        value.setId(15L);
        ControlledVocabularyValue value2 = new ControlledVocabularyValue();
        value2.setStrValue("Engineering");
        value2.setId(9L);

        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(value, value, value2);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Other\", \n" +
                "              \"other\", \n" +
                "              \"Engineering\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        // NOTE: if there are matching value except for 'other', then import it instead
        assertEquals(1 , values.size());
        assertEquals("Engineering", values.get(0).getStrValue());
    }

    @Test
    public void parseCVValue_singleValueExpected_multiValuesPassed_oneMatches() throws JsonParseException {
        // given
        ControlledVocabularyValue value2 = new ControlledVocabularyValue();
        value2.setStrValue("Engineering");
        value2.setId(9L);

        Mockito.when(datasetFieldSvc.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(any(), anyString(), anyBoolean()))
                .thenReturn(null, value2, null);

        String jsonField = "{\n" +
                "            \"typeName\": \"subject\",\n" +
                "            \"multiple\": true,\n" +
                "            \"typeClass\": \"controlledVocabulary\",\n" +
                "            \"value\":\n" +
                "            [\n" +
                "              \"Non existing value 1\", \n" +
                "              \"Engineering\", \n" +
                "              \"Non existing value 2\"\n" +
                "            ]\n" +
                "          }";
        JsonReader jsonReader = Json.createReader(new StringReader(jsonField));
        JsonObject json = jsonReader.readObject();

        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(false);

        // when
        List<ControlledVocabularyValue> values = harvestedJsonParser.parseControlledVocabularyValue(fieldType, json);

        // then
        assertEquals(1 , values.size());
        //NOTE: since first found value is being taken we are never sure which one it will be
        assertEquals("Engineering", values.get(0).getStrValue());
    }
}
