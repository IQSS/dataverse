package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.logging.Logger;

public class DataverseUtil {

    private static final Logger logger = Logger.getLogger(DataverseUtil.class.getCanonicalName());
    
    public static String getSuggestedDataverseNameOnCreate(User user) {
        if (user == null) {
            return null;
        }
        // getDisplayInfo() is never null.
        return user.getDisplayInfo().getTitle() + " " + BundleUtil.getStringFromBundle("dataverse");
    }
    
    public static boolean validateDataverseMetadataExternally(Dataverse dv) {
        String jsonString = json(dv).build().toString(); 
        
        logger.info("dataverse as json: "+jsonString);
       
        return false;
    }

}
