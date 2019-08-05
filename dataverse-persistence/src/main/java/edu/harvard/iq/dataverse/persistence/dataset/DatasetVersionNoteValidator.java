package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author skraffmi
 */
public class DatasetVersionNoteValidator implements ConstraintValidator<ValidateVersionNote, DatasetVersion> {

    private String versionState;
    private String versionNote;

    @Override
    public void initialize(ValidateVersionNote constraintAnnotation) {
        versionState = constraintAnnotation.versionState();
        versionNote = constraintAnnotation.versionNote();
    }


    @Override
    public boolean isValid(DatasetVersion value, ConstraintValidatorContext context) {

        if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED) && versionNote.isEmpty()) {
            if (context != null) {
                context.buildConstraintViolationWithTemplate(value + "  " + BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.textForReason.error")).addConstraintViolation();
            }
            return false;
        }
        if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED) && versionNote.length() > DatasetVersion.VERSION_NOTE_MAX_LENGTH) {
            if (context != null) {
                context.buildConstraintViolationWithTemplate(value + "  " + BundleUtil.getStringFromBundle("file.deaccessionDialog.dialog.limitChar.error")).addConstraintViolation();
            }
            return false;
        }
        return true;

    }

}
