package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.sql.Timestamp;

public class ConfirmEmailUtil {
    
    private ConfirmEmailUtil(){
        // prevent instance creation, this class has only static methods anyway.
    } 
    
    private static final Timestamp GRANDFATHERED_TIME = Timestamp.valueOf("2000-01-01 00:00:00.0");
    
    /**
     * Currently set to Y2K as an easter egg to easily set apart
     * grandfathered accounts from post-launch accounts.
     * @return 
     */
    public static Timestamp getGrandfatheredTime() {
        return GRANDFATHERED_TIME;
    }

    public static String friendlyExpirationTime(int expirationInt) {
        String measurement;
        String expirationString;
        long expirationLong = Long.valueOf(expirationInt);
        boolean hasDecimal = false;
        double expirationDouble = Double.valueOf(expirationLong);

        if (expirationLong == 1) {
            measurement = BundleUtil.getStringFromBundle("minute");
        } else if (expirationLong < 60) {
            measurement = BundleUtil.getStringFromBundle("minutes");
        } else if (expirationLong == 60) {
            expirationLong = expirationLong / 60;
            measurement = BundleUtil.getStringFromBundle("hour");
        } else {
            if (expirationLong % 60 == 0) {
                expirationLong = (long) (expirationLong / 60.0);
            } else {
                expirationDouble /= 60;
                hasDecimal = true;
            }
            measurement = BundleUtil.getStringFromBundle("hours");
        }
        if (hasDecimal == true) {
            expirationString = String.valueOf(expirationDouble);
            return expirationString + " " + measurement;
        } else {
            expirationString = String.valueOf(expirationLong);
            return expirationString + " " + measurement;
        }
    }

}
