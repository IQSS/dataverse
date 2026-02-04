package edu.harvard.iq.dataverse.util.testing;

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
@ExtendWith(JvmSettingExtension.class)
@Inherited
public @interface LocalJvmSettings {
    
    JvmSettingBroker localBroker = new JvmSettingBroker() {
        @Override
        public String getJvmSetting(String key) {
            return System.getProperty(key);
        }
        
        @Override
        public void setJvmSetting(String key, String value) {
            System.setProperty(key, value);
        }
        
        @Override
        public String deleteJvmSetting(String key) {
            return System.clearProperty(key);
        }
    };
    
}