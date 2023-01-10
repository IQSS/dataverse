package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class GeoboxFillValidator implements FieldValidator {

    @Override
    public String getName() {
        return StringUtils.EMPTY;
    }

    @Override
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        List<DatasetField> allGeoboxFields = field.getDatasetFieldParent()
                .map(DatasetField::getDatasetFieldsChildren)
                .getOrElse(Collections.emptyList());

        return allGeoboxFields.stream().allMatch(f -> StringUtils.isBlank(f.getValue()))
                || StringUtils.isNotBlank(field.getValue())
                ? ValidationResult.ok()
                : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.empty"));
    }
}
