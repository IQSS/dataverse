package edu.harvard.iq.dataverse.importer.metadata;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Interface used for custom metadata importers.
 * <p>
 * In order to work properly every implementation should:
 * <p><ol>
 * <li>be annotated with {@code @Eager} (from omnifaces) and {@code @ApplicationScoped},
 * <li>contain an injected instance of {@link ImporterRegistry} and call its {@code register(…)}
 * method in some method annotated with {@code @PostConstruct}.
 * </ol></p>
 */
public interface MetadataImporter {
    /**
     * Returns the name of metadata block which metadata imported by this importer
     * belong to. The application will filter out the importers that are not usable with
     * selected dataverse.
     */
    String getMetadataBlockName();

    /**
     * Returns the bundle used for i18n. This bundle should at least have keys for importer
     * name and description ({@code importer.name} and {@code importer.description} respectively).
     */
    ResourceBundle getBundle(Locale locale);

    /**
     * TODO: to fill after finishing implementation in the app
     */
    ImporterData getImporterData();

    /**
     * TODO: to fill after finishing implementation in the app
     */
    Map<Object, Object> fetchMetadata(ImporterInput importerInput);
}