package edu.harvard.iq.dataverse.util;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

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
