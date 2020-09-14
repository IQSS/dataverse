package edu.harvard.iq.dataverse.globus;

public class Identity {
    private String id;
    private String username;
    private String status;
    private String name;
    private String email;
    private String identityProvider;
    private String organization;

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOrganization() {
        return organization;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getUsername() {
        return username;
    }
}
