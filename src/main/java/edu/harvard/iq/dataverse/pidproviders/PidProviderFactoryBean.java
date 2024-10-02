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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.doi.UnmanagedDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.ezid.EZIdDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.fake.FakeDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.UnmanagedHandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.UnmanagedPermaLinkPidProvider;

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

    @Inject
    DataverseServiceBean dataverseService;
    @EJB
    protected SettingsServiceBean settingsService;
    @Inject
    protected DvObjectServiceBean dvObjectService;
    @Inject
    SystemConfig systemConfig;

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
        Optional<String> providerPathSetting = JvmSettings.PIDPROVIDERS_DIRECTORY.lookupOptional(String.class);
        if (providerPathSetting.isPresent()) {
            Path providersDir = Paths.get(providerPathSetting.get());
            // Get all JAR files from the configured directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(providersDir, "*.jar")) {
                // Using the foreach loop here to enable catching the URI/URL exceptions
                for (Path path : stream) {
                    logger.log(Level.FINE, "Adding {0}", path.toUri().toURL());
                    // This is the syntax required to indicate a jar file from which classes should
                    // be loaded (versus a class file).
                    jarUrls.add(new URL("jar:" + path.toUri().toURL() + "!/"));
                }
            } catch (IOException e) {
                logger.warning("Problem accessing external Providers: " + e.getLocalizedMessage());
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
            logger.fine("Loaded PidProviderFactory of type: " + type);
            // If no entry for this providerName yet or if it is an external provider
            if (!pidProviderFactoryMap.containsKey(type) || providerFactory.getClass().getClassLoader().equals(cl)) {
                logger.fine("Adding PidProviderFactory of type: " + type + " to the map");
                pidProviderFactoryMap.put(type, providerFactory);
            }
            logger.log(Level.FINE,
                    "Loaded PidProviderFactory of type: " + type + " from "
                            + providerFactory.getClass().getCanonicalName() + " and classloader: "
                            + providerFactory.getClass().getClassLoader().getClass().getCanonicalName());
        });
    }

    private void loadProviders() {
        Optional<String[]> providers = JvmSettings.PID_PROVIDERS.lookupOptional(String[].class);
        if (!providers.isPresent()) {
            logger.warning(
                    "No PidProviders configured via dataverse.pid.providers. Please consider updating as older PIDProvider configuration mechanisms will be removed in a future version of Dataverse.");
        } else {
            for (String id : providers.get()) {
                //Allows spaces in PID_PROVIDERS setting
                id=id.trim();
                Optional<String> type = JvmSettings.PID_PROVIDER_TYPE.lookupOptional(id);
                if (!type.isPresent()) {
                    logger.warning("PidProvider " + id
                            + " listed in dataverse.pid.providers is not properly configured and will not be used.");
                } else {
                    String typeString = type.get();
                    if (pidProviderFactoryMap.containsKey(typeString)) {
                        PidProvider provider = pidProviderFactoryMap.get(typeString).createPidProvider(id);
                        provider.setPidProviderServiceBean(this);
                        PidUtil.addToProviderList(provider);
                    }
                }
            }
        }
        String protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol);
        String authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority);
        String shoulder = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder);
        String provider = settingsService.getValueForKey(SettingsServiceBean.Key.DoiProvider);

        if (protocol != null && authority != null && shoulder != null && provider != null) {
            logger.warning("Found legacy settings: " + protocol + " " + authority + " " + shoulder + " " + provider
                    + "Please consider updating as this PIDProvider configuration mechanism will be removed in a future version of Dataverse");
            if (PidUtil.getPidProvider(protocol, authority, shoulder) != null) {
                logger.warning(
                        "Legacy PID provider settings found - ignored since a provider for the same protocol, authority, shoulder has been registered");
            } else {
                PidProvider legacy = null;
                // Try to add a legacy provider
                String identifierGenerationStyle = settingsService
                        .getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "random");
                String dataFilePidFormat = settingsService.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat,
                        "DEPENDENT");
                switch (protocol) {
                case "doi":
                    switch (provider) {
                    case "EZID":

                        String baseUrl = JvmSettings.LEGACY_EZID_API_URL.lookup();
                        String username = JvmSettings.LEGACY_EZID_USERNAME.lookup();
                        String password = JvmSettings.LEGACY_EZID_PASSWORD.lookup();
                        PidUtil.addToProviderList(new EZIdDOIProvider("legacy", "legacy", authority, shoulder,
                                identifierGenerationStyle, dataFilePidFormat, "", "", baseUrl, username, password));

                        break;
                    case "DataCite":
                        String mdsUrl = JvmSettings.LEGACY_DATACITE_MDS_API_URL.lookup();
                        String restUrl = JvmSettings.LEGACY_DATACITE_REST_API_URL.lookup();
                        // Defaults for testing where no account is set up
                        String dcUsername = JvmSettings.LEGACY_DATACITE_USERNAME.lookup();
                        String dcPassword = JvmSettings.LEGACY_DATACITE_PASSWORD.lookup();
                        if (mdsUrl != null && restUrl != null && dcUsername != null && dcPassword != null) {
                            legacy = new DataCiteDOIProvider("legacy", "legacy", authority, shoulder,
                                    identifierGenerationStyle, dataFilePidFormat, "", "", mdsUrl, restUrl, dcUsername,
                                    dcPassword);
                        }
                        break;
                    case "FAKE":
                        logger.warning("Adding FAKE provider");
                        legacy = new FakeDOIProvider("legacy", "legacy", authority, shoulder, identifierGenerationStyle,
                                dataFilePidFormat, "", "");
                        break;
                    }
                    break;
                case "hdl":
                    int index = JvmSettings.LEGACY_HANDLENET_INDEX.lookup(Integer.class);
                    String path = JvmSettings.LEGACY_HANDLENET_KEY_PATH.lookup();
                    String passphrase = JvmSettings.LEGACY_HANDLENET_KEY_PASSPHRASE.lookup();
                    boolean independentHandleService = settingsService
                            .isTrueForKey(SettingsServiceBean.Key.IndependentHandleService, false);
                    String handleAuthHandle = settingsService.getValueForKey(SettingsServiceBean.Key.HandleAuthHandle);

                    legacy = new HandlePidProvider("legacy", "legacy", authority, shoulder, identifierGenerationStyle,
                            dataFilePidFormat, "", "", index, independentHandleService, handleAuthHandle, path,
                            passphrase);
                    break;
                case "perma":
                    String baseUrl = JvmSettings.LEGACY_PERMALINK_BASEURL.lookup();
                    legacy = new PermaLinkPidProvider("legacy", "legacy", authority, shoulder,
                            identifierGenerationStyle, dataFilePidFormat, "", "", baseUrl,
                            PermaLinkPidProvider.SEPARATOR);
                }
                if (legacy != null) {
                    legacy.setPidProviderServiceBean(this);
                    PidUtil.addToProviderList(legacy);
                }
            }
            logger.info("Have " + PidUtil.getManagedProviderIds().size() + " managed PID providers");
        }
        PidUtil.addAllToUnmanagedProviderList(Arrays.asList(new UnmanagedDOIProvider(),
                new UnmanagedHandlePidProvider(), new UnmanagedPermaLinkPidProvider()));
    }

    public String getProducer() {
        return dataverseService.getRootDataverseName();
    }

    public boolean isGlobalIdLocallyUnique(GlobalId globalId) {
        return dvObjectService.isGlobalIdLocallyUnique(globalId);
    }

    String generateNewIdentifierByStoredProcedure() {
        return dvObjectService.generateNewIdentifierByStoredProcedure();
    }

    public PidProvider getDefaultPidGenerator() {
        Optional<String> pidProviderDefaultId = JvmSettings.PID_DEFAULT_PROVIDER.lookupOptional(String.class);
        if (pidProviderDefaultId.isPresent()) {
            return PidUtil.getPidProvider(pidProviderDefaultId.get());
        } else {
            String nonNullDefaultIfKeyNotFound = "";
            String protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol,
                    nonNullDefaultIfKeyNotFound);
            String authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority,
                    nonNullDefaultIfKeyNotFound);
            String shoulder = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder,
                    nonNullDefaultIfKeyNotFound);

            return PidUtil.getPidProvider(protocol, authority, shoulder);
        }
    }

}