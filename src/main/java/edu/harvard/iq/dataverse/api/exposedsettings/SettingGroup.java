package edu.harvard.iq.dataverse.api.exposedsettings;

import java.util.Arrays;
import java.util.List;

public class SettingGroup extends SettingItem {
    private final List<SettingItem> itemList;

    public SettingGroup(String name, List<SettingItem> itemList) {
        super(name);
        this.itemList = itemList;
    }

    public String getName() {
        return name;
    }

    private SettingItem getItemByName(String name) {
        for (SettingItem item : itemList) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public List<SettingItem> getItemList() {
        return this.itemList;
    }

    public SettingItem getItem(String[] orderedNamesRoute) {
        String subItemName = orderedNamesRoute[0];
        if (orderedNamesRoute.length == 1) {
            return getItemByName(subItemName);
        }
        for (SettingItem settingItem : itemList) {
            if (settingItem.getName().equals(subItemName)) {
                return ((SettingGroup) settingItem).getItem(Arrays.copyOfRange(orderedNamesRoute, 1, orderedNamesRoute.length));
            }
        }
        return null;
    }
}
