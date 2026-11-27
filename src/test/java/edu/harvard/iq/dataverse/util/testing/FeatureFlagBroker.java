package edu.harvard.iq.dataverse.util.testing;

import edu.harvard.iq.dataverse.settings.FeatureFlags;

import java.io.IOException;

/**
 * Provide an interface to access and manipulate {@link edu.harvard.iq.dataverse.settings.FeatureFlags}
 * at some place (local, remote, different ways to access, etc.).
 * Part of the {@link FeatureFlagExtension} extension to allow JUnit5 tests to manipulate these
 * settings, enabling to test different code paths and so on.
 * @implNote Keep in mind to use methods that do not require restarts or similar to set or delete a setting.
 *           This must be changeable on the fly, otherwise it will be useless for testing.
 *           Yes, non-hot-reloadable settings may be a problem. The code should be refactored in these cases.
 */
public interface FeatureFlagBroker {
    
    /**
     * Receive the status of a {@link edu.harvard.iq.dataverse.settings.FeatureFlags}.
     * @param flag The feature flag to receive
     * @return The status of the flag (false = disabled, true = enabled, null = not set)
     * @throws IOException When communication goes sideways.
     */
    Boolean get(FeatureFlags flag) throws IOException;
    
    /**
     * Set the state of a {@link edu.harvard.iq.dataverse.settings.FeatureFlags}.
     * @param flag The feature flag to set
     * @param value The flag state we want to have it set to.
     * @throws IOException When communication goes sideways.
     */
    void set(FeatureFlags flag, boolean value) throws IOException;
    
    /**
     * Remove the state of a {@link edu.harvard.iq.dataverse.settings.FeatureFlags}.
     * For some tests, one might want to clear a certain setting again and potentially have it set back afterward.
     * @param flag The feature flag to delete.
     * @throws IOException When communication goes sideways.
     */
    void delete(FeatureFlags flag) throws IOException;
    
}