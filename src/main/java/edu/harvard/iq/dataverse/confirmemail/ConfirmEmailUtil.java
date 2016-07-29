package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.util.BundleUtil;

public class ConfirmEmailUtil {

    public static String friendlyExpirationTime(int expirationInt) {
        String measurement;
        String expirationString;

        if (expirationInt < 60) {
            measurement = BundleUtil.getStringFromBundle("minutes");
        } else if (expirationInt == 60) {
            expirationInt = expirationInt / 60;
            measurement = BundleUtil.getStringFromBundle("hour");
        } else {
            expirationInt = expirationInt / 60;
            measurement = BundleUtil.getStringFromBundle("hours");
        }
        expirationString = Integer.toString(expirationInt);
        return expirationString + " " + measurement;
    }

}
