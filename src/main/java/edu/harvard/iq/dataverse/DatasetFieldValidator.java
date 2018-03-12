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
        if (isTemplateDatasetField(value)){
            return true;
        }
        if (((dsfType.isPrimitive() && dsfType.isRequired())  || (dsfType.isPrimitive() && value.isRequired())) 
                && StringUtils.isBlank(value.getValue())) {
            try{
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is required.").addConstraintViolation();
            } catch (NullPointerException npe){
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
