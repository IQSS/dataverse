package edu.harvard.iq.dataverse;

import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@Named
@javax.enterprise.context.SessionScoped
public class DataverseLocaleBean implements Serializable {

    private static final Logger logger = Logger.getLogger(DataverseLocaleBean.class.getCanonicalName());

    public static final String DEFAULT_LOCALE = "en";

    private static Map<String, String> dataverseLocales;

    static {
        dataverseLocales = new LinkedHashMap<>();
        dataverseLocales.put(DEFAULT_LOCALE, "English");
        dataverseLocales.put("pl", "Polski");
    }

    {
        //Noticed that the NullPointerException was thrown from FacesContext.getCurrentInstance() while running the testcases(mvn:package).
        //Reason: the FacesContext is not initialized until the app starts. So, added the below if-condition
        if(FacesContext.getCurrentInstance() == null) {
            localeCode = DEFAULT_LOCALE;
        }
        else if (FacesContext.getCurrentInstance().getViewRoot() == null ) {
            localeCode = defaultLocale(FacesContext.getCurrentInstance().getExternalContext().getRequestLocale().getLanguage());
        }
        else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            localeCode = DEFAULT_LOCALE;
        }
        else {
            localeCode = defaultLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage());
        }
    }

    private String defaultLocale(String language) {
        if (!dataverseLocales.containsKey(language)) {
            return DEFAULT_LOCALE;
        }
        return language;
    }

    private String localeCode;

    public Map<String, String>  getDataverseLocales(){
        return dataverseLocales;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleTitle() {
        return dataverseLocales.get(localeCode);
    }

    public Locale getLocale() {
        return new Locale(localeCode);
    }

    public void countryLocaleCodeChanged(String code) {
        localeCode = defaultLocale(code);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(defaultLocale(code)));
        try {
            String url = ((HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
