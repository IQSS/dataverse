package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import static java.util.stream.Collectors.joining;
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

    public static void datasetLockCheck(Dataset dataset) throws SwordError {
        if ( dataset.isLocked() ) {
            String message = "Please try again later. Unable to perform operation due to dataset lock: " 
                    + dataset.getLocks().stream().map(l->l.getReason().name() + ": " + l.getInfo()).collect( joining(",") );
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, message);
        }
    }

}
