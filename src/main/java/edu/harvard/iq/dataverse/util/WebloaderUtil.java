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
     */
    public static String getWebloaderUrl(Dataset d, ApiToken apiToken, String localeCode, String baseUrl) {
        // Use URLTokenUtil for params currently in common with external tools.
        URLTokenUtil tokenUtil = new URLTokenUtil(d, apiToken, localeCode);
        String appUrl;
        appUrl = baseUrl
                + "?datasetPid={datasetPid}&siteUrl={siteUrl}&key={apiToken}&datasetId={datasetId}&datasetVersion={datasetVersion}&dvLocale={localeCode}";
        return tokenUtil.replaceTokensWithValues(appUrl);
    }
}
