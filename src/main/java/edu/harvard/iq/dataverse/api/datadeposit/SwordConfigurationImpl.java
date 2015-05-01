package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.swordapp.server.SwordConfiguration;

public class SwordConfigurationImpl implements SwordConfiguration {

    @EJB
    SettingsServiceBean settingsService;

    private static final Logger logger = Logger.getLogger(SwordConfigurationImpl.class.getCanonicalName());

    String getBaseUrlPathCurrent() {
        // see also url-pattern in web.xml
        return getBaseUrlPathV1dot1();
    }

    List<String> getBaseUrlPathsValid() {
        return Arrays.asList(getBaseUrlPathV1(), getBaseUrlPathV1dot1());
    }

    List<String> getBaseUrlPathsDeprecated() {
        return Arrays.asList(getBaseUrlPathV1());
    }

    String getBaseUrlPathV1() {
        return "/dvn/api/data-deposit/v1/swordv2";
    }

    String getBaseUrlPathV1dot1() {
        return "/dvn/api/data-deposit/v1.1/swordv2";
    }

    @Override
    public boolean returnDepositReceipt() {
        return true;
    }

    @Override
    public boolean returnStackTraceInError() {
        /**
         * @todo make this a JVM option Or better - a SettingsServiceBean option
         *
         * Do this at the same time as SWORD: implement equivalent of
         * dvn.dataDeposit.maxUploadInBytes
         * https://github.com/IQSS/dataverse/issues/1043
         */
        return false;
    }

    @Override
    public boolean returnErrorBody() {
        return true;
    }

    @Override
    public String generator() {
        return "http://www.swordapp.org/";
    }

    @Override
    public String generatorVersion() {
        return "2.0";
    }

    @Override
    public String administratorEmail() {
        return null;
    }

    @Override
    public String getAuthType() {
        // using "Basic" here to match what's in SwordAPIEndpoint
        return "Basic";
    }

    @Override
    public boolean storeAndCheckBinary() {
        return true;
    }

    @Override
    public String getTempDirectory() {
        String tmpFileDir = System.getProperty(SystemConfig.FILES_DIRECTORY);
        if (tmpFileDir != null) {
            String swordDirString = tmpFileDir + File.separator + "sword";
            File swordDirFile = new File(swordDirString);
            /**
             * @todo Do we really need this check? It seems like we do because
             * if you create a dataset via the native API and then later try to
             * upload a file via SWORD, the directory defined by
             * dataverse.files.directory may not exist and we get errors deep in
             * the SWORD library code. Could maybe use a try catch in the doPost
             * method of our SWORDv2MediaResourceServlet.
             */
            if (swordDirFile.exists()) {
                return swordDirString;
            } else {
                boolean mkdirSuccess = swordDirFile.mkdirs();
                if (mkdirSuccess) {
                    logger.log(Level.INFO, "Created directory {0}", swordDirString);
                    return swordDirString;
                } else {
                    String msgForSwordUsers = ("Could not determine or create SWORD temp directory. Check logs for details.");
                    logger.log(Level.SEVERE, "{0} Failed to create {1}", new Object[]{msgForSwordUsers, swordDirString});
                    // sadly, must throw RunTimeException to communicate with SWORD user
                    throw new RuntimeException(msgForSwordUsers);
                }
            }
        } else {
            String msgForSwordUsers = ("JVM option \"" + SystemConfig.FILES_DIRECTORY + "\" not defined. Check logs for details.");
            logger.severe(msgForSwordUsers);
            // sadly, must throw RunTimeException to communicate with SWORD user
            throw new RuntimeException(msgForSwordUsers);
        }
    }

    @Override
    public int getMaxUploadSize() {
        int unlimited = -1;
        String maxUploadInBytes = settingsService.getValueForKey(SettingsServiceBean.Key.DataDepositApiMaxUploadInBytes);
        if (maxUploadInBytes != null) {
            try {
                int maxUploadSizeInBytes = Integer.parseInt(maxUploadInBytes);
                return maxUploadSizeInBytes;
            } catch (NumberFormatException ex) {
                logger.log(Level.INFO, "Could not convert {0} from setting {1} to int. Setting Data Deposit API max upload size limit to unlimited.", new Object[]{maxUploadInBytes, SettingsServiceBean.Key.DataDepositApiMaxUploadInBytes});
                return unlimited;
            }
        } else {
            logger.log(Level.FINE, "Setting {0} is undefined. Setting Data Deposit API max upload size limit to unlimited.", SettingsServiceBean.Key.DataDepositApiMaxUploadInBytes);
            return unlimited;
        }
    }

    @Override
    public String getAlternateUrl() {
        return null;
    }

    @Override
    public String getAlternateUrlContentType() {
        return null;
    }

    @Override
    public boolean allowUnauthenticatedMediaAccess() {
        return false;
    }

}
