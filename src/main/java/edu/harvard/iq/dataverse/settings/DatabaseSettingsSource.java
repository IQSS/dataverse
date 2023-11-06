package edu.harvard.iq.dataverse.settings;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Stateless
@Named
public class DatabaseSettingsSource implements SettingsSource<SettingsServiceBean.Key> {

    @Inject
    SettingsServiceBean settingsService;

    @Override
    public <T> T lookup(SettingsServiceBean.Key settingKey, Class<T> klass, T defaultValue) {
        // TODO params and default value support
        return (T) settingsService.getValueForKey(settingKey);
    }
}
