package edu.harvard.iq.dataverse.api.exposedsettings;

public abstract class SettingItem {
    protected String name;

    public SettingItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
