package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class GeoboxValueValidator implements FieldValidator {

    private static BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static BigDecimal MAX_LATITUDE = new BigDecimal("90");

    @Override
    public String getName() {
        return StringUtils.EMPTY;
    }

    @Override
    public ValidationResult isValid(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (field.hasNonUniqueValue()) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("validation.nonunique"));
        }
        String value = field.getSingleValue();
        if (StringUtils.isBlank(value)) {
            return ValidationResult.ok();
        }
        if (!NumberUtils.isParsable(value)) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidNumber",
                    field.getDatasetFieldType().getDisplayName()));
        }
        BigDecimal number = new BigDecimal(value);
        if (GeoboxFields.X1.is(field) || GeoboxFields.X2.is(field)) {
            return number.abs().compareTo(MAX_LONGITUDE) <= 0
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.longitude"));
        } else {
            return number.abs().compareTo(MAX_LATITUDE) <= 0
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.latitude"));
        }
    }
}
