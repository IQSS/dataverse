package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import java.util.logging.Logger;
import opennlp.tools.util.StringUtil;
import org.apache.commons.io.FileUtils;

public class DataverseUtil {

    private static final Logger logger = Logger.getLogger(DataverseUtil.class.getCanonicalName());
    
    public static String getSuggestedDataverseNameOnCreate(User user) {
        if (user == null) {
            return null;
        }
        // getDisplayInfo() is never null.
        return user.getDisplayInfo().getTitle() + " " + BundleUtil.getStringFromBundle("dataverse");
    }
    
    public static boolean validateDataverseMetadataExternally(Dataverse dv, String executable, DataverseRequest request) {
        String jsonMetadata; 
        
        String sourceAddressLabel = "0.0.0.0"; 
        
        if (request != null) {
            IpAddress sourceAddress = request.getSourceAddress();
            if (sourceAddress != null) {
                sourceAddressLabel = sourceAddress.toString();
            }
        }
        
        try {
            jsonMetadata = json(dv).add("sourceAddress", sourceAddressLabel).build().toString();
        } catch (Exception ex) {
            logger.warning("Failed to export dataverse metadata as json; "+ex.getMessage() == null ? "" : ex.getMessage());
            return false; 
        }
        
        if (StringUtil.isEmpty(jsonMetadata)) {
            logger.warning("Failed to export dataverse metadata as json.");
            return false; 
        }
       
        // save the metadata in a temp file: 
        
        try {
            File tempFile = File.createTempFile("dataverseMetadataCheck", ".tmp");
            FileUtils.writeStringToFile(tempFile, jsonMetadata);
                                    
            // run the external executable: 
            String[] params = { executable, tempFile.getAbsolutePath() };
            Process p = Runtime.getRuntime().exec(params);
            p.waitFor();
            
            return p.exitValue() == 0;

        } catch (IOException | InterruptedException ex) {
            logger.warning("Failed run the external executable.");
            return false; 
        }
        
    }

}
