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
