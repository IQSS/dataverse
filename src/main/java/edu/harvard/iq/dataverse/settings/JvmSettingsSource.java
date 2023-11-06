package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import java.util.Optional;

@Stateless
@Named
public class JvmSettingsSource implements SettingsSource<JvmSettings> {

    @Override
    public String lookup(JvmSettings settingKey) {
        return settingKey.lookup();
    }

    @Override
    public Optional<String> lookupOptional(JvmSettings settingKey) {
        return settingKey.lookupOptional();
    }

    @Override
    public <T> T lookup(JvmSettings settingKey, Class<T> klass) {
        return settingKey.lookup(klass);
    }

    @Override
    public <T> Optional<T> lookupOptional(JvmSettings settingKey, Class<T> klass) {
        return settingKey.lookupOptional(klass);
    }

    @Override
    public String lookup(JvmSettings settingKey, String... arguments) {
        return settingKey.lookup(arguments);
    }

    @Override
    public Optional<String> lookupOptional(JvmSettings settingKey, String... arguments) {
        return settingKey.lookupOptional(arguments);
    }

    @Override
    public <T> T lookup(JvmSettings settingKey, Class<T> klass, String... arguments) {
        return settingKey.lookup(klass, arguments);
    }

    @Override
    public <T> Optional<T> lookupOptional(JvmSettings settingKey, Class<T> klass, String... arguments) {
        return settingKey.lookupOptional(klass, arguments);
    }
}
