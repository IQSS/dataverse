package edu.harvard.iq.dataverse.mail.confirmemail;

/**
 * This enum is (and should be) used only for the purposes of handling
 * mode when we're restricting users without confirmed e-mail.
 * <p>It should not be used to mark the real status of e-mail confirmation
 * anywhere.
 */
public enum EffectiveMailConfirmationStatus {
    CONFIRMED,
    UNCONFIRMED,
    NOT_APPLICABLE
}
