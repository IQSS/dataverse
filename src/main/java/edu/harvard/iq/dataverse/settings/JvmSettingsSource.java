package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import java.util.Optional;

@Stateless
@Named
public class JvmSettingsSource implements SettingsSource<JvmSettings> {

    @Override
    public String lookup(JvmSettings settingKey) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(JvmSettings settingKey) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(JvmSettings settingKey, Class<T> klass) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(JvmSettings settingKey, Class<T> klass) {
        return Optional.empty();
    }

    @Override
    public String lookup(JvmSettings settingKey, String... arguments) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(JvmSettings settingKey, String... arguments) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(JvmSettings settingKey, Class<T> klass, String... arguments) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(JvmSettings settingKey, Class<T> klass, String... arguments) {
        return Optional.empty();
    }
}
