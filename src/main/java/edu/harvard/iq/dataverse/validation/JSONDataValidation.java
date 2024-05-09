package edu.harvard.iq.dataverse.validation;

import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.enterprise.inject.spi.CDI;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JSONDataValidation {
    private static final Logger logger = Logger.getLogger(JSONDataValidation.class.getCanonicalName());
    private static DatasetFieldServiceBean datasetFieldService = null;

    /**
     *
     * @param schema Schema file defining the JSON objects to be validated
     * @param jsonInput JSON string to validate against the schema
     * @throws ValidationException
     */
    public static void validate(Schema schema, Map<String, Map<String, List<String>>> schemaChildMap, String jsonInput) throws ValidationException {
        if (datasetFieldService == null) {
            datasetFieldService = CDI.current().select(DatasetFieldServiceBean.class).get();
        }
        JsonNode node = new JsonNode(jsonInput);
        if (node.isArray()) {
            JSONArray arrayNode = node.getArray();
            validateObject(schema, schemaChildMap, "root", arrayNode.toList());
        } else {
            node.getObject().toMap().forEach((k,v) -> {
                validateObject(schema, schemaChildMap, k, (v instanceof JSONArray) ? ((JSONArray) v).toList() : v);
            });
        }
    }

    /*
     * Validate objects recursively
     */
    private static void validateObject(Schema schema, Map<String, Map<String,List<String>>> schemaChildMap, String key, Object value) {
        if (value instanceof Map<?,?>) {
            validateSchemaObject(schema, schemaChildMap, key, (Map) value);

            ((Map<?, ?>) value).entrySet().forEach(e -> {
                validateObject(schema, schemaChildMap, (String) e.getKey(), e.getValue());
            });
        } else if (value instanceof List) {
            ((List<?>) value).listIterator().forEachRemaining(v -> {
                validateObject(schema, schemaChildMap, key, v);
            });
        }
    }

    /*
     * Validate objects specific to a type. Currently only validating Datasets
     */
    private static void validateSchemaObject(Schema schema, Map<String, Map<String,List<String>>> schemaChildMap, String key, Map valueMap) {
        if (schema.definesProperty("datasetVersion")) {
            validateDatasetObject(schema, schemaChildMap, key, valueMap);
        }
    }

    /*
     * Specific validation for Dataset objects
     */
    private static void validateDatasetObject(Schema schema, Map<String, Map<String,List<String>>> schemaChildMap, String key, Map valueMap) {
        if (valueMap != null && valueMap.containsKey("typeClass")) {
            validateTypeClass(schema, schemaChildMap, key, valueMap, valueMap.get("value"), "dataset");
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
    private static void validateTypeClass(Schema schema, Map<String, Map<String,List<String>>> schemaChildMap, String key, Map valueMap, Object value, String messageType) {

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
                            String valTypeName = ((Map<?, ?>) val).containsKey("typeName") ? (String) ((Map<?, ?>) val).get("typeName") : "";
                            if (!k.equals(valTypeName)) {
                                throwValidationException("compound.mismatch", List.of((String) k, valTypeName));
                            }
                        });
                        validateChildren(schema, schemaChildMap, key, ((Map) item).values(), typeName, messageType);
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
    private static void validateChildren(Schema schema, Map<String, Map<String,List<String>>> schemaChildMap, String key, Collection<Object> children, String typeName, String messageType) {
        if (children == null || children.isEmpty()) {
            return;
        }
        List<String> requiredFields = new ArrayList<>();
        requiredFields.addAll((List)schemaChildMap.getOrDefault(typeName, Collections.EMPTY_MAP).getOrDefault("required", Collections.EMPTY_LIST));
        List<String> allowedFields = (List)schemaChildMap.getOrDefault(typeName, Collections.EMPTY_MAP).getOrDefault("allowed", Collections.EMPTY_LIST);
        children.forEach(child -> {
            if (child instanceof Map<?, ?>) {
                String childTypeName = ((Map<?, ?>) child).containsKey("typeName") ? (String)((Map<?, ?>) child).get("typeName") : "";
                if (!allowedFields.isEmpty() && !allowedFields.contains(childTypeName)) {
                    throwValidationException(messageType, "invalidType", List.of(typeName, childTypeName, allowedFields.stream().collect(Collectors.joining(", "))));
                }
                if (!requiredFields.isEmpty() && requiredFields.contains(childTypeName)) {
                    requiredFields.remove(childTypeName);
                }
            }
        });
        if (!requiredFields.isEmpty()) {
            throwValidationException(messageType, "required.missing", List.of(typeName, requiredFields.stream().collect(Collectors.joining(", ")), typeName));
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
