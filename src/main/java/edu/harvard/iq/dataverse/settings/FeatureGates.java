package edu.harvard.iq.dataverse.settings;

/**
 * <p>This enum holds so-called "feature gates" aka "feature flags". It can be used throughout the application
 * to avoid activating or using experimental functionality or feature previews that are opt-in.</p>
 *
 * <p>The current implementation reuses {@link JvmSettings} to interpret any
 * <a href="https://download.eclipse.org/microprofile/microprofile-config-3.0/microprofile-config-spec-3.0.html#_built_in_converters">boolean values</a>
 * (true == case-insensitive one of "true", "1", "YES", "Y", "ON") and hook into the usual settings system
 * (any MicroProfile Config Source available).</p>
 *
 * If you add any new gates, please add a setting in JvmSettings, think of a default status, add some Javadocs
 * about the gated feature and add a "@since" tag to make it easier to identify when a gate has been introduced.
 *
 */
public enum FeatureGates {
    
    /**
     * Enabling will unblock access to the API with an OIDC access token in addition to other available methods.
     * @apiNote Open gate by setting "dataverse.feature.api-oidc-access"
     * @since Dataverse 5.13
     * @see JvmSettings#GATE_API_OIDC_ACCESS
     */
    API_OIDC_ACCESS(JvmSettings.GATE_API_OIDC_ACCESS, false),
    
    ;
    
    final JvmSettings setting;
    final boolean defaultStatus;
    
    FeatureGates(JvmSettings setting, boolean defaultStatus) {
        this.setting = setting;
        this.defaultStatus = defaultStatus;
    }
    
    public boolean enabled() {
        return setting.lookupOptional(Boolean.class).orElse(defaultStatus);
    }

}
