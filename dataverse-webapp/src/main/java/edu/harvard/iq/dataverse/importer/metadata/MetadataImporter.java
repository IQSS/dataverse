package edu.harvard.iq.dataverse.importer.metadata;

import java.util.Collections;
import java.util.List;
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
     * Returns the programmatic description of the form that is used to acquire needed data from
     * the user.
     */
    ImporterData getImporterData();

    /**
     * Should return the data collected by the importer. The structure of the output should be
     * following: <p><ul>
     * <li>Each {@link ResultField} could store name and value, and could also contain child ResultFields,
     * <li>Child ResultFields should only be used in case of compound metadata fields or controlled vocabularies,
     * <li><b>Name</b> of the field must match that contained in TSV metadata file. Moreover, the subfield of compound
     * field has to be contained in proper parent ResultField, even if it's the only provided value for that compound field
     * (for example if we want only provide <i>authorAffiliation</i> for the compoud field <i>author</i>, we have to create
     * ResultField with name <i>author</i> containing one child ResultField with name <i>authorAffiliation</i>),
     * <li>In the case of vocabulary fields we create ResultField with vocabulary name and child ResultField(s) with
     * <u>empty</u> names and values taken from vocabulary values. Even if we want to pass <u>only one</u> value for the given
     * vocabulary we have to put it as a child item and not the direct value.
     */
    List<ResultField> fetchMetadata(Map<ImporterFieldKey, Object> importerInput);

    /**
     * Should return the map of field keys ({@link ImporterFieldKey}) and message keys (that are to be taken from importer's
     * bundle) for those input values that failed validation. Required fields will be sent to this method only in case they are
     * filled – if not they will be handled by the mail application. However non-required fields will be passed even if they're empty.
     */
    default Map<ImporterFieldKey, String> validate(Map<ImporterFieldKey, Object> importerInput) {
        return Collections.emptyMap();
    }

    /**
     * Returns the maximal size of uploaded to importer file(s) in bytes.
     * Zero means that any size is allowed.
     */
    default long getMaxUploadedFileSize() {
        return 0L;
    }
}