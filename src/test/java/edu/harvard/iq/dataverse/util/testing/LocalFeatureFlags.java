package edu.harvard.iq.dataverse.util.testing;

import edu.harvard.iq.dataverse.settings.FeatureFlags;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation expresses that a test class wants to manipulate local
 * settings (because the tests run within the same JVM as the code itself).
 * This is mostly true for unit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@ExtendWith(FeatureFlagExtension.class)
@Inherited
public @interface LocalFeatureFlags {
    
    FeatureFlagBroker localBroker = new FeatureFlagBroker() {
        @Override
        public Boolean get(FeatureFlags flag) {
            // In case the setting wasn't set by us, return null.
            if (System.getProperty(flag.getScopedKey()) == null) {
                return null;
            }
            
            // Otherwise: lookup via MPCONFIG as we need to take the default state into account, which is not exposed.
            return flag.enabled();
        }
        
        @Override
        public void set(FeatureFlags flag, boolean value) {
            System.setProperty(flag.getScopedKey(), String.valueOf(value));
        }
        
        @Override
        public void delete(FeatureFlags flag) {
            System.clearProperty(flag.getScopedKey());
        }
    };
    
}