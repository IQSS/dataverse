package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import java.util.Optional;

@Stateless
@Named
public class DatabaseSettingsSource implements SettingsSource {
    @Override
    public String lookup(String settingKey) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(String settingKey) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(String settingKey, Class<T> klass) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(String settingKey, Class<T> klass) {
        return Optional.empty();
    }

    @Override
    public String lookup(String settingKey, String... arguments) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(String settingKey, String... arguments) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(String settingKey, Class<T> klass, String... arguments) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(String settingKey, Class<T> klass, String... arguments) {
        return Optional.empty();
    }
}
