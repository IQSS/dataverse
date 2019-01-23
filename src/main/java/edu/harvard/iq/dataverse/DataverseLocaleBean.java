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

    private String localeCode;
   
    // should probably be safe to remove the static block below; it was only 
    // needed because some utilities were (improperly) calling the bean in a 
    // static context. (but first let's make sure that the localeCode is actually 
    // set in the init() method!) -- L.A.
    {
        //Noticed that the NullPointerException was thrown from FacesContext.getCurrentInstance() while running the testcases(mvn:package).
        //Reason: the FacesContext is not initialized until the app starts. So, added the below if-condition
        if(FacesContext.getCurrentInstance() == null) {
            logger.info("STATIC: setting LOCALE to en");
            localeCode = "en";
        }
        else if (FacesContext.getCurrentInstance().getViewRoot() == null ) {
            localeCode = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale().getLanguage();
            logger.info("STATIC: setting LOCALE from EXTERNAL CONTEXT: "+localeCode);
        }
        else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            logger.info("STATIC: LOCALE set to en_US, using \"en\"");
            localeCode = "en";
        }
        else {
            localeCode = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
            logger.info("STATIC: setting LOCALE from VIEWROOT: "+localeCode);
        }
    }


    
    // Map from locale to display name eg     en -> English
    private Map<String, String> dataverseLocales;

    public void init() {
        
        dataverseLocales = new LinkedHashMap<>();
        try {
            JSONArray entries = new JSONArray(settingsWrapper.getValueForKey(SettingsServiceBean.Key.Languages, "[]"));
            for (Object obj : entries) {
                JSONObject entry = (JSONObject) obj;
                String locale = entry.getString("locale");
                String title = entry.getString("title");

                dataverseLocales.put(locale, title);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //localeCode = dataverseLocales.keySet().iterator().next();
        /*if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage() == "en_US") {
            localeCode = "en";
        } else {
            localeCode = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        }*/
        logger.info("INIT: LOCALE set to "+localeCode);
    }

    public Map<String, String>  getDataverseLocales(){
        if (dataverseLocales == null) {
            init();
        }
        return dataverseLocales;
    }
    
    public boolean useLocale() {
        if (dataverseLocales == null) {
            init();
        }
        return dataverseLocales.size() > 1;
    }

    public String getLocaleCode() {
        if (localeCode == null) {
            init();
        }
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleTitle() {
        if (dataverseLocales == null) {
            init();
        }
        return dataverseLocales.get(localeCode);
    }

    public void countryLocaleCodeChanged(String code) {
        if (dataverseLocales == null) {
            init();
        }
        localeCode = code;
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(code));
        try {
            String url = ((HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
    
    public void updateLocaleInViewRoot() {
        if (localeCode != null 
                && FacesContext.getCurrentInstance() != null 
                && FacesContext.getCurrentInstance().getViewRoot() != null
                && !localeCode.equals(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage())) {
            logger.info("Forcing locale in ViewRoot: "+localeCode);
            FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
        } else {
            logger.info("Cannot force locale! (it's null)");
        }
    }

}
