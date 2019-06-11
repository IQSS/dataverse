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
public class TermsOfUseAndAccessValidator implements ConstraintValidator<ValidateTermsOfUseAndAccess, TermsOfUseAndAccess>  {

    @Override
    public void initialize(ValidateTermsOfUseAndAccess constraintAnnotation) {
        
    }

    @Override
    public boolean isValid(TermsOfUseAndAccess value, ConstraintValidatorContext context) {
        //if both null invalid
        //if(value.getTemplate() == null && value.getDatasetVersion() == null) return false;
        
        //if both not null invalid
        //return !(value.getTemplate() != null && value.getDatasetVersion() != null);
        return true;
    }
  
}
