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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


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
        // Heads up that "value" is sometimes null! Should || be && instead?
        if (!dsfType.isPrimitive() || !StringUtils.isBlank(value.getValue())) {
            boolean invalidCharactersFound = false;
            String errorMessage = null;
            if (value != null && value.getValue() != null) {
                // https://github.com/primefaces/primefaces/issues/3875 is related
                // in that when you type a \f (form feed) in the "create dataset"
                // page, the partial-response will include it, causing the page to be
                // broken. https://github.com/eclipse-ee4j/mojarra/pull/4534 contains
                // a fix, but it wasn't merged.
                String invalidCharacters = "^.*[\f\u0002].*$";
                Pattern p = Pattern.compile(invalidCharacters);
                Matcher m = p.matcher(value.getValue());
                invalidCharactersFound = m.find();
                errorMessage = BundleUtil.getStringFromBundle("invalidcharacter");
            }
            if (invalidCharactersFound) {
                try {
                    context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                } catch (NullPointerException npe) {
                    // This catch is copied from below.
                    //if there's no context for the error we can't put it anywhere....
                }
                return false;
            } else {
                return true;
            }
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
