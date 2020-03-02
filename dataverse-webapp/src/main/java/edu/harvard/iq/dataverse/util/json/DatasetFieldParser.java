package edu.harvard.iq.dataverse.util.json;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import io.vavr.control.Option;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.typeClassString;

/**
 * Parser mainly used for {@link ExportService}.
 */
public class DatasetFieldParser {

    JsonArrayBuilder fieldsArray = Json.createArrayBuilder();
    Map<DatasetFieldType, ParserDataHolder> parsedData = new HashMap<>();

    // -------------------- LOGIC --------------------

    /**
     * Parser that is parsing the same as with old DatasetField model (backwards compatible),
     * for that reason it is became quite complicated.
     * <p></p>
     * Main idea is that compound fields with the same type are merged into one with values copied.
     * <p></p>
     * Similarly if there are multiple primitive fields with same type they are merged into one with multiple values.
     * Originally the intent was to use Jackson parser(which is much easier to use) but it is not exactly compatible with javax parser building.
     * The {@link #parsedData} has to be used because since when you add values to the main fields array,
     * they cannot be modified anymore so this is a workaround.
     */
    public JsonArrayBuilder parseDatasetFields(List<DatasetField> dsfFields, boolean excludeEmailFields) {
        for (DatasetField dsf : dsfFields) {

            DatasetFieldType dsfType = dsf.getDatasetFieldType();
            JsonObjectBuilder fieldNode = prepareDatasetFieldObject(dsf, dsfType, parsedData);

            if (dsfType.isControlledVocabulary()) {
                for (ControlledVocabularyValue cvv
                        : sort(dsf.getControlledVocabularyValues(), ControlledVocabularyValue.DisplayOrder)) {

                    Option.of(parsedData.get(dsfType))
                            .peek(parserDataHolder -> parserDataHolder.getPrimitiveValues().ifPresent(values -> values.add(
                                    cvv.getStrValue())))
                            .onEmpty(() -> parsedData.put(dsfType,
                                                          new ParserDataHolder(fieldNode,
                                                                               Lists.newArrayList(cvv.getStrValue()))));
                }

            } else if (dsfType.isPrimitive()) {

                dsf.getFieldValue()
                        .peek(fieldValue -> addValueToList(fieldNode, dsfType, fieldValue, parsedData));

            } else if (dsfType.isCompound()) {
                JsonArrayBuilder dsfChildArray;

                if (parsedData.get(dsfType) == null) {
                    dsfChildArray = Json.createArrayBuilder();
                } else {
                    dsfChildArray = parsedData.get(dsfType).getChildValues().orElseGet(Json::createArrayBuilder);
                }

                parsedData.put(dsfType,
                               new ParserDataHolder(fieldNode,
                                                    parseChildren(excludeEmailFields, dsf, dsfChildArray)));
            }
        }

        transferValuesToDsfParent(parsedData).forEach(dsfParent -> fieldsArray.add(dsfParent));

        return fieldsArray;
    }

    // -------------------- PRIVATE --------------------

    private JsonObjectBuilder prepareDatasetFieldObject(DatasetField dsf, DatasetFieldType dsfType, Map<DatasetFieldType, ParserDataHolder> parsedData) {
        JsonObjectBuilder fieldNode;

        if (parsedData.containsKey(dsf.getDatasetFieldType())) {
            fieldNode = parsedData.get(dsf.getDatasetFieldType()).getParentDsf();
        } else {

            fieldNode = Json.createObjectBuilder();
            fieldNode.add("typeName", dsfType.getName());
            fieldNode.add("multiple", dsfType.isAllowMultiples());
            fieldNode.add("typeClass", typeClassString(dsfType));
        }
        return fieldNode;
    }

