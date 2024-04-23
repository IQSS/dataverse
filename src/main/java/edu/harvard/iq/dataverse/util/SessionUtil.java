package edu.harvard.iq.dataverse.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionUtil {

    /**
	 * Changes the session id (jsessionId) - for use when the session's authority increases (i.e. at login)
	 * Servlet 3.1 Note: This method is needed while using Servlets 2.0. 3.1 has a HttpServletRequest.chageSessionId(); method that can be used instead.
	 * 
	 * @param h the current HttpServletRequest
     * e.g. for pages you can get this from (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
     */
    public static void changeSessionId(HttpServletRequest h) {
        HttpSession session = h.getSession(false);
        HashMap<String, Object> sessionAttributes = new HashMap<String,Object>();
        for(Enumeration<String> e = session.getAttributeNames();e.hasMoreElements();) {
        	String name = e.nextElement();
        	sessionAttributes.put(name, session.getAttribute(name));
        }
        h.getSession().invalidate();
        session = h.getSession(true);
        for(Entry<String, Object> entry: sessionAttributes.entrySet()) {
        	session.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}
