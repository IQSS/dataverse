package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagValidator;
import edu.harvard.iq.dataverse.util.bagit.BagValidator.BagValidatorSettings;
import edu.harvard.iq.dataverse.util.bagit.ManifestReader;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProviderFactory;
import edu.harvard.iq.dataverse.util.bagit.data.FileUtilWrapper;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Optional;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
@SessionScoped
@Named
public class BagItFileHandlerFactory implements Serializable {

    private static final Logger logger = Logger.getLogger(BagItFileHandlerFactory.class.getCanonicalName());

    public static final String BAGIT_HANDLER_ENABLED_SETTING = ":BagItHandlerEnabled";

    @EJB
    private SettingsServiceBean settingsService;
    
    private BagItFileHandler bagItFileHandler;

    @PostConstruct
    public void initialize() {
        boolean bagitHandlerEnabled = settingsService.isTrue(BAGIT_HANDLER_ENABLED_SETTING, false);
        if(!bagitHandlerEnabled) {
            logger.info("action=initialize completed message=bagit-file-handler-disabled");
            bagItFileHandler = null;
            return;
        }

        Integer validatorJobPoolSize = getIntegerSetting(BagValidatorSettings.JOB_POOL_SIZE.getSettingsKey(), BagValidatorSettings.JOB_POOL_SIZE.getDefaultValue());
        Integer maxErrors = getIntegerSetting(BagValidatorSettings.MAX_ERRORS.getSettingsKey(), BagValidatorSettings.JOB_WAIT_INTERVAL.getDefaultValue());
        Integer jobWaitIntervalInSeconds = getIntegerSetting(BagValidatorSettings.JOB_WAIT_INTERVAL.getSettingsKey(), BagValidatorSettings.JOB_WAIT_INTERVAL.getDefaultValue());
        BagValidator bagValidator = new BagValidator(validatorJobPoolSize, maxErrors, jobWaitIntervalInSeconds, new ManifestReader());
        bagItFileHandler = new BagItFileHandler(new FileUtilWrapper(), new FileDataProviderFactory(), bagValidator, new BagItFileHandlerPostProcessor());
        logger.info(String.format("action=initialize completed validatorJobPoolSize=%s maxErrors=%s jobWaitIntervalInSeconds=%s message=bagit-file-handler-created", validatorJobPoolSize, maxErrors, jobWaitIntervalInSeconds));
    }

    public Optional<BagItFileHandler> getBagItFileHandler() {
        return Optional.ofNullable(bagItFileHandler);
    }

    private Integer getIntegerSetting(String settingsKey, Integer defaultValue) {
        String settingsValue = settingsService.get(settingsKey);
        if(settingsValue != null) {
            try {
                return Integer.valueOf(settingsValue);
            } catch (Exception e) {
                logger.info(String.format("action=initialize message=error-getting-int-setting setting=%s value=%s defaultValue=%s", settingsKey, settingsValue, defaultValue));
            }
        }

        return defaultValue;
    }
}
