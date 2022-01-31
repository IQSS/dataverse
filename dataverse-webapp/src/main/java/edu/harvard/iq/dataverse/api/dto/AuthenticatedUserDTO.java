package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthenticatedUserDTO {
    private Long id;
    private String identifier;
    private String displayName;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean superuser;
    private String affiliation;
    private String position;
    private String persistentUserId;
    private String emailLastConfirmed;
    private String createdTime;
    private String lastLoginTime;
    private String lastApiUseTime;
    private String authenticationProviderId;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getSuperuser() {
        return superuser;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getPosition() {
        return position;
    }

    public String getPersistentUserId() {
        return persistentUserId;
    }

    public String getEmailLastConfirmed() {
        return emailLastConfirmed;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public String getLastApiUseTime() {
        return lastApiUseTime;
    }

    public String getAuthenticationProviderId() {
        return authenticationProviderId;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSuperuser(Boolean superuser) {
        this.superuser = superuser;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setPersistentUserId(String persistentUserId) {
        this.persistentUserId = persistentUserId;
    }

    public void setEmailLastConfirmed(String emailLastConfirmed) {
        this.emailLastConfirmed = emailLastConfirmed;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public void setLastApiUseTime(String lastApiUseTime) {
        this.lastApiUseTime = lastApiUseTime;
    }

    public void setAuthenticationProviderId(String authenticationProviderId) {
        this.authenticationProviderId = authenticationProviderId;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public AuthenticatedUserDTO convert(AuthenticatedUser user) {

            // -------------------- LOGIC --------------------

            AuthenticatedUserDTO converted = new AuthenticatedUserDTO();
            converted.setId(user.getId());
            converted.setIdentifier(user.getIdentifier());
            converted.setDisplayName(user.getDisplayInfo().getTitle());
            converted.setFirstName(user.getFirstName());
            converted.setLastName(user.getLastName());
            converted.setEmail(user.getEmail());
            converted.setSuperuser(user.isSuperuser());
            converted.setAffiliation(user.getAffiliation());
            converted.setPosition(user.getPosition());
            converted.setEmailLastConfirmed(safeTimestampToString(user.getEmailConfirmed()));
            converted.setCreatedTime(safeTimestampToString(user.getCreatedTime()));
            converted.setLastLoginTime(safeTimestampToString(user.getLastLoginTime()));
            converted.setLastApiUseTime(safeTimestampToString(user.getLastApiUseTime()));
            AuthenticatedUserLookup lookup = user.getAuthenticatedUserLookup();
            if (lookup != null) {
                converted.setPersistentUserId(lookup.getPersistentUserId());
                converted.setAuthenticationProviderId(lookup.getAuthenticationProviderId());
            }
            return converted;
        }

        // -------------------- PRIVATE --------------------

        private String safeTimestampToString(Object object) {
            return object != null ? Util.getDateTimeFormat().format(object) : null;
        }
    }
}
