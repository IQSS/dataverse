package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * ExporterRegistry is responsible for managing the registration, retrieval, and lifecycle of {@code Exporter}s.
 * It dynamically loads exporters from external JAR files and provides access to those exporters via their format names.
 * <p>
 * This class is designed as a Jakarta EJB Singleton and is initialized at application startup.
 * It uses a non-modifiable {@link Map} internally to store exporters under their format name, ensuring the state of
 * the map is always consistent and thread-safe.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Locates and loads exporter JAR files from a specified directory.</li>
 *   <li>Use {@code ServiceLoader} to discover and register {@code Exporter} implementations dynamically.</li>
 *   <li>Allows external exporters to replace internal ones for the same format name.</li>
 *   <li>Provides thread-safe access to registered exporters and their metadata.</li>
 * </ul>
 * @implNote <p>Note on Concurrency: EJB singletons use container-managed concurrency by default, where every business
 *           method implicitly runs under an exclusive {@code @Lock(LockType.WRITE)}, meaning only one caller at
 *           a time may use the bean. Since this registry is populated once in and is effectively immutable afterwards,
 *           that exclusivity is unnecessary.</p>
 *           <p>The class-level {@code @Lock(LockType.READ)} instead allows any number of callers to read from the
 *           registry concurrently, avoiding an application-wide bottleneck on exporter lookups. If a method that
 *           mutates the registry is ever added (e.g. a reload operation), it must be annotated with
 *           {@code @Lock(LockType.WRITE)} to regain exclusive access for that method.</p>
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class ExporterRegistryBean {
    
    /**
     * Represents a set of labels associated with an exporter.
     */
    public record Labels(
        String localizedDisplayName,
        String formatName
    ) {}
    
    private static final Logger logger = Logger.getLogger(ExporterRegistryBean.class.getCanonicalName());
    
    // When the class is initialized, the exporter map is an empty, non-modifiable map (key = format name).
    // Once the exporters have been located and loaded, the map is replaced, fully loaded, still unmodifiable.
    // No half-initialized state is exposable this way. Future optimizations may use @Lock on it, too, for example,
    // when implementing a reload mechanism.
    private Map<String, Exporter> exporters = Map.of();
    // Caching the classloader used to load plugin JAR files, keeping it open, will allow reuse for reloads
    // or loading more resources from plugin JARs. May be dropped later if not necessary.
    private URLClassLoader exporterClassLoader;
    
    /**
     * Retrieves an exporter associated with the specified format name.
     *
     * @param formatName the name of the format for which to retrieve the exporter
     * @return an {@code Optional} containing the exporter if found, or
     *         an empty {@code Optional} if no exporter is associated with the given format name
     */
    public Optional<Exporter> get(String formatName) {
        return Optional.ofNullable(exporters.get(formatName));
    }
    
    /**
     * Retrieves a list of all registered exporters in the system.
     * @return an unmodifiable list of {@link Exporter} instances representing all the exporters currently available
     */
    public List<Exporter> getAll() {
        return List.copyOf(exporters.values());
    }
    
    /**
     * Retrieves a list of {@link Labels} representing the exporters registered in the system.
     * @return a list of {@code Labels} objects
     */
    public List<Labels> getLabels() {
        return exporters.values().stream()
            .map(exporter -> new Labels(
                exporter.getDisplayName(BundleUtil.getCurrentLocale()),
                exporter.getFormatName()))
            .toList();
    }
    
    /**
     * Validates that an exporter is registered for the given format name.
     * Throws an exception if the format name is null or if no exporter has been registered under that name.
     *
     * @param formatName the name of the format to check; must not be null
     * @throws IllegalArgumentException if formatName is null, or if no exporter is registered for the specified format name
     */
    public void requireExists(String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("format name may not be null");
        }
        if (!exporters.containsKey(formatName)) {
            throw new IllegalArgumentException("no exporter registered for format: " + formatName);
        }
    }
    
    /**
     * Validates that every format in the provided list has a corresponding exporter registered in this registry.
     * If one or more formats are not recognized, an exception is thrown listing all invalid formats.
     *
     * @param formats the list of format names that must each have a registered exporter; must not be null;
     *                an empty list is allowed (no formats are checked)
     * @throws IllegalArgumentException if any format in the list does not have a corresponding registered exporter,
     *                                  with the message enumerating all invalid format names; or if the list is null
     */
    public void requireAllExist(List<String> formats) {
        if (formats == null) {
            throw new IllegalArgumentException("list must not be null (hint: use empty list to express 'all')");
        }
        Set<String> invalidFormats = formats.stream()
                                            .filter(format -> !exporters.containsKey(format))
                                            .collect(Collectors.toUnmodifiableSet());
        if (!invalidFormats.isEmpty()) {
            throw new IllegalArgumentException("no exporters available for " + String.join(", ", invalidFormats));
        }
    }
    
    @PostConstruct
    private void initialize() {
        /*
         * Step 1 - find the EXPORTERS dir and add all jar files there to a class loader
         */
        List<URL> jarUrls = new ArrayList<>();
        Optional<String> exportPathSetting = JvmSettings.EXPORTERS_DIRECTORY.lookupOptional(String.class);
        if (exportPathSetting.isPresent()) {
            Path exporterDir = Paths.get(exportPathSetting.get());
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
        this.exporterClassLoader = URLClassLoader.newInstance(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());
        
        /*
         * Step 2 - load all Exporters that can be found, using the jars as additional sources
         */
        ServiceLoader<Exporter> loader = ServiceLoader.load(Exporter.class, this.exporterClassLoader);
        
        /*
         * Step 3 - Fill exporterMap with providerName as the key, allow external
         * exporters to replace internal ones for the same providerName. FWIW: From the
         * logging it appears that ServiceLoader returns classes in ~ alphabetical order
         * rather than by class loader, so internal classes handling a given
         * providerName may be processed before or after external ones.
         */
        Map<String, Exporter> loadedExporters = new HashMap<>();
        loader.forEach(exp -> {
            String formatName = exp.getFormatName();
            // If no entry for this providerName yet or if it is an external exporter
            if (!exporters.containsKey(formatName) || exp.getClass().getClassLoader().equals(this.exporterClassLoader)) {
                loadedExporters.put(formatName, exp);
            }
            logger.log(
                Level.FINE,
                "SL: {0} from {1} and classloader: {2}",
                new Object[]{
                    formatName,
                    exp.getClass().getCanonicalName(),
                    exp.getClass().getClassLoader().getClass().getCanonicalName()
                });
        });
        
        // Step 4 - Create prerequisite dependency graph and verify integrity
        var requiredBy = buildAndVerifyRequirements(loadedExporters);
        // All good, (more or less) atomic updates now.
        this.exporters = loadedExporters;
        
    }
    
    @PreDestroy
    private void tearDown() {
        if (exporterClassLoader == null) {
            return;
        }
        
        try {
            exporterClassLoader.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close exporter classloader", e);
        }
    }
    
    /**
     * Builds a map of prerequisite format names to the list of export formats that depend on them.
     * For each registered exporter that declares a prerequisite format, the exporter's format name is collected
     * under the prerequisite key. (Thus exporters without a prerequisite are not included.)
     *
     * @return a map where each key is a prerequisite format name and each value is the list of format names of
     *         exporters that require that prerequisite; an empty map if no exporter declares a prerequisite
     */
    static Map<String, List<String>> buildFormatRequiredByMap(Map<String, Exporter> exporters) {
        Objects.requireNonNull(exporters);
        Map<String, List<String>> requiredByMap = new HashMap<>();
        
        for (Exporter exporter : exporters.values()) {
            exporter.getPrerequisiteFormatName().ifPresent(prereq ->
                requiredByMap
                    // Create new list if necessary
                    .computeIfAbsent(prereq, k -> new ArrayList<>())
                    // Put down exporter as depending on this format
                    .add(exporter.getFormatName()));
        }
        
        // Make a deep, read-only copy before returning
        return requiredByMap.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue())
            ));
    }
    
    /**
     * Builds the prerequisite dependency map from the given exporters and verifies that every prerequisite format
     * referenced by an exporter is itself backed by a registered exporter in the provided map.
     * In addition, it verifies no prerequisite formats form a cyclic dependency.
     *
     * @return the built prerequisite dependency map, see {@link #buildFormatRequiredByMap(Map)}.
     * @throws ExportException if one or more prerequisite format names in the dependency map
     *                         do not have a corresponding entry in the provided exporters map
     */
    static Map<String, List<String>> buildAndVerifyRequirements(Map<String, Exporter> exporters) {
        Map<String, List<String>> formatRequiredBy = buildFormatRequiredByMap(exporters);

        // Check that all prerequisite formats have a registered exporter
        if (!exporters.keySet().containsAll(formatRequiredBy.keySet())) {
            Map<String, List<String>> unsatisfied = formatRequiredBy.entrySet().stream()
                .filter(e -> !exporters.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            logger.log(Level.SEVERE, "Exporter registry integrity check failed: the following exporters are missing prerequisites: {}", unsatisfied);
            throw new ExportException("Exporter registry integrity check failed");
        }
        
        // Now that we know all exporters are present as required, check for cyclic dependencies!
        // How: a cycle exists if we revisit a format already seen within the current chain.
        // Checking against the whole chain, not just the starting format, is essential:a chain may merely lead
        // *into* a cycle it is not part of, e.g., D -> A -> B -> A.
        boolean cycleDetected = false;
        for (String startFormat : exporters.keySet()) {
            List<String> chain = new ArrayList<>();
            // Using a set here to enable O(1) lookup for seen formats.
            Set<String> seen = new HashSet<>();
            
            String current = startFormat;
            while (current != null) {
                chain.add(current);
                if (!seen.add(current)) {
                    logger.log(Level.SEVERE, "Exporter registry integrity check failed due to cyclic format dependency chain: {0}", String.join(" -> ", chain));
                    cycleDetected = true;
                    break;
                }
                // Existence was verified above, so the lookup cannot return null here.
                // If no format is detected, break the loop by returning null.
                current = exporters.get(current).getPrerequisiteFormatName().orElse(null);
            }
        }
        if (cycleDetected) {
            throw new ExportException("Exporter registry integrity check failed: cyclic dependencies detected.");
        }
        
        return formatRequiredBy;
    }
}
