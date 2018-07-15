package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory-backed SettingsBean, for tests.
 * @author michael
 */
public class TestSettingsServiceBean extends SettingsServiceBean {
    
    private final Map<String, String> settings = new HashMap<>();

    @Override
    public void delete(String name) {
        settings.remove(name);
    }

    @Override
    public Setting set(String name, String content) {
        settings.put(name, content);
        return new Setting(name, content);
    }

    @Override
    public String get(String name) {
        return settings.get(name);
    }

}
