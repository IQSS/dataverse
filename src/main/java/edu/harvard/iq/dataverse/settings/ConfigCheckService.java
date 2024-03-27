package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.MailSessionProducer;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.mail.internet.InternetAddress;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@DependsOn({"StartupFlywayMigrator", "PidProviderFactoryBean"})
public class ConfigCheckService {
    
    private static final Logger logger = Logger.getLogger(ConfigCheckService.class.getCanonicalName());
    
    @Inject
    MailSessionProducer mailSessionProducer;
    @Inject
    MailServiceBean mailService;

    public static class ConfigurationError extends RuntimeException {
        public ConfigurationError(String message) {
            super(message);
        }
    }
    
    @PostConstruct
    public void startup() {
        if (!checkSystemDirectories() || !checkPidProviders()) {
            throw new ConfigurationError("Not all configuration checks passed successfully. See logs above.");
        }
        
        // Only checks resulting in warnings, nothing critical that needs to stop deployment
        checkSystemMailSetup();
    }

    /**
     * In this method, we check the existence and write-ability of all important directories we use during
     * normal operations. It does not include checks for the storage system. If directories are not available,
     * try to create them (and fail when not allowed to).
     *
     * @return True if all checks successful, false otherwise.
     */
    public boolean checkSystemDirectories() {
        Map<Path, String> paths = Map.of(
                Path.of(JvmSettings.UPLOADS_DIRECTORY.lookup()), "temporary JSF upload space (see " + JvmSettings.UPLOADS_DIRECTORY.getScopedKey() + ")",
                Path.of(FileUtil.getFilesTempDirectory()), "temporary processing space (see " + JvmSettings.FILES_DIRECTORY.getScopedKey() + ")",
                Path.of(JvmSettings.DOCROOT_DIRECTORY.lookup()), "docroot space (see " + JvmSettings.DOCROOT_DIRECTORY.getScopedKey() + ")");
        
        boolean success = true;
        for (Path path : paths.keySet()) {
            // Check if the configured path is absolute - avoid potential problems with relative paths this way
            if (! path.isAbsolute()) {
                logger.log(Level.SEVERE, () -> "Configured directory " + path + " for " + paths.get(path) + " is not absolute");
                success = false;
                continue;
            }
            
            if (! Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    String details;
                    if (e instanceof FileSystemException) {
                        details = ": " + e.getClass();
                    } else {
                        details = "";
                    }
                    
                    logger.log(Level.SEVERE, () -> "Could not create directory " + path + " for " + paths.get(path) + details);
                    success = false;
                }
            } else if (!Files.isWritable(path)) {
                logger.log(Level.SEVERE, () -> "Directory " + path + " for " + paths.get(path) + " exists, but is not writeable");
                success = false;
            }
        }
        return success;
    }
    
    /**
     * This method is not expected to make a deployment fail, but send out clear warning messages about missing or
     * wrong configuration settings.
     */
    public void checkSystemMailSetup() {
        // Check if a system mail setting has been provided or issue warning about disabled mail notifications
        Optional<InternetAddress> mailAddress = mailService.getSystemAddress();
        
        // Not present -> warning
        if (mailAddress.isEmpty()) {
            logger.warning("Could not find a system mail setting in database (key :" + Key.SystemEmail + ", deprecated) or JVM option '" + JvmSettings.SYSTEM_EMAIL.getScopedKey() + "'");
            logger.warning("Mail notifications and system messages are deactivated until you provide a configuration");
        }
        
        // If there is an app server provided mail config, let's determine if the setup is matching
        // TODO: when support for appserver provided mail session goes away, this code can be deleted
        if (mailSessionProducer.hasSessionFromAppServer()) {
            if (mailAddress.isEmpty()) {
                logger.warning("Found a mail session provided by app server, but no system mail address (see logs above)");
            // Check if the "from" in the session is the same as the system mail address (see issue 4210)
            } else {
                String sessionFrom = mailSessionProducer.getSession().getProperty("mail.from");
                if (! mailAddress.get().toString().equals(sessionFrom)) {
                    logger.warning(() -> String.format(
                        "Found app server mail session provided 'from' (%s) does not match system mail setting (%s)",
                        sessionFrom, mailAddress.get()));
                }
            }
        }
    }

    /**
     * Verifies that at least one PidProvider capable of editing/minting PIDs is
     * configured. Requires the @DependsOn("PidProviderFactoryBean") annotation above
     * since it is the @PostCOnstruct init() method of that class that loads the PidProviders
     *
     * @return True if all checks successful, false otherwise.
     */
    private boolean checkPidProviders() {
        return PidUtil.getManagedProviderIds().size() > 0;
    }
}
