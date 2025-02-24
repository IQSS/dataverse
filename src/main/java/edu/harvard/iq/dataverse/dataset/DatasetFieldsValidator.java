package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.StringJoiner;

@Stateless
@Named
public class DatasetFieldsValidator {

    @Inject
    protected DatasetFieldServiceBean datasetFieldService;

    public String validateFields(List<DatasetField> fields, DatasetVersion datasetVersion) {
        StringJoiner errors = new StringJoiner(" ");

        for (DatasetField dsf : fields) {
            if (!datasetFieldService.isFieldRequiredInDataverse(dsf.getDatasetFieldType(), datasetVersion.getDataset().getOwner())) {
                continue;
            }

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

    private boolean isEmptyMultipleValue(DatasetField dsf) {
        return dsf.getDatasetFieldType().isAllowMultiples() &&
                dsf.getControlledVocabularyValues().isEmpty() &&
                dsf.getDatasetFieldCompoundValues().isEmpty() &&
                dsf.getDatasetFieldValues().isEmpty();
    }

    private boolean isEmptyControlledVocabulary(DatasetField dsf) {
        return dsf.getDatasetFieldType().isControlledVocabulary() &&
                dsf.getSingleControlledVocabularyValue().getStrValue().isEmpty();
    }

    private boolean isEmptyCompoundValue(DatasetField dsf) {
        return dsf.getDatasetFieldType().isCompound() &&
                dsf.getDatasetFieldCompoundValues().isEmpty();
    }

    private boolean isEmptySingleValue(DatasetField dsf) {
        return !dsf.getDatasetFieldType().isControlledVocabulary() &&
                !dsf.getDatasetFieldType().isCompound() &&
                dsf.getSingleValue().getValue().isEmpty();
    }
}
