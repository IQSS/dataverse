package edu.harvard.iq.dataverse;


import java.io.Serializable;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import javax.faces.component.UIViewRoot;
 
@ManagedBean(name="language")
@SessionScoped
public class LanguageBean implements Serializable{
    private static final Logger logger = Logger.getLogger(LanguageBean.class.getName());
    private static final long serialVersionUID = 1L;

    static FacesContext context = FacesContext.getCurrentInstance();
    static private Locale locale;
    static {
        if (context== null)
          locale= new Locale ("en_US");
        else
          locale= context.getViewRoot().getLocale();
    }
	
    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return locale.getLanguage();
    }

    public void changeLanguage(String language) {
    	//logger.log(Level.INFO,"----------------TEST   "+ language);
        locale = new Locale(language);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(language));
        //logger.log(Level.INFO,JH.localize("login.invaliduserpassword"));
    }
}