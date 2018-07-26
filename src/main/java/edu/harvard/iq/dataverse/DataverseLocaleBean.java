package edu.harvard.iq.dataverse;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.json.*;

@Named
@javax.enterprise.context.SessionScoped

public class DataverseLocaleBean implements Serializable {


    private static final Logger logger = Logger.getLogger(DataverseLocaleBean.class.getCanonicalName());

    private List<DataverseLocale> dataverseLocales;
    private static Map<String,Object> languages;

    String json_url= "http://localhost:8080/resources/lang/languages.json";

    JSONObject json_obj;


    private String localeCode;
    private Locale locale;

    FacesContext context = FacesContext.getCurrentInstance();

    {
        if (context== null){
            locale= new Locale ("");
        } else if(context.getViewRoot().getLocale().getLanguage()== "en_US" || context.getViewRoot().getLocale().getLanguage()== "en"){
            locale= new Locale ("");
        }else{
            locale= context.getViewRoot().getLocale();
        }
    }


    public DataverseLocaleBean(){

    }

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }


    public List<DataverseLocale> getCountriesInMap() {
        dataverseLocales = new ArrayList<DataverseLocale>();

        try {
            json_obj = new JSONObject(readUrl(json_url));
            JSONArray json_array = json_obj.getJSONArray("languages");
            for (int i = 0; i < json_array.length(); i++) {
                String title = json_array.getJSONObject(i).getString("title");
                String localeStr = json_array.getJSONObject(i).getString("locale");

                dataverseLocales.add(new DataverseLocale(i , title, new Locale(localeStr)));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dataverseLocales;
    }

    public Locale getLocale(){
        return this.locale;
    }

    public void setLocale(Locale _locale){
        this.locale = _locale;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public void countryLocaleCodeChanged(ValueChangeEvent e){
        String id = e.getNewValue().toString();

        for (Iterator<DataverseLocale> it = dataverseLocales.iterator(); it.hasNext(); ) {
            DataverseLocale dL = it.next();

            if (dL.getId().toString().equals(id)) {
                FacesContext.getCurrentInstance()
                        .getViewRoot().setLocale((Locale) dL.getName());

                setLocaleCode(dL.getId().toString());
                setLocale((Locale) dL.getName());
             }
        }

        try {

            String url = ((HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }





}