    private JsonArrayBuilder parseChildren(boolean excludeEmailFields, DatasetField dsf, JsonArrayBuilder dsfChildArray) {

        JsonObjectBuilder childKey = Json.createObjectBuilder();
        for (DatasetField dsfChild : dsf.getDatasetFieldsChildren()) {

            if (excludeEmailFields && FieldType.EMAIL.equals(dsfChild.getDatasetFieldType().getFieldType())) {
                continue;
            }

            JsonObjectBuilder childObject = Json.createObjectBuilder();

            DatasetFieldType childType = dsfChild.getDatasetFieldType();
            childObject.add("typeName", childType.getName());
            childObject.add("multiple", childType.isAllowMultiples());
            childObject.add("typeClass", typeClassString(childType));

            if (childType.isControlledVocabulary()) {
                List<ControlledVocabularyValue> controlledVocab = dsfChild.getControlledVocabularyValues();

                List<String> vocabValues = controlledVocab.stream()
                        .sorted(Comparator.comparing(ControlledVocabularyValue::getDisplayOrder))
                        .map(ControlledVocabularyValue::getStrValue)
                        .collect(Collectors.toList());

                parseVocabularyValues(childType, vocabValues, childObject);

            } else if (childType.isPrimitive()) {

                dsfChild.getFieldValue()
                        .peek(fieldValue -> childObject.add("value", fieldValue));

            }

            childKey.add(childType.getName(), childObject);

        }

        dsfChildArray.add(childKey);

        return dsfChildArray;
    }

    /**
     * Method used for adding values to Datasetfields,
     * it is needed because you need to add values to fields at the end of parsing,
     * otherwise there is no way to modify those values (which is needed if you have multiple fields of the same {@link DatasetFieldType}
     */
    private List<JsonObjectBuilder> transferValuesToDsfParent(Map<DatasetFieldType, ParserDataHolder> parsedData) {
        return parsedData.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getDisplayOrder()))
                .map(parserEntries -> {
                    ParserDataHolder parserData = parserEntries.getValue();
                    JsonObjectBuilder parentDsf = parserData.getParentDsf();

                    if (parserEntries.getKey().isPrimitive() && !parserEntries.getKey().isControlledVocabulary()) {

                        parsePrimitiveField(parserData, parentDsf);

                    } else if (parserEntries.getKey().isCompound()) {

                        parentDsf.add("value", parserData.getChildValues().orElseGet(Json::createArrayBuilder));
                    } else if (parserEntries.getKey().isControlledVocabulary()) {

                        parseVocabularyValues(parserEntries.getKey(),
                                              parserData.getPrimitiveValues().orElse(Lists.newArrayList()),
                                              parentDsf);

                    }

                    return parentDsf;
                })
                .collect(Collectors.toList());
    }

    private void parsePrimitiveField(ParserDataHolder parserData, JsonObjectBuilder parentDsf) {
        parserData.getPrimitiveValues().ifPresent(fieldValues -> {
            if (fieldValues.size() > 1) {
                JsonArrayBuilder parserArray = Json.createArrayBuilder();

                fieldValues.forEach(parserArray::add);
                parentDsf.add("value", parserArray);

            } else if (fieldValues.size() == 1) {
                parentDsf.add("value", fieldValues.get(0));
            }
        });
    }

    private void parseVocabularyValues(DatasetFieldType datasetFieldType, List<String> values, JsonObjectBuilder parentDsf) {
        if (datasetFieldType.isAllowMultiples()) {
            JsonArrayBuilder parserArray = Json.createArrayBuilder();

            Optional.of(values).orElseGet(ArrayList::new)
                    .forEach(parserArray::add);

            parentDsf.add("value", parserArray);
        } else {

            Optional.of(values).ifPresent(strings -> {
                parentDsf.add("value", strings.get(0));
            });
        }
    }

    private Option<ParserDataHolder> addValueToList(JsonObjectBuilder fieldNode, DatasetFieldType dsfType, String fieldValue, Map<DatasetFieldType, ParserDataHolder> parsedData) {
        return Option.of(parsedData.get(dsfType))
                .peek(parserDataHolder -> parserDataHolder.getPrimitiveValues().ifPresent(values -> values.add(
                        fieldValue)))
                .onEmpty(() -> parsedData.put(dsfType, new ParserDataHolder(fieldNode,
                                                                            Lists.newArrayList(
                                                                                    fieldValue))));
    }

    static private <T> Iterable<T> sort(List<T> list, Comparator<T> cmp) {
        ArrayList<T> tbs = new ArrayList<>(list);
        tbs.sort(cmp);
        return tbs;
    }
}
