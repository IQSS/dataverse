package edu.harvard.iq.dataverse.api.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author ellenk
 */
public class FieldDTO {

    private String typeName;

    private Boolean multiple;

    private String typeClass;

    /**
    * The contents of value depend on the field attributes
    * if single/primitive, value is a String
    * if multiple, value is a JSonArray
    *      multiple/primitive: each JSonArray element will contain String
    *      multiple/compound: each JSonArray element will contain Set of FieldDTOs
    */
    private JsonElement value;

    // -------------------- GETTERS --------------------

    public String getTypeName() {
        return typeName;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public String getTypeClass() {
        return typeClass;
    }

    public JsonElement getValue() {
        return value;
    }

    // -------------------- LOGIC --------------------

    public static FieldDTO createPrimitiveFieldDTO(String typeName, String value) {
        FieldDTO primitive = new FieldDTO();
        primitive.typeName = typeName;
        primitive.setSinglePrimitive(value);
        return primitive;
    }

    public static FieldDTO createMultiplePrimitiveFieldDTO(String typeName, List<String> values) {
        FieldDTO primitive = new FieldDTO();
        primitive.typeName = typeName;
        primitive.setMultiplePrimitive(values);
        return primitive;
    }

    public static FieldDTO createMultipleVocabFieldDTO(String typeName, List<String> values) {
        FieldDTO primitive = new FieldDTO();
        primitive.typeName = typeName;
        primitive.setMultipleVocab(values);
        return primitive;
    }

    public static FieldDTO createVocabFieldDTO(String typeName, String value) {
        FieldDTO field = new FieldDTO();
        field.typeName = typeName;
        field.setSingleVocab(value);
        return field;
    }

    public static FieldDTO createCompoundFieldDTO(String typeName, FieldDTO... value) {
        FieldDTO field = new FieldDTO();
        field.typeName = typeName;
        field.setSingleCompound(value);
        return field;
    }

    /**
     * Creates a FieldDTO that contains a size=1 list of compound values
     */
    public static FieldDTO createMultipleCompoundFieldDTO(String typeName, FieldDTO... value) {
        FieldDTO field = new FieldDTO();
        field.typeName = typeName;
        field.setMultipleCompound(value);
        return field;
    }

    public static FieldDTO createMultipleCompoundFieldDTO(String typeName, List<Set<FieldDTO>> compoundList) {
        FieldDTO field = new FieldDTO();
        field.typeName = typeName;
        field.setMultipleCompound(compoundList);
        return field;
    }

    public String getSinglePrimitive() {
        return value == null ? "" : value.getAsString();
    }

    public String getSingleVocab() {
        return value == null ? "" : value.getAsString();
    }

    public Set<FieldDTO> getSingleCompound() {
        Gson gson = new Gson();
        JsonObject element = (JsonObject) value;

        return element.entrySet().stream()
                .map(e -> gson.fromJson(e.getValue(), FieldDTO.class))
                .collect(Collectors.toSet());
    }

    public List<String> getMultiplePrimitive() {
        return StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
    }

    public List<String> getMultipleVocab() {
        return StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
    }

    public List<Set<FieldDTO>> getMultipleCompound() {
        Gson gson = new Gson();
        List<Set<FieldDTO>> fields = new ArrayList<>();

        for (JsonElement jsonElement : value.getAsJsonArray()) {
            JsonObject element = (JsonObject) jsonElement;
            Set<FieldDTO> elementFields = element.entrySet().stream()
                    .map(e -> gson.fromJson(e.getValue(), FieldDTO.class))
                    .collect(Collectors.toSet());
            fields.add(elementFields);
        }

        return fields;
    }

    public void setSinglePrimitive(String value) {
        Gson gson = new Gson();
        typeClass = "primitive";
        multiple = false;
        this.value = gson.toJsonTree(value, String.class);
    }

    public void setSingleVocab(String value) {
        Gson gson = new Gson();
        typeClass = "controlledVocabulary";
        multiple = false;
        this.value = gson.toJsonTree(value);
    }

    public void setSingleCompound(FieldDTO... compoundField) {
        Gson gson = new Gson();
        this.typeClass = "compound";
        this.multiple = false;

        JsonObject obj = new JsonObject();
        for (FieldDTO fieldDTO : compoundField) {
            if (fieldDTO != null) {
                obj.add(fieldDTO.typeName, gson.toJsonTree(fieldDTO, FieldDTO.class));
            }
        }

        this.value = obj;
    }

    public void setMultiplePrimitive(String[] value) {
        Gson gson = new Gson();
        typeClass = "primitive";
        multiple = true;
        this.value = gson.toJsonTree(Arrays.asList(value));
    }

    public void setMultiplePrimitive(List<String> value) {
        Gson gson = new Gson();
        typeClass = "primitive";
        multiple = true;
        this.value = gson.toJsonTree(value);
    }

    public void setMultipleVocab(List<String> value) {
        Gson gson = new Gson();
        typeClass = "controlledVocabulary";
        multiple = true;
        this.value = gson.toJsonTree(value);
    }

    /**
     * Set value to a size 1 list of compound fields, that is a single set of primitives
     */
    public void setMultipleCompound(FieldDTO... fieldList) {
        Gson gson = new Gson();
        this.typeClass = "compound";
        this.multiple = true;

        List<Map<String, FieldDTO>> mapList = new ArrayList<>();
        Map<String, FieldDTO> fieldMap = Arrays.stream(fieldList)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(f -> f.typeName, f -> f, (prev, next) -> next));
        mapList.add(fieldMap);

        this.value = gson.toJsonTree(mapList);

    }

