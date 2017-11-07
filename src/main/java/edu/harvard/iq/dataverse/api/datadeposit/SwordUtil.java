package edu.harvard.iq.dataverse.api.datadeposit;

import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

/**
 * @todo move all this to the newer SwordService Bean
 */
public class SwordUtil {

    static String DCTERMS = "http://purl.org/dc/terms/";

    /*
     * @todo get rid of this method
     */
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

    /*
     * @todo get rid of this method
     */
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
