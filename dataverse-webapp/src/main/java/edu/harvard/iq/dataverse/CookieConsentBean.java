package edu.harvard.iq.dataverse;

import org.apache.commons.lang.StringUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.util.Collections;

@ManagedBean
@RequestScoped
@Named("cookieConsent")
public class CookieConsentBean {
    private static int ONE_YEAR_IN_SECONDS = 365 * 24 * 60 * 60;

    public void addCookie() {
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .addResponseCookie("ConsentCookie",
                        StringUtils.EMPTY,
                        Collections.singletonMap("maxAge", ONE_YEAR_IN_SECONDS));
    }
}
