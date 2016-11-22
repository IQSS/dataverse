package edu.harvard.iq.dataverse.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import edu.harvard.iq.dataverse.LanguageBean;

/**
 * Utility class for common JSF tasks.
 * @author michael
 */
public class JsfHelper {
	private static final Logger logger = Logger.getLogger(JsfHelper.class.getName());


	LanguageBean languageBean;
	public static final JsfHelper JH = new JsfHelper();

	public static void addSuccessMessage(String message) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash().put("successMsg", message);

	} 
	public static void addFlashMessage(String message) {
		addSuccessMessage(message);
	}
	public static void addErrorMessage(String message) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash().put("errorMsg", message);      
	} 
	public static void addInfoMessage(String message) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash().put("infoMsg", message);      
	} 
	public static void addWarningMessage(String message) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash().put("warningMsg", message);      
	} 
	public void addMessage( FacesMessage.Severity s, String summary, String details ) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, summary, details));
	}
	public void addMessage( FacesMessage.Severity s, String summary ) {
		addMessage(s, summary, "");
	}

	public <T extends Enum<T>> T enumValue( String param, Class<T> enmClass, T defaultValue ) {
		if ( param == null ) return defaultValue;
		param = param.trim();
		try {
			return Enum.valueOf(enmClass, param);
		} catch ( IllegalArgumentException iar ) {
			logger.log(Level.WARNING, "Illegal value for enum {0}: ''{1}''", new Object[]{enmClass.getName(), param});   
			return defaultValue;
		}
	}

	/**
	 * @deprecated Localization applies not only to the front end (JSF) but also
	 * the API so consider using the newer, more flexible BundleUtil methods
	 * instead.
	 */

	@Deprecated
	public String localize( String messageKey ) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		String messageBundleName = facesContext.getApplication().getMessageBundle(); 
		languageBean = new LanguageBean();
		Locale locale = languageBean.getLocale();//facesContext.getViewRoot().getLocale(); //new Locale("zh","CN") ; 
		ResourceBundle bundle = ResourceBundle.getBundle("Bundle", locale);
		return bundle.getString(messageKey);
	}

}
