package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;

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

    public static boolean validateDataverseMetadataExternally(Dataverse dv, String executable,
            DataverseRequest request) {
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
            logger.warning(
                    "Failed to export dataverse metadata as json; " + ex.getMessage() == null ? "" : ex.getMessage());
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

    public static void checkMetadataLangauge(Dataset ds, Dataverse owner, Map<String, String> mLangMap) {
        // Verify metadatalanguage is allowed
        logger.fine("Dataset mdl: " + ds.getMetadataLanguage());
        logger.fine("Owner mdl: " + owner.getMetadataLanguage());
        logger.fine("Map langs: " + mLangMap.toString());

        // :MetadataLanguage setting is not set
        // Must send UNDEFINED or match parent
        if (mLangMap.isEmpty()) {
            if (!(ds.getMetadataLanguage().equals(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE)
                    || ds.getMetadataLanguage().equals(owner.getMetadataLanguage()))) {
                throw new BadRequestException("This repository is not configured to support metadataLanguage.");
            }
        } else {
            // When :MetadataLanguage is set, the specificed language must either match the
            // parent collection choice, or, if that is undefined, be one of the choices
            // allowed by the setting
            if (!((ds.getMetadataLanguage().equals(owner.getMetadataLanguage())
                    && !owner.getMetadataLanguage().equals(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE))
                    || (owner.getMetadataLanguage().equals(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE)
                            && (mLangMap.containsKey(ds.getMetadataLanguage()))))) {
                throw new BadRequestException("Specified metadatalanguage ( metadataLanguage, "
                        + JsonLDTerm.schemaOrg("inLanguage").getUrl() + ") not allowed in this collection.");
            }
        }
    }

}
