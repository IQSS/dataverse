package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.BundleUtil;

public class DataverseUtil {

    public static String getSuggestedDataverseNameOnCreate(User user) {
        if (user == null) {
            return null;
        }
        // getDisplayInfo() is never null.
        return user.getDisplayInfo().getTitle() + " " + BundleUtil.getStringFromBundle("dataverse");
    }

}
