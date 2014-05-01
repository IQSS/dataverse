/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
public class DatasetFieldTypeValidator implements ConstraintValidator<ValidateDatasetFieldType, DatasetFieldValue> {

    //private String fieldType;
    public void initialize(ValidateDatasetFieldType constraintAnnotation) {
        //this.fieldType = constraintAnnotation.value();
    }

    public boolean isValid(DatasetFieldValue value, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation(); // we do this so we can have different messages depending on the different issue


        DatasetFieldType dsfType = value.getDatasetField().getDatasetFieldType();
        String fieldType = dsfType.getFieldType();


        if (dsfType.isRequired() && StringUtils.isBlank(value.getValue())) {
            context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is required.").addConstraintViolation();
            return false;
        }

        if (fieldType.equals("date")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                format.parse(value.getValue());
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is not a valid date.").addConstraintViolation();
                return false;
            }

        }
        return true;
    }

}
