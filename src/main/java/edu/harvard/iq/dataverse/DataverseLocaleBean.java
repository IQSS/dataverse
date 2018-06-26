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


    private static final Logger logger = Logger.getLogger(DataverseLocale.class.getCanonicalName());

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
            logger.info("  step1 ***");
        } else if(context.getViewRoot().getLocale().getLanguage()== "en_US" || context.getViewRoot().getLocale().getLanguage()== "en"){
            locale= new Locale ("");
            logger.info("  step2 ***");
        }else{
            locale= context.getViewRoot().getLocale();
            logger.info("  step3 ***");
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

    public Map<String, Object> getCountriesInMap_Bkup() {

        if(languages == null) {
            try {

                languages = new LinkedHashMap<String, Object>();


                json_obj = new JSONObject(readUrl(json_url));
                //note the saved affiliation is the "title" of the affiliates.json file

                JSONArray json_array = json_obj.getJSONArray("languages");
                // logger.info("  loading file ********************* " + json_url + "******* " + json_array.length());
                for (int i = 0; i < json_array.length(); i++) {
                    String title = json_array.getJSONObject(i).getString("title");
                    String localeStr = json_array.getJSONObject(i).getString("locale");

                    languages.put(title, new Locale(localeStr));
                    //logger.info("  put languages ********************* " + title + " ** " + localeStr + " ** " + i);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return languages;
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
                //logger.info("  put countries ********************* " + title + " ** " + localeStr + " ** " + i);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dataverseLocales;
    }

    public Locale getLocale(){
        logger.info("  getting the locale  ********************* " + this.locale );
        return this.locale;
    }

    public void setLocale(Locale _locale){
        logger.info("  Setting the locale  ********************* " + _locale   );
        this.locale = _locale;
    }

    public String getLocaleCode() {

        logger.info("  getting the localeCode ********************* " + localeCode );
        return localeCode;
    }


    public void setLocaleCode(String localeCode) {

        logger.info("  Setting the localeCode ********************* " + localeCode   );
        this.localeCode = localeCode;

    }


    public void countryLocaleCodeChanged_BKup(ValueChangeEvent e){


        String newLocaleValue = e.getNewValue().toString();

        //System.out.println( "Changing locale value " + newLocaleValue );
        logger.info("  Changing locale value ********************* " + newLocaleValue );


        //loop a map to compare the locale code
        for (Map.Entry<String, Object> entry : languages.entrySet()) {

            if(entry.getValue().toString().equals(newLocaleValue)){

                FacesContext.getCurrentInstance()
                        .getViewRoot().setLocale((Locale) entry.getValue());

                setLocaleCode(newLocaleValue);
                setLocale((Locale) entry.getValue());

                logger.info("  Changed to locale value ********************* " +  entry.getValue() );
            }
        }

        try{

            String url = ((HttpServletRequest)FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

            //logger.info("  Changed to locale value ********************* " +  locale );

        }catch(IOException ioe){
            logger.log(Level.SEVERE, "rediect error", ioe);
        }

    }


    public void countryLocaleCodeChanged(ValueChangeEvent e){
        logger.info("  countryLocaleCodeChanged ********************* "    );

        //DataverseLocale newValue = (DataverseLocale)e.getNewValue();
        //String lang = newValue.getDisplayName();

        String id = e.getNewValue().toString();

        logger.info("  Changing locale value ********************* " + id );

        for (Iterator<DataverseLocale> it = dataverseLocales.iterator(); it.hasNext(); ) {
            DataverseLocale dL = it.next();

            logger.info("   ********************* " );
            logger.info("  get dL.getDisplayName()********************* " + dL.getDisplayName() );
            logger.info("  get dL.getName()********************* " + dL.getName() );
            logger.info("  get dL.getId()********************* " + dL.getId() );
            logger.info("   ********************* " );

            if (dL.getId().toString().equals(id)) {

                FacesContext.getCurrentInstance()
                        .getViewRoot().setLocale((Locale) dL.getName());

                setLocaleCode(dL.getId().toString());
                setLocale((Locale) dL.getName());

                logger.info("  Changed to locale value ********************* " +  dL.getDisplayName()  );
            }
        }

        try {

            String url = ((HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest()).getHeader("referer");
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);

            logger.info("  forwarded to locale value ********************* " +  locale );

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }





}

