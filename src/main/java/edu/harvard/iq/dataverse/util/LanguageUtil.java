package edu.harvard.iq.dataverse.util;

import java.util.Locale;
import edu.harvard.iq.dataverse.LanguageBean;

public class LanguageUtil {
    
    LanguageBean language;
    Locale locale;
    
    public LanguageUtil()
    {
    	language = new LanguageBean();
		locale = language.getLocale();	
    }
    
    
    public String  getLanguage() {			
		return locale.getLanguage() ;
	}
 
 
}