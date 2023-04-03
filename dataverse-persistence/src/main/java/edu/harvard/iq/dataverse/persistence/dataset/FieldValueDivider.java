package edu.harvard.iq.dataverse.persistence.dataset;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValueDivider {
    private static final FieldValueDivider EMPTY = new FieldValueDivider();

    private String sourceFieldName = StringUtils.EMPTY;
    private List<String> fieldsToCopyNames = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    private FieldValueDivider() { }

    // -------------------- LOGIC --------------------

    public static FieldValueDivider create(DatasetFieldType fieldType) {
        boolean hasDivider = fieldType.hasMetadata("divider");
        if (!hasDivider) {
            return EMPTY;
        }
        Map<String, Object> dividerData = (Map<String, Object>) fieldType.getMetadata("divider");
        FieldValueDivider divider = new FieldValueDivider();
        divider.sourceFieldName = (String) dividerData.get("source");
        divider.fieldsToCopyNames.addAll((List<String>) dividerData.get("copy"));
        return divider;
    }

    public List<DatasetField> divide(DatasetField sourceCompound, String delimiter) {
        List<DatasetField> fields = new ArrayList<>();
        DatasetField sourceField = sourceCompound.getDatasetFieldsChildren().stream()
                .filter(f -> sourceFieldName.equals(f.getTypeName()))
                .findFirst().orElse(null);
        if (sourceField == null) {
            return fields;
        }
        List<String> values = splitValue(sourceField, delimiter);
        Map<String, String> valuesToCopy = prepareValuesToCopy(sourceCompound);
        for (int i = 0; i < values.size(); i++) {
            fields.add(i == 0
                    ? sourceCompound
                    : DatasetField.createNewEmptyDatasetField(sourceCompound.getDatasetFieldType(), null));
            for (DatasetField subfield : fields.get(i).getDatasetFieldsChildren()) {
                String name = subfield.getTypeName();
                if (sourceFieldName.equals(name)) {
                    subfield.setFieldValue(values.get(i));
                } else if (valuesToCopy.containsKey(name)) {
                    subfield.setFieldValue(valuesToCopy.get(name));
                }
            }
        }
        return fields;
    }

    // -------------------- PRIVATE --------------------

    private List<String> splitValue(DatasetField sourceField, String delimiter) {
        String value = sourceField.getFieldValue().getOrElse(StringUtils.EMPTY);
        return Arrays.stream(value.split(delimiter))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private Map<String, String> prepareValuesToCopy(DatasetField sourceCompound) {
        return sourceCompound.getDatasetFieldsChildren().stream()
                .filter(f -> fieldsToCopyNames.contains(f.getTypeName())
                        && StringUtils.isNotBlank(f.getFieldValue().getOrElse(StringUtils.EMPTY)))
                .collect(Collectors.toMap(DatasetField::getTypeName, f -> f.getFieldValue().get(), (prev, next) -> next));
    }
}
