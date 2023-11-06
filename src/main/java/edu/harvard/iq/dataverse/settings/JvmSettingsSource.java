package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

@Stateless
@Named
public class JvmSettingsSource implements SettingsSource<JvmSettings> {

    @Override
    public <T> T lookup(JvmSettings settingKey, Class<T> klass, T defaultValue) {
        if (defaultValue != null) {
            return settingKey.lookupOptional(klass).orElse(defaultValue);
        }
        return settingKey.lookup(klass);
        //TODO parameters support
    }
}
