package edu.harvard.iq.dataverse.api.exposedsettings;

public class Setting<T> extends SettingItem {
    private final T value;

    public Setting(String name, T value) {
        super(name);
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
