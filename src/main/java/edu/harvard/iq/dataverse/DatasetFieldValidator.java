/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;


/**
 *
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
        //TODO more needs to be done for 'multi-layer' compound fields
        if (dsfType.isPrimitive() && dsfType.isRequired() && value.getTemplate() == null && StringUtils.isBlank(value.getValue())) {
            if (value.getParentDatasetFieldCompoundValue() != null && value.getParentDatasetFieldCompoundValue().getParentDatasetField().getTemplate() != null){
                return true;
            }
            context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is required.").addConstraintViolation();
            return false;
        }
        return true;
    }

}
