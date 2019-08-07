package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * @author gdurand
 */
public class DatasetFieldValidator implements ConstraintValidator<ValidateDatasetFieldType, DatasetField> {

    @Override
    public void initialize(ValidateDatasetFieldType constraintAnnotation) {
    }

    @Override
    public boolean isValid(DatasetField value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation(); // we do this so we can have different messages depending on the different issue

        DatasetFieldType dsfType = value.getDatasetFieldType();
        //SEK Additional logic turns off validation for templates
        if (isTemplateDatasetField(value)) {
            return true;
        }
        if (((dsfType.isPrimitive() && dsfType.isRequired()) || (dsfType.isPrimitive() && value.isRequired()))
                && StringUtils.isBlank(value.getValue())) {
            try {
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle("isrequired")).addConstraintViolation();
            } catch (NullPointerException npe) {
                //if there's no context for the error we can't put it anywhere....
            }

            return false;
        }
        return true;
    }

    private boolean isTemplateDatasetField(DatasetField dsf) {
        if (dsf.getParentDatasetFieldCompoundValue() != null) {
            return isTemplateDatasetField(dsf.getParentDatasetFieldCompoundValue().getParentDatasetField());
        } else {
            return dsf.getTemplate() != null;
        }
    }
}
