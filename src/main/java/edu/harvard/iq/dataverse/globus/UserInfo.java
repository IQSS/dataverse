package edu.harvard.iq.dataverse.globus;

public class UserInfo implements java.io.Serializable{

    private String identityProviderDisplayName;
    private String identityProvider;
    private String organization;
    private String sub;
    private String preferredUsername;
    private String name;
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public void setIdentityProviderDisplayName(String identityProviderDisplayName) {
        this.identityProviderDisplayName = identityProviderDisplayName;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getEmail() {
        return email;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public String getSub() {
        return sub;
    }

    public String getName() {
        return name;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getIdentityProviderDisplayName() {
        return identityProviderDisplayName;
    }

    public String getOrganization() {
        return organization;
    }
}
