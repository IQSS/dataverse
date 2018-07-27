package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.json.*;

@Named
@javax.enterprise.context.SessionScoped
public class DataverseLocaleBean implements Serializable {

    private static final Logger logger = Logger.getLogger(DataverseLocaleBean.class.getCanonicalName());

    @Inject
    SettingsWrapper settingsWrapper;

    // Map from locale to display name eg     en -> English
    private Map<String, String> dataverseLocales;

    private String localeCode;

    FacesContext context = null;//FacesContext.getCurrentInstance();

    {
        if (context == null || context.getViewRoot().getLocale().getLanguage() == "en_US") {
            localeCode = "en";
        } else {
            localeCode = context.getViewRoot().getLocale().getLanguage();
        }
    }

    public DataverseLocaleBean() {

    }

    public Map<String, String> getCountriesMap() {
        dataverseLocales = new LinkedHashMap<>();

        try {
            JSONArray entries = new JSONArray(settingsWrapper.getValueForKey(SettingsServiceBean.Key.Languages, "[]"));
            for (Object obj : entries) {
                JSONObject entry = (JSONObject) obj;
                String title = entry.getString("title");
                String locale = entry.getString("locale");

                dataverseLocales.put(locale, title);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dataverseLocales;
    }

    public boolean useLocale() {
        if (dataverseLocales == null) {
            getCountriesMap();
        }
        return dataverseLocales.size() > 1;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleTitle() {
        if (dataverseLocales == null) {
            getCountriesMap();
        }
        return dataverseLocales.get(localeCode);
    }

    public void countryLocaleCodeChanged(String code) {
        if (dataverseLocales == null) {
            getCountriesMap();
        }
        localeCode = code;
        FacesContext.getCurrentInstance()
                .getViewRoot().setLocale(new Locale(dataverseLocales.get(code)));
        try {
            String url = ((HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

}
