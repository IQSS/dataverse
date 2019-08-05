package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.user.User;

public class DataverseUtil {

    public static String getSuggestedDataverseNameOnCreate(User user) {
        if (user == null) {
            return null;
        }
        // getDisplayInfo() is never null.
        return user.getDisplayInfo().getTitle() + " " + BundleUtil.getStringFromBundle("dataverse");
    }

}
