/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 *
 * @author skraffmi
 */
public class DatasetDeaccessionNoteValidator implements ConstraintValidator<ValidateDeaccessionNote, DatasetVersion> {
    
    private String versionState;
    private String deaccessionNote;

    @Override
    public void initialize(ValidateDeaccessionNote constraintAnnotation) {
        versionState = constraintAnnotation.versionState();
        deaccessionNote = constraintAnnotation.deaccessionNote();
    }


    @Override
    public boolean isValid(DatasetVersion value, ConstraintValidatorContext context) {
        
        if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED) && deaccessionNote.isEmpty()){
            if (context != null) {
                context.buildConstraintViolationWithTemplate(value + "  " + BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.textForReason.error")).addConstraintViolation();
            }
            return false;
        }
        if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED) && deaccessionNote.length() > DatasetVersion.VERSION_NOTE_MAX_LENGTH){
            if (context != null) {
                context.buildConstraintViolationWithTemplate(value + "  " + BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.limitChar.error")).addConstraintViolation();
            }
            return false;
        }
        return true;
        
    }

}
