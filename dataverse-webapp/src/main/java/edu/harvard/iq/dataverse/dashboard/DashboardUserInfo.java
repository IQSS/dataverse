package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Logger;

public class DashboardUserInfo implements Serializable {

    private static final Logger logger = Logger.getLogger(DashboardUserInfo.class.getCanonicalName());
    private static final long serialVersionUID = 8350493065070981744L;

    private String id;
    private String identifier;
    private String name;
    private String email;
    private String affiliation;
    private String roles;
    private boolean superuser;
    private String authenticator;
    private boolean isEmailVerified;
    private String notificationsLanguage;

    // -------------------- CONSTRUCTORS --------------------

    public DashboardUserInfo() {
    }

    public DashboardUserInfo(AuthenticatedUser user, String authenticator, boolean isEmailVerified, String notificationsLanguage) {
        this.id = user.getId().toString();
        this.identifier = user.getUserIdentifier();
        this.name = user.getLastName().trim() + ", " + user.getFirstName().trim();
        this.email = user.getEmail();
        this.affiliation = user.getAffiliation();
        this.roles = user.getRoles();
        this.superuser = user.isSuperuser();
        this.authenticator = authenticator;
        this.isEmailVerified = isEmailVerified;
        this.notificationsLanguage = notificationsLanguage;
    }

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getRoles() {
        return roles;
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public String getNotificationsLanguage() {
        return notificationsLanguage;
    }

    // -------------------- SETTERS --------------------

    public void setId(String id) {
        this.id = id;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public void setNotificationsLanguage(String notificationsLanguage) {
        this.notificationsLanguage = notificationsLanguage;
    }

    // -------------------- HashCode & Equals --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardUserInfo that = (DashboardUserInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
