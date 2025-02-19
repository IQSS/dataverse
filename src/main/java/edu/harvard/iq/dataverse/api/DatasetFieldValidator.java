package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.List;
import java.util.StringJoiner;

public class DatasetFieldValidator {

    public static String validate(List<DatasetField> fields) {
        StringJoiner errors = new StringJoiner(" ");

        for (DatasetField dsf : fields) {
            String fieldName = dsf.getDatasetFieldType().getDisplayName();

            if (isEmptyMultipleValue(dsf)) {
                errors.add(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredMultipleValueForField", List.of(fieldName)));
            } else if (!dsf.getDatasetFieldType().isAllowMultiples()) {
                if (isEmptyControlledVocabulary(dsf)) {
                    errors.add(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredControlledVocabularyValueForField", List.of(fieldName)));
                } else if (isEmptyCompoundValue(dsf)) {
                    errors.add(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredCompoundValueForField", List.of(fieldName)));
                } else if (isEmptySingleValue(dsf)) {
                    errors.add(BundleUtil.getStringFromBundle("datasetFieldValidator.error.emptyRequiredSingleValueForField", List.of(fieldName)));
                }
            }
        }

        return errors.length() > 0 ? errors.toString() : "";
    }

    private static boolean isEmptyMultipleValue(DatasetField dsf) {
        return dsf.getDatasetFieldType().isAllowMultiples() &&
                dsf.getControlledVocabularyValues().isEmpty() &&
                dsf.getDatasetFieldCompoundValues().isEmpty() &&
                dsf.getDatasetFieldValues().isEmpty();
    }

    private static boolean isEmptyControlledVocabulary(DatasetField dsf) {
        return dsf.getDatasetFieldType().isControlledVocabulary() &&
                dsf.getSingleControlledVocabularyValue().getStrValue().isEmpty();
    }

    private static boolean isEmptyCompoundValue(DatasetField dsf) {
        return dsf.getDatasetFieldType().isCompound() &&
                dsf.getDatasetFieldCompoundValues().isEmpty();
    }

    private static boolean isEmptySingleValue(DatasetField dsf) {
        return !dsf.getDatasetFieldType().isControlledVocabulary() &&
                !dsf.getDatasetFieldType().isCompound() &&
                dsf.getSingleValue().getValue().isEmpty();
    }
}
