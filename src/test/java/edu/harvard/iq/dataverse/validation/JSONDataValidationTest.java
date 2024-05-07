package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

public class JSONDataValidationTest {

    @Mock
    static DatasetFieldServiceBean datasetFieldServiceMock;
    @Mock
    static DatasetFieldType datasetFieldTypeMock;
    static ControlledVocabularyValue cvv = new ControlledVocabularyValue();
    static String rawSchema() {
        return """
         {
            "$schema": "http://json-schema.org/draft-04/schema#",
            "$defs": {
            "field": {
                "type": "object",
                "required": ["typeClass", "multiple", "typeName"],
                "properties": {
                    "value": {
                        "anyOf": [
                            {
                                "type": "array"
                            },
                            {
                                "type": "string"
                            },
                            {
                                "$ref": "#/$defs/field"
                            }
                        ]
                    },
                    "typeClass": {
                        "type": "string"
                    },
                    "multiple": {
                        "type": "boolean"
                    },
                    "typeName": {
                        "type": "string"
                    }
                }
            }
        },
        "type": "object",
        "properties": {
            "datasetVersion": {
                "type": "object",
                "properties": {
                   "license": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string"
                            },
                            "uri": {
                                "type": "string",
                                "format": "uri"
                           }
                        },
                        "required": ["name", "uri"]
                    },
                    "metadataBlocks": {
                        "type": "object",
                       "properties": {
                                   "citation": {
                                    "type": "object",
                                    "properties": {
                                        "fields": {
                                            "type": "array",
                                            "items": {
                                                "$ref": "#/$defs/field"
                                            },
                                            "minItems": 5,
                                            "allOf": [
                                                {
                                                    "contains": {
                                                        "properties": {
                                                            "typeName": {
                                                                "const": "title"
                                                            }
                                                        }
                                                    }
                                                },
                                                {
                                                    "contains": {
                                                        "properties": {
                                                            "typeName": {
                                                                "const": "author"
                                                            }
                                                        }
                                                    }
                                                },
                                                {
                                                    "contains": {
                                                        "properties": {
                                                            "typeName": {
                                                                "const": "datasetContact"
                                                            }
                                                        }
                                                    }
                                                },
                                                {
                                                    "contains": {
                                                        "properties": {
                                                            "typeName": {
                                                                "const": "dsDescription"
                                                            }
                                                        }
                                                    }
                                                },
                                                {
                                                    "contains": {
                                                        "properties": {
                                                            "typeName": {
                                                                "const": "subject"
                                                            }
                                                        }
                                                    }
                                                }
                                            ]
                                        }
                                    },
                                    "required": ["fields"]
                                }
                             },
                            "required": ["citation"]
                        }
                    },
                    "required": ["metadataBlocks"]
                }
            },
            "required": ["datasetVersion"]
        }
        """;
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
                               "value": {
                                  "dsDescriptionValue":{
                                   "value":   "Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.",
                                   "multiple": false,
                                  "typeClass": "primitive",
                                  "typeName": "dsDescriptionValue"
                               }},
                               "typeClass": "compound",
                               "multiple": false,
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
    }
    @Test
    public void testGoodJson() {
        Schema schema = SchemaLoader.load(new JSONObject(rawSchema()));
        JSONDataValidation.validate(schema, jsonInput());
    }
    @Test
    public void testBadJson() {
        Schema schema = SchemaLoader.load(new JSONObject(rawSchema()));
        try {
            JSONDataValidation.validate(schema, jsonInput().replace("\"Social Sciences\"", "\"Social Sciences\",\"Bad\""));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

        try {
            // test multiple = false but value is list
            JSONDataValidation.validate(schema, jsonInput().replaceAll("true", "false"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        // verify that child objects are also validated
        String childTest = "\"multiple\": false, \"typeName\": \"authorAffiliation\"";
        try {
            String trimmedStr = jsonInput().replaceAll("\\s{2,}", " ");
            // test child object with multiple set to true
            JSONDataValidation.validate(schema, trimmedStr.replace(childTest, childTest.replace("false", "true")));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test dsDescription but dsDescriptionValue missing
            JSONDataValidation.validate(schema, jsonInput().replace("typeName\": \"dsDescriptionValue", "typeName\": \"notdsDescriptionValue"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        try {
            // test dsDescription but child dsDescriptionValue missing
            JSONDataValidation.validate(schema, jsonInput().replace("dsDescriptionValue\":{", "notdsDescriptionValue\":{"));
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }
}
