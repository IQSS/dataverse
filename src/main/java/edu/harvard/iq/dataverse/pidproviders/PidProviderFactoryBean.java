package edu.harvard.iq.dataverse.pidproviders;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import edu.harvard.iq.dataverse.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import io.gdcc.spi.export.Exporter;

/**
 * This Bean loads all of the PidProviderFactory types available (e.g. EZID,
 * DataCite, Handle, PermaLink) and then reads the configuration to load
 * particular PidProviders (e.g. a DataCite provider with a specific
 * authority/shoulder, username/password, etc.)
 */
@Startup
@Singleton
public class PidProviderFactoryBean {

    private static final Logger logger = Logger.getLogger(PidProviderFactoryBean.class.getCanonicalName());

    private ServiceLoader<PidProviderFactory> loader;
    private Map<String, PidProviderFactory> pidProviderFactoryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadProviderFactories();
        loadProviders();
    }

    private void loadProviderFactories() {
        /*
         * Step 1 - find the PROVIDERS dir and add all jar files there to a class loader
         */
        List<URL> jarUrls = new ArrayList<>();
        Optional<String> providerPathSetting = JvmSettings.PROVIDERS_DIRECTORY.lookupOptional(String.class);
        if (providerPathSetting.isPresent()) {
            Path exporterDir = Paths.get(providerPathSetting.get());
            // Get all JAR files from the configured directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(exporterDir, "*.jar")) {
                // Using the foreach loop here to enable catching the URI/URL exceptions
                for (Path path : stream) {
                    logger.log(Level.FINE, "Adding {0}", path.toUri().toURL());
                    // This is the syntax required to indicate a jar file from which classes should
                    // be loaded (versus a class file).
                    jarUrls.add(new URL("jar:" + path.toUri().toURL() + "!/"));
                }
            } catch (IOException e) {
                logger.warning("Problem accessing external Exporters: " + e.getLocalizedMessage());
            }
        }
        URLClassLoader cl = URLClassLoader.newInstance(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());

        /*
         * Step 2 - load all PidProcviderFactories that can be found, using the jars as
         * additional sources
         */
        loader = ServiceLoader.load(PidProviderFactory.class, cl);
        /*
         * Step 3 - Fill pidProviderFactoryMap with type as the key, allow external
         * factories to replace internal ones for the same type. FWIW: From the logging
         * it appears that ServiceLoader returns classes in ~ alphabetical order rather
         * than by class loader, so internal classes handling a given providerName may
         * be processed before or after external ones.
         */
        loader.forEach(providerFactory -> {
            String type = providerFactory.getType();
            // If no entry for this providerName yet or if it is an external exporter
            if (!pidProviderFactoryMap.containsKey(type) || providerFactory.getClass().getClassLoader().equals(cl)) {
                pidProviderFactoryMap.put(type, providerFactory);
            }
            logger.log(Level.FINE,
                    "Loaded PidProviderFactory of type: " + type + " from "
                            + providerFactory.getClass().getCanonicalName() + " and classloader: "
                            + providerFactory.getClass().getClassLoader().getClass().getCanonicalName());
        });
    }

    private void loadProviders() {
        String[] providers = JvmSettings.PID_PROVIDERS.lookup(String[].class);
        for (String name : providers) {
            String type = JvmSettings.PID_PROVIDER_TYPE.lookup(name);
            if (pidProviderFactoryMap.containsKey(type)) {
                GlobalIdServiceBean provider = pidProviderFactoryMap.get(type).createPidProvider(name);
                PidUtil.addToProviderList(provider);
            }
        }
        PidUtil.addAllToUnmanagedProviderList(Arrays.asList(unmanagedDOISvc, unmanagedHandleSvc));
    }

}