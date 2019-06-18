/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @EJB
    SettingsServiceBean settingService;

    @EJB
    SystemConfig systemConfig;

    private Map<String, String> settingsMap;
    private Map<String, String> configuredLocales;

    // -------------------- GETTERS --------------------

    public boolean isPublicInstall() {
        return settingService.isTrueForKey(SettingsServiceBean.Key.PublicInstall);
    }

    public String getMetricsUrl() {
        return settingService.getValueForKey(SettingsServiceBean.Key.MetricsUrl);
    }

    public boolean isShibPassiveLoginEnabled() {
        return settingService.isTrueForKey(SettingsServiceBean.Key.ShibPassiveLoginEnabled);
    }

    public boolean isProvCollectionEnabled() {
        return settingService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled);
    }

    public boolean isRsyncUpload() {
        return systemConfig.isRsyncUpload();
    }

    public boolean isRsyncDownload() {
        return systemConfig.isRsyncDownload();
    }

    public boolean isRsyncOnly() {
        return systemConfig.isRsyncOnly();
    }

    public boolean isHTTPUpload() {
        return systemConfig.isHTTPUpload();
    }

    public Integer getUploadMethodsCount() {
        return systemConfig.getUploadMethodCount();
    }

    public String getGuidesBaseUrl() {
        return systemConfig.getGuidesBaseUrl();
    }

    public String getGuidesVersion() {
        return systemConfig.getGuidesVersion();
    }

    public String getDropBoxKey() {

        String configuredDropBoxKey = getSettingValue(SettingsServiceBean.Key.DropboxKey.toString());
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return "";
    }

    // -------------------- LOGIC --------------------

    public Boolean isHasDropBoxKey() {

        return !getDropBoxKey().isEmpty();
    }

    public String getSettingValue(String settingKey) {
        if (settingsMap == null) {
            initSettingsMap();
        }

        return settingsMap.get(settingKey);
    }

    public boolean isLocalesConfigured() {
        if (configuredLocales == null) {
            initLocaleSettings();
        }
        return configuredLocales.size() > 1;
    }

    public Map<String, String> getConfiguredLocales() {
        if (configuredLocales == null) {
            initLocaleSettings();
        }
        return configuredLocales;
    }

    public String getConfiguredLocaleName(String localeCode) {
        if (configuredLocales == null) {
            initLocaleSettings();
        }
        return configuredLocales.get(localeCode);
    }

    // -------------------- PRIVATE --------------------

    private void initLocaleSettings() {

        configuredLocales = new LinkedHashMap<>();

        try {
            JSONArray entries = new JSONArray(getSettingValue(SettingsServiceBean.Key.Languages.toString()));
            for (Object obj : entries) {
                JSONObject entry = (JSONObject) obj;
                String locale = entry.getString("locale");
                String title = entry.getString("title");

                configuredLocales.put(locale, title);
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            // do we want to know? - probably not
        }
    }

    private void initSettingsMap() {
        // initialize settings map
        settingsMap = settingService.listAll();
    }
}

