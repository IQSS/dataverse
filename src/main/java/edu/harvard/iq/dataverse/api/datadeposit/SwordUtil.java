package edu.harvard.iq.dataverse.api.datadeposit;

import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

public class SwordUtil {

    public static SwordError throwSpecialSwordErrorWithoutStackTrace(String SwordUriRegistryError, String error) {
        if (SwordUriRegistryError == null) {
            SwordUriRegistryError = UriRegistry.ERROR_BAD_REQUEST;
        }
        if (error == null) {
            error = "UNKNOWN";
        }
        SwordError swordError = new SwordError(SwordUriRegistryError, error);
        StackTraceElement[] emptyStackTrace = new StackTraceElement[0];
        swordError.setStackTrace(emptyStackTrace);
        return swordError;
    }

    public static SwordError throwRegularSwordErrorWithoutStackTrace(String error) {
        if (error == null) {
            error = "UNKNOWN";
        }
        SwordError swordError = new SwordError(error);
        StackTraceElement[] emptyStackTrace = new StackTraceElement[0];
        swordError.setStackTrace(emptyStackTrace);
        return swordError;
    }

}
