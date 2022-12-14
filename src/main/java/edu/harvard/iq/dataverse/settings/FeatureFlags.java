package edu.harvard.iq.dataverse.settings;

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
    
    /* None yet - please add the first here. Example code:
     *
     * MY_FLAG_NAME_HERE("sth-fancy", false),
     *
     */
    
    ;
    
    final String flag;
    final boolean defaultStatus;
    
    FeatureFlags(String flag, boolean defaultStatus) {
        this.flag = flag;
        this.defaultStatus = defaultStatus;
    }
    
    public boolean enabled() {
        return JvmSettings.FEATURE_FLAG.lookupOptional(Boolean.class, flag).orElse(defaultStatus);
    }

}
