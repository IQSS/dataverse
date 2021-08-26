package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @Inject
    SettingsServiceBean settingService;

    @Inject
    DataverseSession session;

    @EJB
    SystemConfig systemConfig;

    private final LazyLoaded<Map<String, String>> configuredLocales = new LazyLoaded<>(this::languagesLoader);
    private final LazyLoaded<Map<String, String>> configuredAboutUrls = new LazyLoaded<>(this::aboutUrlsLoader);

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
        return systemConfig.getGuidesBaseUrl(session.getLocale());
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

    public String getDataCiteCitationsPageUrl() {
        return settingService.getValueForKey(SettingsServiceBean.Key.DoiDataCiteCitationsPageUrl);
    }

    // -------------------- LOGIC --------------------

    public Boolean isHasDropBoxKey() {
        return !getDropBoxKey().isEmpty();
    }

    public String getEnumSettingValue(SettingsServiceBean.Key key) {
        return getSettingValue(key.toString());
    }

    public String getSettingValue(String settingKey) {
        return settingService.get(settingKey);
    }

    public boolean isLocalesConfigured() {
        return configuredLocales.get().size() > 1;
    }

    public Map<String, String> getConfiguredLocales() {
        return configuredLocales.get();
    }

    public String getConfiguredLocaleName(String localeCode) {
        return configuredLocales.get().get(localeCode);
    }

    public Map<String, String> getConfiguredAboutUrls() {
        return configuredAboutUrls.get();
    }

    public boolean isDataCiteInstallation() {
        String protocol = getEnumSettingValue(SettingsServiceBean.Key.DoiProvider);
        return "DataCite".equals(protocol);
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> languagesLoader() {
        return settingService.getValueForKeyAsListOfMaps(SettingsServiceBean.Key.Languages).stream()
                .collect(toMap(getKey("locale"), getKey("title"), throwingMerger(), LinkedHashMap::new));
    }

    private Map<String, String> aboutUrlsLoader() {
        String lang = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        return settingService.getValueForKeyAsListOfMaps(SettingsServiceBean.Key.NavbarAboutUrl).stream()
                .collect(toMap(getKey("url"), getKey("title." + lang), throwingMerger(), LinkedHashMap::new));
    }

    private static Function<Map<String, String>, String> getKey(String key) {
        return map -> map.get(key);
    }
    private static BinaryOperator<String> throwingMerger() {
        return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
    }
}
