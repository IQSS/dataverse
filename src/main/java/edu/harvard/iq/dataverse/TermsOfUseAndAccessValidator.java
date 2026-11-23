/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author skraffmi
 */
public class TermsOfUseAndAccessValidator implements ConstraintValidator<ValidateTermsOfUseAndAccess, TermsOfUseAndAccess> {

    @Override
    public void initialize(ValidateTermsOfUseAndAccess constraintAnnotation) {

    }

    @Override
    public boolean isValid(TermsOfUseAndAccess value, ConstraintValidatorContext context) {
        
        return isTOUAValid(value, context);

    }
    
    public static boolean isTOUAValid(TermsOfUseAndAccess value, ConstraintValidatorContext context){
        
        //if part of a template it is valid
        if (value.getTemplate() != null){
            return true;
        }
        
         //If there are no restricted files then terms are valid 
        if (!value.getDatasetVersion().isHasRestrictedFile()) {
            return true;
        }
        /*If there are restricted files then the version
        must allow access requests or have terms of access filled in.
         */
        boolean valid = value.isFileAccessRequest() == true || (value.getTermsOfAccess() != null && !value.getTermsOfAccess().isEmpty());
        if (!valid) {
            try {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate(BundleUtil.getStringFromBundle("dataset.message.toua.invalid")).addConstraintViolation();
                }

               value.setValidationMessage(BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
            } catch (NullPointerException e) {
                return false;
            }
            return false;
        }
        if (value.getLicense() != null) {
            if (!StringUtils.isBlank(value.getTermsOfUse())) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate("Terms of use and license cannot both be set.").addConstraintViolation();
                }
                return false;
            }
            if (!StringUtils.isBlank(value.getCitationRequirements()) ||
                !StringUtils.isBlank(value.getConditions()) ||
                !StringUtils.isBlank(value.getConfidentialityDeclaration()) ||
                !StringUtils.isBlank(value.getDepositorRequirements()) ||
                !StringUtils.isBlank(value.getDisclaimer()) ||
                !StringUtils.isBlank(value.getRestrictions()) ||
                !StringUtils.isBlank(value.getSpecialPermissions())) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate("When a license is set, no other terms can be set.").addConstraintViolation();
                }
                return false;
            }
        } else {
            if (StringUtils.isBlank(value.getTermsOfUse())) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate("Either terms of use or license must be set.").addConstraintViolation();
                }
                return false;
            }
            // TODO get the file based terms of use of existing/other files
        }
        return valid;
    }
}
