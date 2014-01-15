package edu.harvard.iq.dataverse.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Utility class for common JSF tasks.
 * @author michael
 */
public class JsfHelper {
	
	public static final JsfHelper JH = new JsfHelper();
	
	public void addMessage( FacesMessage.Severity s, String summary, String details ) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, summary, details));
	}
	public void addMessage( FacesMessage.Severity s, String summary ) {
		addMessage(s, summary, "");
	}
	
}
