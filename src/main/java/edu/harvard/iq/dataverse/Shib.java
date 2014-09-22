package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@ViewScoped
@Named("Shib")
public class Shib implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(Shib.class.getCanonicalName());

    @Inject
    DataverseSession session;

    @EJB
    AuthenticationServiceBean authSvc;

    HttpServletRequest request;

    /**
     * @todo these are the attributes we are getting from the IdP at
     * testshib.org. What other attributes should we expect?
     *
     * Here is a dump from https://pdurbin.pagekite.me/Shibboleth.sso/Session
     *
     * Miscellaneous
     *
     * Session Expiration (barring inactivity): 479 minute(s)
     *
     * Client Address: 10.0.2.2
     *
     * SSO Protocol: urn:oasis:names:tc:SAML:2.0:protocol
     *
     * Identity Provider: https://idp.testshib.org/idp/shibboleth
     *
     * Authentication Time: 2014-09-12T17:07:36.137Z
     *
     * Authentication Context Class:
     * urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
     *
     * Authentication Context Decl: (none)
     *
     *
     *
     * Attributes
     *
     * affiliation: Member@testshib.org;Staff@testshib.org
     *
     * cn: Me Myself And I
     *
     * entitlement: urn:mace:dir:entitlement:common-lib-terms
     *
     * eppn: myself@testshib.org
     *
     * givenName: Me Myself
     *
     * persistent-id:
     * https://idp.testshib.org/idp/shibboleth!https://pdurbin.pagekite.me/shibboleth!zylzL+NruovU5OOGXDOL576jxfo=
     *
     * sn: And I
     *
     * telephoneNumber: 555-5555
     *
     * uid: myself
     *
     * unscoped-affiliation: Member;Staff
     */
    List<String> shibAttrs = Arrays.asList(
            "Shib-Identity-Provider",
            "uid",
            "cn",
            "sn",
            "givenName",
            "telephoneNumber",
            "eppn",
            "affiliation",
            "unscoped-affiliation",
            "entitlement",
            "persistent-id"
    );

    List<String> shibValues = new ArrayList<>();
    /**
     * @todo make this configurable?
     */
    private final String shibIdpAttribute = "Shib-Identity-Provider";
    private final String uniquePersistentIdentifier = "eppn";
    private final String displayNameAttribute = "cn";
    private final String firstNameAttribute = "givenName";
    private final String lastNameAttribute = "sn";
    private final String emailAttribute = "mail";
    private boolean debug = false;

    public void init() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        request = (HttpServletRequest) context.getRequest();
        /**
         * @todo DRY! put all these similar checks in a function
         */
        Object shibIdpObject = request.getAttribute(shibIdpAttribute);
        if (shibIdpObject == null) {
            throw new RuntimeException("Shibboleth Identity Provider attribute (" + shibIdpAttribute + ") was null");
        }
        String shibIdp = shibIdpObject.toString();
        if (shibIdp.isEmpty()) {
            throw new RuntimeException("Shibboleth Identity Provider attribute (" + shibIdpAttribute + ") was empty");
        }
        Object userIdentifierObject = request.getAttribute(uniquePersistentIdentifier);
        if (userIdentifierObject == null) {
            throw new RuntimeException("Unique persistent identifer attribute (" + uniquePersistentIdentifier + ") was null");
        }
        String userIdentifier = userIdentifierObject.toString();
        if (userIdentifier.isEmpty()) {
            throw new RuntimeException("Unique persistent identifer attribute (" + uniquePersistentIdentifier + ") was empty");
        }

        String displayName = getDisplayName(displayNameAttribute, firstNameAttribute, lastNameAttribute);
        /**
         * @todo is it ok if email address is null? What will blow up?
         */
        String emailAddress = getValueFromAttribute(emailAttribute);
        RoleAssigneeDisplayInfo displayInfo = new RoleAssigneeDisplayInfo(displayName, emailAddress);

        String userPersistentId = shibIdp + "|" + userIdentifier;
        /**
         * @todo where should "shib" be defined?
         */
        String authPrvId = "shib";
        AuthenticatedUser au = authSvc.lookupUser(authPrvId, userPersistentId);
        if (au != null) {
            logger.info("Found " + userPersistentId + ". Logging in.");
            session.setUser(au);
        } else {
            logger.info("Couldn't find " + userPersistentId + ". Creating a new user.");
            au = authSvc.createAuthenticatedUser(authPrvId, userPersistentId, displayInfo);
            session.setUser(au);
        }
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("/dataverse.xhtml");
        } catch (IOException ex) {
            Logger.getLogger(Shib.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (debug) {
            printAttributes(request);
        }
    }

    public List<String> getShibValues() {
        return shibValues;
    }

    private void printAttributes(HttpServletRequest request) {
        for (String attr : shibAttrs) {

            /**
             * @todo explain in Installers Guide that in order for these
             * attributes to be found attributePrefix="AJP_" must be added to
             * /etc/shibboleth/shibboleth2.xml like this:
             *
             * <ApplicationDefaults entityID="https://dataverse.org/shibboleth"
             * REMOTE_USER="eppn" attributePrefix="AJP_">
             *
             */
            Object attrObject = request.getAttribute(attr);
            if (attrObject != null) {
                shibValues.add(attr + ": " + attrObject.toString());
            }
        }
        logger.info("shib values: " + shibValues);
    }

    /**
     * @return The value of a Shib attribute (if non-empty) or null.
     */
    private String getValueFromAttribute(String attribute) {
        Object attributeObject = request.getAttribute(attribute);
        if (attributeObject != null) {
            String attributeValue = attributeObject.toString();
            if (!attributeValue.isEmpty()) {
                return attributeValue;
            }
        }
        return null;
    }

    /**
     * @return The best display name we can retrieve or construct based on
     * attributes received from Shibboleth. Shouldn't be null, maybe "Unknown"
     */
    private String getDisplayName(String displayNameAttribute, String firstNameAttribute, String lastNameAttribute) {
        Object displayNameObject = request.getAttribute(displayNameAttribute);
        if (displayNameObject != null) {
            String displayName = displayNameObject.toString();
            if (!displayName.isEmpty()) {
                return displayName;
            } else {
                return getDisplayNameFromFirstNameLastName(firstNameAttribute, lastNameAttribute);
            }
        } else {
            return getDisplayNameFromFirstNameLastName(firstNameAttribute, lastNameAttribute);
        }
    }

    /**
     * @return First name plus last name if available, just first name or just
     * last name or "Unknown".
     */
    private String getDisplayNameFromFirstNameLastName(String firstNameAttribute, String lastNameAttribute) {
        String firstName = getValueFromAttribute(firstNameAttribute);
        String lastName = getValueFromAttribute(lastNameAttribute);
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Unknown";
        }
    }
}
