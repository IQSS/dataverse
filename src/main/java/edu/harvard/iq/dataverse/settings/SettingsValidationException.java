package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SettingsValidationException extends RuntimeException {
    public SettingsValidationException(String message) {
        super(message);
    }
}
