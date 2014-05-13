package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.File;
import java.util.logging.Logger;
import org.swordapp.server.SwordConfiguration;

public class SwordConfigurationImpl implements SwordConfiguration {

    private static final Logger logger = Logger.getLogger(SwordConfigurationImpl.class.getCanonicalName());

    String getBaseUrlPath() {
        return "/dvn/api/data-deposit/v1/swordv2";
    }

    @Override
    public boolean returnDepositReceipt() {
        return true;
    }

    @Override
    public boolean returnStackTraceInError() {
        return true;
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
        /**
         * @todo is it safe to use dataverse.files.directory for this?
         */
//        String tmpFileDir = System.getProperty("vdc.temp.file.dir");
        String tmpFileDir = System.getProperty("dataverse.files.directory");
        if (tmpFileDir != null) {
            return tmpFileDir + File.separator + "sword";
        } else {
            return null;
        }
    }

    @Override
    public int getMaxUploadSize() {
        int unlimited = -1;
        /**
         * @todo rename this from dvn to dataverse?
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
