package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
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
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        if (!(GeoboxFields.Y1.is(field) || GeoboxFields.Y2.is(field))) {
            return ValidationResult.ok();
        }
        GeoboxFields otherCoord = GeoboxFields.Y1.is(field) ? GeoboxFields.Y2 : GeoboxFields.Y1;
        DatasetField other = field.getDatasetFieldParent()
                .map(DatasetField::getDatasetFieldsChildren)
                .getOrElse(Collections.emptyList())
                .stream()
                .filter(otherCoord::is)
                .findFirst()
                .orElse(null);
        if (other == null || StringUtils.isBlank(other.getValue()) || !NumberUtils.isParsable(other.getValue())
                || StringUtils.isBlank(field.getValue())) {
            return ValidationResult.ok();
        }
        BigDecimal y1 = new BigDecimal(otherCoord == GeoboxFields.Y2 ? field.getValue() : other.getValue());
        BigDecimal y2 = new BigDecimal(otherCoord == GeoboxFields.Y1 ? field.getValue() : other.getValue());
        return y1.compareTo(y2) <= 0
                ? ValidationResult.ok()
                : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.invalid.latitude.relation"));
    }
}
