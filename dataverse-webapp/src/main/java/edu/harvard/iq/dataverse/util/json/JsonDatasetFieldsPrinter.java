package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import io.vavr.control.Option;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Printer mainly used for {@link ExportService} and rest api.
 */
public class JsonDatasetFieldsPrinter {

    // -------------------- LOGIC --------------------

    /**
     * Printer that works the same as with old DatasetField model (backwards compatible),
     * for that reason it is became quite complicated.
     * <p></p>
     * Main idea is that compound fields with the same type are merged into one with values copied.
     * <p></p>
     * Similarly if there are multiple primitive fields with same type they are merged into one with multiple values.
     * Originally the intent was to use Jackson parser(which is much easier to use) but it is not exactly compatible with javax parser building.
     */
    public JsonArrayBuilder json(List<DatasetField> dsfFields, boolean excludeEmailFields) {
        JsonArrayBuilder fieldsArray = Json.createArrayBuilder();

        for(DatasetField dsf : dsfFields) {
            if (dsf.getDatasetFieldsChildren().size() > 1) {
                dsf.getDatasetFieldsChildren().sort(Comparator.comparing(DatasetField::getDatasetFieldTypeDisplayOrder));
            }
        }
        dsfFields.sort(Comparator.comparing(DatasetField::getDatasetFieldTypeDisplayOrder));


        for (DatasetFieldsByType fieldsByType: DatasetFieldUtil.groupByType(dsfFields)) {
            if (excludeEmailFields && FieldType.EMAIL.equals(fieldsByType.getDatasetFieldType().getFieldType())) {
                continue;
            }

            DatasetFieldType dsfType = fieldsByType.getDatasetFieldType();
            List<DatasetField> datasetFields = fieldsByType.getDatasetFields();

            if (dsfType.isControlledVocabulary()) {

                List<String> controlledVocabularyStrValues = datasetFields.stream()
                    .flatMap(dsf -> dsf.getControlledVocabularyValues().stream())
                    .sorted(ControlledVocabularyValue.DisplayOrder)
                    .map(ControlledVocabularyValue::getStrValue)
                    .collect(toList());

                JsonObjectBuilder fieldNode = prepareDatasetFieldObject(dsfType);
                attachFieldValues(dsfType, controlledVocabularyStrValues, fieldNode);

                fieldsArray.add(fieldNode);

            } else if (dsfType.isPrimitive()) {

                List<String> fieldValues = datasetFields.stream()
                    .map(dsf -> dsf.getFieldValue())
                    .filter(fieldValue -> fieldValue.isDefined())
                    .map(Option::get)
                    .collect(toList());

                JsonObjectBuilder fieldNode = prepareDatasetFieldObject(dsfType);
                attachFieldValues(dsfType, fieldValues, fieldNode);

                fieldsArray.add(fieldNode);

            } else if (dsfType.isCompound()) {

                List<JsonObject> fieldNodes = datasetFields.stream()
                    .map(datasetField -> parseChildren(excludeEmailFields, datasetField))
                    .collect(toList());


                JsonObjectBuilder fieldNode = prepareDatasetFieldObject(dsfType);
                attachFieldObjectValues(dsfType, fieldNodes, fieldNode);

                fieldsArray.add(fieldNode);
            }

        }

        return fieldsArray;
    }

    // -------------------- PRIVATE --------------------

    private JsonObjectBuilder prepareDatasetFieldObject(DatasetFieldType dsfType) {
        JsonObjectBuilder fieldNode;
        fieldNode = Json.createObjectBuilder();
        fieldNode.add("typeName", dsfType.getName());
        fieldNode.add("multiple", dsfType.isAllowMultiples());
        fieldNode.add("typeClass", typeClassString(dsfType));
        return fieldNode;
    }

    private JsonObject parseChildren(boolean excludeEmailFields, DatasetField dsf) {

        JsonObjectBuilder childKey = Json.createObjectBuilder();
        for (DatasetField dsfChild : dsf.getDatasetFieldsChildren()) {

            if (excludeEmailFields && FieldType.EMAIL.equals(dsfChild.getDatasetFieldType().getFieldType())) {
                continue;
            }

            DatasetFieldType childType = dsfChild.getDatasetFieldType();
            JsonObjectBuilder childObject = prepareDatasetFieldObject(childType);

            if (childType.isControlledVocabulary()) {
                List<ControlledVocabularyValue> controlledVocab = dsfChild.getControlledVocabularyValues();

                List<String> vocabValues = controlledVocab.stream()
                        .sorted(Comparator.comparing(ControlledVocabularyValue::getDisplayOrder))
                        .map(ControlledVocabularyValue::getStrValue)
                        .collect(toList());

                attachFieldValues(childType, vocabValues, childObject);

            } else if (childType.isPrimitive()) {

                dsfChild.getFieldValue()
                        .peek(fieldValue -> childObject.add("value", fieldValue));

            }

            childKey.add(childType.getName(), childObject);

        }

        return childKey.build();
    }

    private void attachFieldValues(DatasetFieldType datasetFieldType, List<String> fieldValues, JsonObjectBuilder valueParent) {
        if (datasetFieldType.isAllowMultiples() || fieldValues.size() > 1) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            fieldValues.forEach(v -> arrayBuilder.add(v));

            valueParent.add("value", arrayBuilder);
        } else {
            valueParent.add("value", fieldValues.get(0));
        }
    }

    private void attachFieldObjectValues(DatasetFieldType datasetFieldType, List<JsonObject> fieldObjectValues, JsonObjectBuilder valueParent) {
        if (datasetFieldType.isAllowMultiples() || fieldObjectValues.size() > 1) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            fieldObjectValues.forEach(v -> arrayBuilder.add(v));

            valueParent.add("value", arrayBuilder);
        } else {
            valueParent.add("value", fieldObjectValues.get(0));
        }
    }

    private String typeClassString(DatasetFieldType typ) {
        if (typ.isControlledVocabulary()) {
            return "controlledVocabulary";
        }
        if (typ.isCompound()) {
            return "compound";
        }
        return "primitive";
    }
}
