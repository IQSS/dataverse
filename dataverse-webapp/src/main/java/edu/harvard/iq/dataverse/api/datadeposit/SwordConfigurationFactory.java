package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.swordapp.server.SwordConfiguration;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;

/**
 * Factory of {@link SwordConfigurationImpl} objects
 * @author dbojanek
 */
@Stateless
public class SwordConfigurationFactory {

    @Inject
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsService;

    // -------------------- LOGIC --------------------

    /**
     * Creates new {@link SwordConfigurationImpl} object
     * @return object created
     */
    public SwordConfiguration createSwordConfiguration() {
        SwordConfigurationImpl swordConf = new SwordConfigurationImpl();
        swordConf.setTempDirectory(createTempDirectory());
        swordConf.setMaxUploadSize(calculateMaxUploadSize());
        swordConf.setErrorBody(true);
        swordConf.setDepositReceipt(true);
        swordConf.setStackTraceInError(false);
        swordConf.setGeneratorUrl("http://www.swordapp.org/");
        swordConf.setGeneratorVersion("2.0");
        swordConf.setAdministratorEmail(null);
        swordConf.setAuthType("Basic");
        swordConf.setStoreAndCheckBinary(true);
        swordConf.setAlternateUrl(null);
        swordConf.setAlternateUrlContentType(null);
        swordConf.setAllowUnauthenticatedMediaAccess(false);

        return swordConf;
    }

    // -------------------- PRIVATE ---------------------

    /**
     * @return temporary Sword directory, based on system config
     */
    private String createTempDirectory() {
        String tmpFileDir = systemConfig.getFilesDirectory();
        File swordDirFile = new File(tmpFileDir, "sword");

        swordDirFile.mkdirs();
        if(!swordDirFile.exists()) {
            throw new RuntimeException("Could not determine or create SWORD temp directory. Check logs for details.");
        }
        return swordDirFile.getAbsolutePath();
    }

    /**
     * @return calculated maximum upload size
     */
    private int calculateMaxUploadSize() {
        int unlimited = -1;

        Long maxUploadInBytes = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);

        if (maxUploadInBytes == null) {
            // (a) No setting, return unlimited
            return unlimited;

        } else if (maxUploadInBytes > Integer.MAX_VALUE) {
            // (b) setting returns the limit of int, return max int value  (BUG)
            return Integer.MAX_VALUE;

        } else {
            // (c) Return the setting as an int
            return maxUploadInBytes.intValue();

        }
    }
}
