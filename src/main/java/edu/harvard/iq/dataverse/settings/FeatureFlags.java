package edu.harvard.iq.dataverse.settings;

import java.util.Objects;

/**
 * <p>
 *     This enum holds so-called "feature flags" aka "feature gates", etc. It can be used throughout the application
 *     to avoid activating or using experimental functionality or feature previews that are opt-in.
 * </p><p>
 *     The current implementation reuses {@link JvmSettings} to interpret any
 *     <a href="https://download.eclipse.org/microprofile/microprofile-config-3.0/microprofile-config-spec-3.0.html#_built_in_converters">boolean values</a>
 *     (true == case-insensitive one of "true", "1", "YES", "Y", "ON") and hook into the usual settings system
 *     (any MicroProfile Config Source available).
 * </p><p>
 *     If you add any new flags, please add it here, think of a default status, add some Javadocs about the flagged
 *     feature and add a "@since" tag to make it easier to identify when a flag has been introduced.
 * </p><p>
 *     When removing a flag because of a feature being removed, drop the entry. When a feature matures, drop the flag,
 *     too! Flags are not meant to be switches for configuration!
 * </p>
 *
 * @see <a href="https://guides.dataverse.org/en/latest/installation/config.html#feature-flags">Configuration Guide</a>
 * @see <a href="https://guides.dataverse.org/en/latest/developers/configuration.html#adding-a-feature-flag">Developer Guide</a>
 */
public enum FeatureFlags {

    /**
     * Enables API authentication via session cookie (JSESSIONID). Caution: Enabling this feature flag exposes the installation to CSRF risks
     * @apiNote Raise flag by setting "dataverse.feature.api-session-auth"
     * @since Dataverse 5.14
     */
    API_SESSION_AUTH("api-session-auth"),
    /**
     * Enables API authentication via Bearer Token.
     * @apiNote Raise flag by setting "dataverse.feature.api-bearer-auth"
     * @since Dataverse @TODO:
     */
    API_BEARER_AUTH("api-bearer-auth"),
    /**
     * For published (public) objects, don't use a join when searching Solr. 
     * Experimental! Requires a reindex with the following feature flag enabled,
     * in order to add the boolean publicObject_b:true field to all the public
     * Solr documents. 
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.avoid-expensive-solr-join"
     * @since Dataverse 6.3
     */
    AVOID_EXPENSIVE_SOLR_JOIN("avoid-expensive-solr-join"),
    /**
     * With this flag enabled, the boolean field publicObject_b:true will be 
     * added to all the indexed Solr documents for publicly-available collections,
     * datasets and files. This flag makes it possible to rely on it in searches,
     * instead of the very expensive join (the feature flag above).
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.add-publicobject-solr-field"
     * @since Dataverse 6.3
     */
    ADD_PUBLICOBJECT_SOLR_FIELD("add-publicobject-solr-field"),
    /**
     * With this flag set, Dataverse will index the actual origin of harvested
     * metadata records, instead of the "Harvested" string in all cases. 
     * 
     * @apiNote Raise flag by setting
     * "dataverse.feature.index-harvested-metadata-source"
     * @since Dataverse 6.3
     */
    INDEX_HARVESTED_METADATA_SOURCE("index-harvested-metadata-source"),

    /**
     * Dataverse normally deletes all solr documents related to a dataset's files
     * when the dataset is reindexed. With this flag enabled, additional logic is
     * added to the reindex process to delete only the solr documents that are no
     * longer needed. (Required docs will be updated rather than deleted and 
     * replaced.) Enabling this feature flag should make the reindex process 
     * faster without impacting the search results.
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.reduce-solr-deletes"
     * @since Dataverse 6.3
     */
    REDUCE_SOLR_DELETES("reduce-solr-deletes"),
    /**
     * With this flag enabled, the Return To Author pop-up will not have a required
     * "Reason" field, and a reason will not be required in the 
     * /api/datasets/{id}/returnToAuthor api call.
     * 
     * @apiNote Raise flag by setting
     * "dataverse.feature.disable-return-to-author-reason"
     * @since Dataverse 6.3
     */
    DISABLE_RETURN_TO_AUTHOR_REASON("disable-return-to-author-reason"),
    /**
     * This flag disables the feature that automatically selects one of the 
     * DataFile thumbnails in the dataset/version as the dedicated thumbnail
     * for the dataset.
     * 
     * @apiNote Raise flag by setting
     * "dataverse.feature.enable-dataset-thumbnail-autoselect"
     * @since Dataverse 6.4
     */
    DISABLE_DATASET_THUMBNAIL_AUTOSELECT("disable-dataset-thumbnail-autoselect"),
    /**
     * Feature flag for the new Globus upload framework.
     */
    GLOBUS_USE_EXPERIMENTAL_ASYNC_FRAMEWORK("globus-use-experimental-async-framework"),
    ;
    
    final String flag;
    final boolean defaultStatus;
    
    /**
     * Construct a flag with default status "off".
     *
     * @param flag This flag name will be used to create a scoped String with {@link JvmSettings#FEATURE_FLAG},
     *             making it available as "dataverse.feature.${flag}".
     */
    FeatureFlags(String flag) {
        this(flag, false);
    }
    
    /**
     * Construct a flag.
     * @param flag This flag name will be used to create a scoped String with {@link JvmSettings#FEATURE_FLAG},
     *             making it available as "dataverse.feature.${flag}".
     * @param defaultStatus A sensible default should be given here. Probably this will be "false" for most
     *                      experimental feature previews.
     */
    FeatureFlags(String flag, boolean defaultStatus) {
        Objects.requireNonNull(flag);
        this.flag = flag;
        this.defaultStatus = defaultStatus;
    }
    
    /**
     * Determine the status of this flag via {@link JvmSettings}.
     * @return True or false, depending on the configuration or {@link #defaultStatus} if not found.
     */
    public boolean enabled() {
        return JvmSettings.FEATURE_FLAG.lookupOptional(Boolean.class, flag).orElse(defaultStatus);
    }

}
