package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

public interface ConfigurableSearchService extends SearchService {
    void setSettingsService(SettingsServiceBean settingsService);
}