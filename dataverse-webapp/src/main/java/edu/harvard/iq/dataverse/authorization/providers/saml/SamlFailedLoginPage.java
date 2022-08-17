package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.authorization.SamlLoginIssue;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

@ViewScoped
@Named("FailedLogin")
public class SamlFailedLoginPage implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SamlFailedLoginPage.class);

    private SamlLoginIssue samlLoginIssue;

    // -------------------- GETTERS --------------------

    public SamlLoginIssue getSamlLoginIssue() {
        return samlLoginIssue;
    }

    // -------------------- LOGIC --------------------

    public void init() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        SamlLoginIssue loginIssue = session != null
                ? (SamlLoginIssue) session.getAttribute(SamlAuthenticationServlet.SAML_LOGIN_ISSUE_SESSION_PARAM) : null;
        if (loginIssue != null) {
            samlLoginIssue = loginIssue;
            session.removeAttribute(SamlAuthenticationServlet.SAML_LOGIN_ISSUE_SESSION_PARAM);
        } else {
            logger.warn("Cannot find session or original login issue. Using AUTHENTICATION_ERROR.");
            samlLoginIssue = new SamlLoginIssue(SamlLoginIssue.Type.AUTHENTICATION_ERROR);
        }
        String errorMessage = BundleUtil.getStringFromBundle("failed.login.cause." + samlLoginIssue.type.toString().toLowerCase().replace("_", "."));
        JsfHelper.addErrorMessage(errorMessage, "");
        for (String message : samlLoginIssue.messages) {
            JsfHelper.addWarningMessage(message, "");
        }
    }
}
