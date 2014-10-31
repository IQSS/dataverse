package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.swordapp.server.SwordConfiguration;

public class SwordConfigurationImpl implements SwordConfiguration {

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

    /**
     * @returns null (unused)
     */
    @Override
    public String getTempDirectory() {
        logger.info("getTempDirectory() called unexpectedly!");
        return null;
    }

    @Override
    public int getMaxUploadSize() {
        int unlimited = -1;
        /**
         * @todo Move this from a JVM option to a setting in the database.
         *
         * SWORD: implement equivalent of dvn.dataDeposit.maxUploadInBytes
         * https://github.com/IQSS/dataverse/issues/1043
         */
        String jvmOption = "dvn.dataDeposit.maxUploadInBytes";
        String maxUploadInBytes = System.getProperty(jvmOption);
        if (maxUploadInBytes != null) {
            try {
                int maxUploadSizeInBytes = Integer.parseInt(maxUploadInBytes);
                return maxUploadSizeInBytes;
            } catch (NumberFormatException ex) {
                logger.fine("Could not convert " + maxUploadInBytes + " from JVM option " + jvmOption + " to int. Setting Data Deposit APU max upload size limit to unlimited.");
                return unlimited;
            }
        } else {
            logger.fine("JVM option " + jvmOption + " is undefined. Setting Data Deposit APU max upload size limit to unlimited.");
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
