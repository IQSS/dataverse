package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import java.util.Optional;

@Stateless
@Named
public class DatabaseSettingsSource implements SettingsSource<SettingsServiceBean.Key> {

    @Override
    public String lookup(SettingsServiceBean.Key settingKey) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(SettingsServiceBean.Key settingKey) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(SettingsServiceBean.Key settingKey, Class<T> klass) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(SettingsServiceBean.Key settingKey, Class<T> klass) {
        return Optional.empty();
    }

    @Override
    public String lookup(SettingsServiceBean.Key settingKey, String... arguments) {
        return null;
    }

    @Override
    public Optional<String> lookupOptional(SettingsServiceBean.Key settingKey, String... arguments) {
        return Optional.empty();
    }

    @Override
    public <T> T lookup(SettingsServiceBean.Key settingKey, Class<T> klass, String... arguments) {
        return null;
    }

    @Override
    public <T> Optional<T> lookupOptional(SettingsServiceBean.Key settingKey, Class<T> klass, String... arguments) {
        return Optional.empty();
    }
}
