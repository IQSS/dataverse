/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import edu.harvard.iq.dataverse.util.BundleUtil;

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

        // If invalid characters are found, mutate the value by removing them.
        if (value != null && value.getValue() != null) {
            String invalidCharacters = "[\f\u0002\ufffe]";
            Pattern p = Pattern.compile(invalidCharacters);
            Matcher m = p.matcher(value.getValue());
            boolean invalidCharactersFound = m.find();
            if (invalidCharactersFound) {
                List<DatasetFieldValue> datasetFieldValues = value.getDatasetFieldValues();
                List<ControlledVocabularyValue> controlledVocabularyValues = value.getControlledVocabularyValues();
                if (!datasetFieldValues.isEmpty()) {
                    datasetFieldValues.get(0).setValue(value.getValue().replaceAll(invalidCharacters, ""));
                } else if (controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty()) {
                    // This controlledVocabularyValues logic comes from value.getValue().
                    // Controlled vocabularies shouldn't have invalid characters in them
                    // but they do, we can add a "replace" here. Some untested, commented code below.
                    // if (controlledVocabularyValues.get(0) != null) {
                    //    controlledVocabularyValues.get(0).setStrValue(value.getValue().replaceAll(invalidCharacters, ""));
                    // }
                }
            }
        }

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
