package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class GeoboxLatitudeRelationValidator implements FieldValidator {

    @Override
    public String getName() {
        return StringUtils.EMPTY;
    }

    /**
     * Warning: This validator assumes that if field is filled then it's filled
     * correctly (i.e. value can be safely converted to a number).
     */
    @Override
    public ValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (!(GeoboxFields.Y1.is(field) || GeoboxFields.Y2.is(field))) {
            return ValidationResult.ok();
        }
        GeoboxFields otherCoord = GeoboxFields.Y1.is(field) ? GeoboxFields.Y2 : GeoboxFields.Y1;
        ValidatableField other = field.getParent()
                .map(ValidatableField::getChildren)
                .getOrElse(Collections.emptyList())
                .stream()
                .filter(otherCoord::is)
                .findFirst()
                .orElse(null);
        if (other == null || StringUtils.isBlank(other.getSingleValue()) || !NumberUtils.isParsable(other.getSingleValue())
                || StringUtils.isBlank(field.getSingleValue())) {
            return ValidationResult.ok();
        }
        BigDecimal y1 = new BigDecimal(otherCoord == GeoboxFields.Y2 ? field.getSingleValue() : other.getSingleValue());
        BigDecimal y2 = new BigDecimal(otherCoord == GeoboxFields.Y1 ? field.getSingleValue() : other.getSingleValue());
        return y1.compareTo(y2) <= 0
                ? ValidationResult.ok()
                : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.latitude.relation"));
    }
}
