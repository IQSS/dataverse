package edu.harvard.iq.dataverse.util;

import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;

public class WebloaderUtil {

    private static final Logger logger = Logger.getLogger(WebloaderUtil.class.getCanonicalName());

    /**
     * Create the URL required to launch https://github.com/gdcc/dvwebloader
     * 
     * @param d The dataset to upload files to
     * @param apiToken The API token for authentication
     * @param localeCode The locale code for i18n
     * @param baseUrl The base URL of the webloader (e.g., http://localhost:8888/dvwebloader/dvwebloaderV2.html)
     * @param useS3Tagging Whether to use S3 tagging (false for storage backends that don't support it)
     * @return The complete URL with all parameters
     */
    public static String getWebloaderUrl(Dataset d, ApiToken apiToken, String localeCode, String baseUrl, boolean useS3Tagging) {
        // Use URLTokenUtil for params currently in common with external tools.
        URLTokenUtil tokenUtil = new URLTokenUtil(d, apiToken, localeCode);
        String appUrl;
        appUrl = baseUrl
                + "?datasetPid={datasetPid}&siteUrl={siteUrl}&key={apiToken}&datasetId={datasetId}&datasetVersion={datasetVersion}&dvLocale={localeCode}"
                + "&useS3Tagging=" + useS3Tagging;
        return tokenUtil.replaceTokensWithValues(appUrl);
    }

    /**
     * Create the URL required to launch https://github.com/gdcc/dvwebloader
     * (backward compatible version without useS3Tagging parameter - defaults to true)
     */
    public static String getWebloaderUrl(Dataset d, ApiToken apiToken, String localeCode, String baseUrl) {
        return getWebloaderUrl(d, apiToken, localeCode, baseUrl, true);
    }
}
