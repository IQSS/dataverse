package edu.harvard.iq.dataverse.api.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatasetFieldDTOFactory {

    // -------------------- LOGIC --------------------

    public static DatasetFieldDTO createPrimitive(String typeName, String value) {
        return new DatasetFieldDTO(typeName, false, DatasetFieldDTO.PRIMITIVE, value);
    }

    public static DatasetFieldDTO createMultiplePrimitive(String typeName, List<String> values) {
        return new DatasetFieldDTO(typeName, true, DatasetFieldDTO.PRIMITIVE, values);
    }

    public static DatasetFieldDTO createVocabulary(String typeName, String value) {
        return new DatasetFieldDTO(typeName, false, DatasetFieldDTO.VOCABULARY, value);
    }

    public static DatasetFieldDTO createMultipleVocabulary(String typeName, List<String> values) {
        return new DatasetFieldDTO(typeName, true, DatasetFieldDTO.VOCABULARY, values);
    }

    public static DatasetFieldDTO createCompound(String typeName, DatasetFieldDTO... fields) {
        return new DatasetFieldDTO(typeName, false, DatasetFieldDTO.COMPOUND, fieldsToMap(Arrays.stream(fields)));
    }

    public static DatasetFieldDTO createMultipleCompound(String typeName, DatasetFieldDTO... fields) {
        List<Map<String, DatasetFieldDTO>> values = new ArrayList<>();
        values.add(fieldsToMap(Arrays.stream(fields)));
        return new DatasetFieldDTO(typeName, true, DatasetFieldDTO.COMPOUND, values);
    }

    public static DatasetFieldDTO createMultipleCompound(String typeName, List<Set<DatasetFieldDTO>> fieldSets) {
        List<Map<String, DatasetFieldDTO>> values = fieldSets.stream()
                .map(fieldSet -> fieldsToMap(fieldSet.stream()))
                .collect(Collectors.toList());
        return new DatasetFieldDTO(typeName, true, DatasetFieldDTO.COMPOUND, values);
    }

    public static void embedInMetadataBlock(DatasetFieldDTO newField, MetadataBlockWithFieldsDTO metadataBlock) {
        if (metadataBlock.getFields() == null) {
            metadataBlock.setFields(new ArrayList<>());
        }
        Optional<DatasetFieldDTO> current = metadataBlock.getFields().stream()
                .filter(f -> newField.getTypeName().equals(f.getTypeName()))
                .findFirst();
        if (!current.isPresent()) {
            metadataBlock.getFields().add(newField);
            return;
        }
        DatasetFieldDTO existing = current.get();
        if (existing.getMultiple()) {
            DatasetFieldDTO temporary;
            if (DatasetFieldDTO.COMPOUND.equals(newField.getTypeClass())) {
                temporary = createMultipleCompound("",
                        mergeValues(existing.getMultipleCompound(), newField.getMultipleCompound()));
            } else if (DatasetFieldDTO.VOCABULARY.equals(newField.getTypeClass())) {
                temporary = createMultipleVocabulary("",
                        mergeValues(existing.getMultipleVocabulary(), newField.getMultipleVocabulary()));
            } else {
                temporary = createMultiplePrimitive("",
                        mergeValues(existing.getMultiplePrimitive(), newField.getMultiplePrimitive()));
            }
            existing.setValue(temporary.getValue());
        } else {
            // If field doesn't allow multiples, replace the value
            existing.setValue(newField.getValue());
        }
    }

    // -------------------- PRIVATE --------------------

    private static <T> List<T> mergeValues(List<T> existingValues, List<T> newValues) {
        List<T> merged = new ArrayList<>(existingValues);
        merged.addAll(newValues);
        return merged;
    }

    private static Map<String, DatasetFieldDTO> fieldsToMap(Stream<DatasetFieldDTO> fields) {
        return fields
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DatasetFieldDTO::getTypeName, f -> f, (prev, next) -> next, LinkedHashMap::new));
    }
}