    /**
     * Set value to a list of compound fields (each member of the list is a set of fields)
     */
    public void setMultipleCompound(List<Set<FieldDTO>> compoundFieldList) {
        Gson gson = new Gson();
        this.typeClass = "compound";
        this.multiple = true;

        List<Map<String, FieldDTO>> mapList = new ArrayList<>();
        for (Set<FieldDTO> compoundField : compoundFieldList) {
            Map<String, FieldDTO> fieldMap = new HashMap<>();
            for (FieldDTO fieldDTO : compoundField) {
                fieldMap.put(fieldDTO.typeName, fieldDTO);
            }
            mapList.add(fieldMap);
        }
        this.value = gson.toJsonTree(mapList);

    }

    /**
     * @return the value of this field, translated from a JsonElement to the
     * appropriate DTO format
     */
    public Object getConvertedValue() {
        if (multiple) {
            if (typeClass.equals("compound")) {
                return getMultipleCompound();
            } else if (typeClass.equals("controlledVocabulary")) {
                return getMultipleVocab();
            } else {
                return getMultiplePrimitive();
            }
        } else {
            if (typeClass.equals("compound")) {
                return getSingleCompound();
            } else if (typeClass.equals("controlledVocabulary")) {
                return getSingleVocab();
            } else {
                return getSinglePrimitive();
            }
        }
    }

    // -------------------- SETTERS --------------------

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public void setValue(JsonElement value) {
        this.value = value;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.typeName);
        hash = 53 * hash + Objects.hashCode(this.multiple);
        hash = 53 * hash + Objects.hashCode(this.typeClass);
        hash = 53 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FieldDTO other = (FieldDTO) obj;
        if (!Objects.equals(this.typeName, other.typeName)) {
            return false;
        }
        if (!Objects.equals(this.multiple, other.multiple)) {
            return false;
        }
        if (!Objects.equals(this.typeClass, other.typeClass)) {
            return false;
        }
        return Objects.equals(this.value, other.value);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "FieldDTO{" + "typeName=" + typeName
                + ", multiple=" + multiple
                + ", typeClass=" + typeClass
                + ", value=" + getConvertedValue() + '}';
    }
}
