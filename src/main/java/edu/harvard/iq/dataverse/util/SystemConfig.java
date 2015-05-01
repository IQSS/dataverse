package edu.harvard.iq.dataverse.util;

import com.ocpsoft.pretty.PrettyContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * System-wide configuration
 */
@Stateless
@Named
public class SystemConfig {

    private static final Logger logger = Logger.getLogger(SystemConfig.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

    /**
     * A JVM option for the advertised fully qualified domain name (hostname) of
     * the Dataverse installation, such as "dataverse.example.com", which may
     * differ from the hostname that the server knows itself as.
     *
     * The equivalent in DVN 3.x was "dvn.inetAddress".
     */
    public static final String FQDN = "dataverse.fqdn";
    
    /**
     * A JVM option for specifying the "official" URL of the site. 
     * Unlike the FQDN option above, this would be a complete URL, 
     * with the protocol, port number etc. 
     */
    public static final String SITE_URL = "dataverse.siteUrl";

    /**
     * A JVM option for where files are stored on the file system.
     */
    public static final String FILES_DIRECTORY = "dataverse.files.directory";

    /**
     * A JVM option to override the number of minutes for which a password reset
     * token is valid ({@link #minutesUntilPasswordResetTokenExpires}).
     */
    private static final String PASSWORD_RESET_TIMEOUT_IN_MINUTES = "dataverse.auth.password-reset-timeout-in-minutes";

    /**
     * A common place to find the String for a sane Solr hostname:port
     * combination.
     */
    private String saneDefaultForSolrHostColonPort = "localhost:8983";

    /**
     * The default number of datafiles that we allow to be created through 
     * zip file upload.
     */
    private static final int defaultZipUploadFilesLimit = 1000; 

    /**
     * @todo Stop hard coding the same value in idpselect_config.js
     */
    public static final int APACHE_HTTPS_PORT = 8181;
    
    public String getSolrHostColonPort() {
        String solrHostColonPort = settingsService.getValueForKey(SettingsServiceBean.Key.SolrHostColonPort, saneDefaultForSolrHostColonPort);
        return solrHostColonPort;
    }
    
    
    
    /**
     * The number of minutes for which a password reset token is valid. Can be
     * overridden by {@link #PASSWORD_RESET_TIMEOUT_IN_MINUTES}.
     */
    public static int getMinutesUntilPasswordResetTokenExpires() {
        final int reasonableDefault = 60;
        String configuredValueAsString = System.getProperty(PASSWORD_RESET_TIMEOUT_IN_MINUTES);
        if (configuredValueAsString != null) {
            int configuredValueAsInteger = 0;
            try {
                configuredValueAsInteger = Integer.parseInt(configuredValueAsString);
                if (configuredValueAsInteger > 0) {
                    return configuredValueAsInteger;
                } else {
                    logger.log(Level.INFO,PASSWORD_RESET_TIMEOUT_IN_MINUTES + " is configured as a negative number \"{0}\". Using default value instead: {1}", new Object[]{configuredValueAsInteger, reasonableDefault});
                    return reasonableDefault;
                }
            } catch (NumberFormatException ex) {
                logger.log(Level.INFO,"Unable to convert " + PASSWORD_RESET_TIMEOUT_IN_MINUTES + " from \"{0}\" into an integer value: {1}. Using default value {2}", new Object[]{configuredValueAsString, ex, reasonableDefault});
            }
        }
        return reasonableDefault;
    }
    
    /**
     * The "official", designated URL of the site;
     * can be defined as a complete URL; or derived from the 
     * "official" hostname. If none of these options is set,
     * defaults to the InetAddress.getLocalHOst() and https;
     * These are legacy JVM options. Will be eventualy replaced
     * by the Settings Service configuration.
     */
    public String getDataverseSiteUrl() {
        String hostUrl = System.getProperty(SITE_URL);
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty(FQDN);
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        hostUrl = "https://" + hostName;
        return hostUrl;
    }
    
    /**
     * URL Tracking: 
     */
    public String getPageURLWithQueryString() {
        return PrettyContext.getCurrentInstance().getRequestURL().toURL() + PrettyContext.getCurrentInstance().getRequestQueryString().toQueryString();
    }

    /**
     * The "official" server's fully-qualified domain name: 
     */
    public String getDataverseServer() {
        // still reliese on a JVM option: 
        String fqdn = System.getProperty(FQDN);
        if (fqdn == null) {
            try {
                fqdn = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return fqdn;
    }
    
    /**
     * Download-as-zip size limit.
     * returns 0 if not specified; 
     * (the file zipper will then use the default value)
     * set to -1 to disable zip downloads. 
     */
    
    public long getZipDownloadLimit() {
        String zipLimitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipDonwloadLimit);   
        
        Long zipLimit = null; 
        if (zipLimitOption != null && !zipLimitOption.equals("")) {
            try {
                zipLimit = new Long(zipLimitOption);
            } catch (NumberFormatException nfe) {
                zipLimit = null; 
            }
        }
        
        if (zipLimit != null) {
            return zipLimit.longValue();
        }
        
        return 0L; 
    }
    
    public int getZipUploadFilesLimit() {
        String limitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipUploadFilesLimit);
        Integer limit = null; 
        
        if (limitOption != null && !limitOption.equals("")) {
            try {
                limit = new Integer(limitOption);
            } catch (NumberFormatException nfe) {
                limit = null; 
            }
        }
        
        if (limit != null) {
            return limit.intValue();
        }
        
        return defaultZipUploadFilesLimit; 
    }

    // curl -X PUT -d@/tmp/apptos.txt http://localhost:8080/api/s/settings/:ApplicationTermsOfUse
    public String getApplicationTermsOfUse() {
        String saneDefaultForAppTermsOfUse = "There are no Terms of Use for this Dataverse installation.";
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, saneDefaultForAppTermsOfUse);
        return appTermsOfUse;
    }

    public String getApiTermsOfUse() {
        String saneDefaultForApiTermsOfUse = "There are no API Terms of Use for this Dataverse installation.";
        String apiTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApiTermsOfUse, saneDefaultForApiTermsOfUse);
        return apiTermsOfUse;
    }

    public String getApplicationPrivacyPolicyUrl() {
        String saneDefaultForPrivacyPolicyUrl = null;
        String appPrivacyPolicyUrl = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationPrivacyPolicyUrl, saneDefaultForPrivacyPolicyUrl);
        return appPrivacyPolicyUrl;
    }

    public boolean isShibEnabled() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShibEnabled, safeDefaultIfKeyNotFound);
    }

    public boolean isDebugEnabled() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.Debug, safeDefaultIfKeyNotFound);
    }

}
