package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
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
    public ValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (field.hasNonUniqueValue()) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("validation.nonunique"));
        }
        List<? extends ValidatableField> allGeoboxFields = field.getParent()
                .map(ValidatableField::getChildren)
                .getOrElse(Collections.emptyList());

        return allGeoboxFields.stream().allMatch(f -> StringUtils.isBlank(f.getSingleValue()))
                || StringUtils.isNotBlank(field.getSingleValue())
                ? ValidationResult.ok()
                : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.empty"));
    }
}
