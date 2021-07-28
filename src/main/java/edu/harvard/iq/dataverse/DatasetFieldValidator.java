/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Collections;
import java.util.List;
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

        // if value is not primitive or not empty
        if (!dsfType.isPrimitive() || !StringUtils.isBlank(value.getValue())) {
            return true;
        }
       
        if (value.isRequired()) { 
            String errorMessage = null;
            DatasetFieldCompoundValue parent = value.getParentDatasetFieldCompoundValue();
            if (parent == null || parent.getParentDatasetField().isRequired()) {
                errorMessage = BundleUtil.getStringFromBundle("isrequired", List.of(dsfType.getDisplayName()));
            } else if (areSiblingsPopulated(value)) {
                errorMessage = BundleUtil.getStringFromBundle("isrequired.conditional", List.of(dsfType.getDisplayName(),parent.getParentDatasetField().getDatasetFieldType().getDisplayName()));
            }

            if (errorMessage != null) {
                try {
                    context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                } catch (NullPointerException npe){
                    //if there's no context for the error we can't put it anywhere....
                }
                
                return false;
            }
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
    
    private boolean areSiblingsPopulated(DatasetField dsf) {
        if (dsf.getParentDatasetFieldCompoundValue() != null) {
            DatasetFieldCompoundValue compound = dsf.getParentDatasetFieldCompoundValue();
            for (DatasetField sibling : compound.getChildDatasetFields()) {
                if (!StringUtils.isBlank(sibling.getValue())) {
                    return true;
                }
            }      
        }

        return false;
    } 
}
