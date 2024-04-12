package edu.harvard.iq.dataverse.globus;

public class GlobusEndpoint {

    private String id;
    private String clientToken;
    private String basePath;

    public GlobusEndpoint(String id, String clientToken, String basePath) {
        this.id = id;
        this.clientToken = clientToken;
        this.basePath = basePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}