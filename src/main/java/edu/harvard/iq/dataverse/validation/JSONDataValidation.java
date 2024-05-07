package edu.harvard.iq.dataverse.validation;

import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.enterprise.inject.spi.CDI;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JSONDataValidation {
    private static DatasetFieldServiceBean datasetFieldService = null;
    private static Map<String,List<String>> schemaDTOMap = new ConcurrentHashMap<>();

    /**
     *
     * @param schema Schema file defining the JSON objects to be validated
     * @param jsonInput JSON string to validate against the schema
     * @throws ValidationException
     */
    public static void validate(final Schema schema, String jsonInput) throws ValidationException {
        if (datasetFieldService == null) {
            datasetFieldService = CDI.current().select(DatasetFieldServiceBean.class).get();
        }
        if (schemaDTOMap.isEmpty()) {
            // TODO: load from a config file
            schemaDTOMap.put("datasetContact", Collections.EMPTY_LIST);
            schemaDTOMap.put("datasetContact.required", List.of("datasetContactName"));
            schemaDTOMap.put("datasetContact.allowed", List.of("datasetContactName", "datasetContactEmail","datasetContactAffiliation"));
            schemaDTOMap.put("dsDescription", Collections.EMPTY_LIST);
            schemaDTOMap.put("dsDescription.required", List.of("dsDescriptionValue"));
            schemaDTOMap.put("dsDescription.allowed", List.of("dsDescriptionValue", "dsDescriptionDate"));
        }
        JsonNode node = new JsonNode(jsonInput);
        if (node.isArray()) {
            JSONArray arrayNode = node.getArray();
            validateObject(schema, "root", arrayNode.toList());
        } else {
            node.getObject().toMap().forEach((k,v) -> {
                validateObject(schema, k, (v instanceof JSONArray) ? ((JSONArray) v).toList() : v);
            });
        }
    }

    /*
     * Validate objects recursively
     */
    private static void validateObject(final Schema schema, String key, Object value) {
        if (value instanceof Map<?,?>) {
            validateSchemaObject(schema, key, (Map) value);

            ((Map<?, ?>) value).entrySet().forEach(e -> {
                validateObject(schema, (String) e.getKey(), e.getValue());
            });
        } else if (value instanceof List) {
            ((List<?>) value).listIterator().forEachRemaining(v -> {
                validateObject(schema, key, v);
            });
        }
    }

    /*
     * Validate objects specific to a type. Currently only validating Datasets
     */
    private static void validateSchemaObject(final Schema schema, String key, Map valueMap) {
        if (schema.definesProperty("datasetVersion")) {
            validateDatasetObject(schema, key, valueMap);
        }
    }

    /*
     * Specific validation for Dataset objects
     */
    private static void validateDatasetObject(final Schema schema, String key, Map valueMap) {
        if (valueMap != null && valueMap.containsKey("typeClass")) {
            validateTypeClass(schema, key, valueMap, valueMap.get("value"), "dataset");
        }
    }

    /*
     * key: The name of the parent object
     * valueMap: Map of all the metadata of the object
     * value: The value field of the object
     * messageType: Refers to the parent: if this is an object from a dataset the messageType would be 'dataset'
     *              This needs to match the Bundle.properties for mapping the error messages when an exception occurs
     *
     *  Rules for typeClass:
     *      The contents of value depend on the field attributes
     *      if single/primitive, value is a String
     *      if multiple, value is a JsonArray
     *         multiple/primitive: each JsonArray element will contain String
     *         multiple/compound: each JsonArray element will contain Set of FieldDTOs
     */
    private static void validateTypeClass(Schema schema, String key, Map valueMap, Object value, String messageType) {

        String typeClass = valueMap.containsKey("typeClass") ? valueMap.get("typeClass").toString() : "";
        String typeName = valueMap.containsKey("typeName") ? valueMap.get("typeName").toString() : "";
        boolean multiple = Boolean.valueOf(String.valueOf(valueMap.getOrDefault("multiple", "false")));

        // make sure there is a value since 'value' is required
        if (value == null) {
            throwValidationException("value.missing", List.of(key, typeName));
        }

        if (multiple && !(value instanceof List<?>)) {
            throwValidationException("notlist.multiple", List.of(key, typeName, typeClass));
        }
        if (!multiple && value instanceof List<?>) {
            throwValidationException("list.notmultiple", List.of(key, typeName));
        }
        if ("primitive".equals(typeClass) && !multiple && !(value instanceof String)) {
            throwValidationException("type", List.of(key, typeName, typeClass));
        }
        if ("primitive".equals(typeClass) && multiple) {
            ((List<?>) value).listIterator().forEachRemaining(primitive -> {
                if (!(primitive instanceof String)) {
                    throwValidationException("type", List.of(key, typeName, typeClass));
                }
            });
        }
        if ("compound".equals(typeClass)) {
            if (multiple && value instanceof List<?>) {
                ((List<?>) value).listIterator().forEachRemaining(item -> {
                    if (!(item instanceof Map<?, ?>)) {
                        throwValidationException("compound", List.of(key, typeName, typeClass));
                    } else {
                        ((Map) item).forEach((k,val) -> {
                            if (!(val instanceof Map<?, ?>)) {
                                throwValidationException("compound", List.of(key, typeName, typeClass));
                            }
                            // validate mismatch between compound object key and typeName in value
                            String valTypeName = ((Map<?, ?>) val).containsKey("typeName") ? (String)((Map<?, ?>) val).get("typeName") : "";
                            if (!k.equals(valTypeName)) {
                                throwValidationException("compound.mismatch", List.of((String)k, valTypeName));
                            }
                            validateChildObject(schema, (String)k, val, messageType + "." + typeName,
                                    schemaDTOMap.getOrDefault(typeName+".required", Collections.EMPTY_LIST), schemaDTOMap.getOrDefault(typeName+".allowed", Collections.EMPTY_LIST));
                        });
                    }
                });
            }
        }

        if ("controlledVocabulary".equals(typeClass)) {
            DatasetFieldType dsft = datasetFieldService.findByName(typeName);
            if (value instanceof List<?>) {
                ((List<?>) value).listIterator().forEachRemaining(cvv -> {
                    if (datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(dsft, (String) cvv, true) == null) {
                        throwValidationException("dataset", "cvv.missing", List.of(key, typeName, (String) cvv));
                    }
                });
            } else {
                if (datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(dsft, (String) value, true) == null) {
                    throwValidationException("dataset", "cvv.missing", List.of(key, typeName, (String) value));
                }
            }
        }
    }

    // If value is another object or list of objects that need to be further validated then childType refers to the parent
    // Example: If this is a dsDescriptionValue from a dataset the messageType would be dataset.dsDescriptionValue
    // This needs to match the Bundle.properties for mapping the error messages when an exception occurs
    private static void validateChildObject(Schema schema, String key, Object child, String messageType, List<String> requiredFields, List<String> allowedFields) {
        if (child instanceof Map<?, ?>) {
            Map childMap = (Map<String, Object>) child;

            if (!childMap.containsKey("value")) { // if child is simple key/value where the value Map is what we really want to validate
                requiredFields.forEach(field -> {
                    if (!childMap.containsKey(field)) {
                        throwValidationException(messageType, "required.missing", List.of(key, field));
                    }
                });
                childMap.forEach((k, v) -> {
                    if (!allowedFields.isEmpty() && !allowedFields.contains(k)) {
                        throwValidationException(messageType, "invalidType", List.of(key, (String) k));
                    }
                });
                childMap.forEach((k,v) -> {
                    Map<?, ?> valueMap = (v instanceof Map<?, ?>) ? (Map<?, ?>) v : null;
                    if (valueMap == null || !k.equals(valueMap.get("typeName"))) {
                        throwValidationException(messageType, "invalidType", List.of(key, (String) k));
                    }
                    validateChildObject(schema, (String)k, v, messageType, requiredFields, allowedFields);
                });
            } else { // this child is an object with a "value" and "typeName" attribute
                String typeName = childMap.containsKey("typeName") ? childMap.get("typeName").toString() : "";
                validateTypeClass(schema, typeName, childMap, childMap.get("value"), messageType);
            }
        }
    }
    private static void throwValidationException(String key, List<String> argList) {
        throw new ValidationException(BundleUtil.getStringFromBundle("schema.validation.exception." + key, argList));
    }
    private static void throwValidationException(String type, String message, List<String> argList) {
        if (type != null) {
            throwValidationException(type + "." + message, argList);
        } else {
            throwValidationException(message, argList);
        }
    }
}
