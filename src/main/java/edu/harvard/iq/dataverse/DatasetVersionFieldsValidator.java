/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 *
 * @author skraffmi
 */
public class DatasetVersionFieldsValidator implements ConstraintValidator<ValidateVersionFields, DatasetVersion> {

    @Override
    public void initialize(final ValidateVersionFields constraintAnnotation) {

    }

    @Override
    public boolean isValid(DatasetVersion t, ConstraintValidatorContext cvc) {
        return isHasDatasetFields(t, null);
    }

    public static boolean isHasDatasetFields(DatasetVersion t, ConstraintValidatorContext context) {
        return !t.getFlatDatasetFields().isEmpty();
    }
}