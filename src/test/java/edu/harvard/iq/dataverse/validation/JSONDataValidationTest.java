package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

public class JSONDataValidationTest {

    @Mock
    static DatasetFieldServiceBean datasetFieldServiceMock;
    @Mock
    static DatasetFieldType datasetFieldTypeMock;
    static ControlledVocabularyValue cvv = new ControlledVocabularyValue();
    static Map<String, Map<String, List<String>>> schemaChildMap = new HashMap<>();

    static JSONObject rawSchema = null;
    static JSONObject rawSchema() throws IOException {
        if (rawSchema == null) {
                Path file = Path.of("doc/sphinx-guides/source/_static/api/dataset-schema.json");
                String schema = Files.readString(file, StandardCharsets.UTF_8);
                rawSchema = new JSONObject(schema);
        }
        return rawSchema;
    }

    static String jsonInput() {
        return """
                   {
                   "datasetVersion": {
                       "license": {
                         "name": "CC0 1.0",
                         "uri": "http://creativecommons.org/publicdomain/zero/1.0"
                       },
                       "metadataBlocks": {
                         "citation": {
                           "fields": [
                             {
                               "value": "Darwin's Finches",
                               "typeClass": "primitive",
                               "multiple": false,
                               "typeName": "title"
                             },
                             {
                               "value": [
                                 {
                                   "authorName": {
                                     "value": "Finch, Fiona",
                                     "typeClass": "primitive",
                                     "multiple": false,
                                     "typeName": "authorName"
                                   },
                                   "authorAffiliation": {
                                     "value": "Birds Inc.",
                                     "typeClass": "primitive",
                                     "multiple": false,
                                     "typeName": "authorAffiliation"
                                   }
                                 }
                               ],
                               "typeClass": "compound",
                               "multiple": true,
                               "typeName": "author"
                             },
                             {
                               "value": [
                                   { "datasetContactEmail" : {
                                       "typeClass": "primitive",
                                       "multiple": false,
                                       "typeName": "datasetContactEmail",
                                       "value" : "finch@mailinator.com"
                                   },
                                   "datasetContactName" : {
                                       "typeClass": "primitive",
                                       "multiple": false,
                                       "typeName": "datasetContactName",
                                       "value": "Finch, Fiona"
                                   }
                               }],
                               "typeClass": "compound",
                               "multiple": true,
                               "typeName": "datasetContact"
                             },
                             {
                               "value": [{
                                  "dsDescriptionValue":{
                                    "value":   "Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.",
                                    "multiple": false,
                                    "typeClass": "primitive",
                                    "typeName": "dsDescriptionValue"
                                  },
                                  "dsDescriptionDate": {
                                     "typeName": "dsDescriptionDate",
                                     "multiple": false,
                                     "typeClass": "primitive",
                                     "value": "2021-07-13"
                                   }
                                }],
                               "typeClass": "compound",
                               "multiple": true,
                               "typeName": "dsDescription"
                              },
                             {
                               "value": [
                                 "Medicine, Health and Life Sciences",
                                 "Social Sciences"
                               ],
                               "typeClass": "controlledVocabulary",
                               "multiple": true,
                               "typeName": "subject"
                             }
                           ],
                           "displayName": "Citation Metadata"
                         }
                       }
                     }
                   }
                """;
    }

    @BeforeAll
    static void setup() throws NoSuchFieldException, IllegalAccessException {
        datasetFieldServiceMock = Mockito.mock(DatasetFieldServiceBean.class);
        datasetFieldTypeMock = Mockito.mock(DatasetFieldType.class);
        Field datasetFieldServiceField = JSONDataValidation.class.getDeclaredField("datasetFieldService");
        datasetFieldServiceField.setAccessible(true);
        datasetFieldServiceField.set(JSONDataValidation.class, datasetFieldServiceMock);

        Mockito.when(datasetFieldServiceMock.findByName(any(String.class))).thenReturn(datasetFieldTypeMock);
        List<String> cvvList = List.of("Medicine, Health and Life Sciences", "Social Sciences");
        cvvList.forEach(i -> {
            Mockito.when(datasetFieldServiceMock.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(datasetFieldTypeMock, i,true)).thenReturn(cvv);
        });
        Mockito.when(datasetFieldServiceMock.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(datasetFieldTypeMock, "Bad",true)).thenReturn(null);

        Map<String, List<String>> datasetContact = new HashMap<>();
        datasetContact.put("required", List.of("datasetContactName"));
        datasetContact.put("allowed", List.of("datasetContactName", "datasetContactEmail","datasetContactAffiliation"));
        schemaChildMap.put("datasetContact",datasetContact);
        Map<String, List<String>> dsDescription = new HashMap<>();
        dsDescription.put("required", List.of("dsDescriptionValue"));
        dsDescription.put("allowed", List.of("dsDescriptionValue", "dsDescriptionDate"));
        schemaChildMap.put("dsDescription",dsDescription);

    }
    @Test
    public void testSchema() throws IOException {
        Schema schema = SchemaLoader.load(rawSchema());
        schema.validate(new JSONObject(jsonInput()));
    }
    @Test
    public void testValid() throws IOException {
        Schema schema = SchemaLoader.load(rawSchema());
        JSONDataValidation.validate(schema, schemaChildMap, jsonInput());
    }
    @Test
    public void testInvalid() throws IOException {
        Schema schema = SchemaLoader.load(rawSchema());
        try {
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replace("\"Social Sciences\"", "\"Social Sciences\",\"Bad\""));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

        try {
            // test multiple = false but value is list
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replaceAll("true", "false"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        // verify that child objects are also validated
        String childTest = "\"multiple\": false, \"typeName\": \"authorAffiliation\"";
        try {
            String trimmedStr = jsonInput().replaceAll("\\s{2,}", " ");
            // test child object with multiple set to true
            JSONDataValidation.validate(schema, schemaChildMap, trimmedStr.replace(childTest, childTest.replace("false", "true")));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test dsDescription but dsDescriptionValue missing
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replace("typeName\": \"dsDescriptionValue", "typeName\": \"notdsDescriptionValue"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test dsDescription but child dsDescriptionValue missing
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replace("dsDescriptionValue\":{", "notdsDescriptionValue\":{"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test required dataType missing
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replaceAll("\"datasetContactName\"", "\"datasetContactAffiliation\""));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test dataType not allowed
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput().replaceAll("\"datasetContactEmail\"", "\"datasetContactNotAllowed\""));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }
}
