package edu.harvard.iq.dataverse.authorization;

/**
 * Display information for an {@link AuthenticationProvider}.
 * 
 * @author michael
 */
public class AuthenticationProviderDisplayInfo {
   
    private String title;
    private String subtitle;
    private String id;
    
    public AuthenticationProviderDisplayInfo() {
    }

    public AuthenticationProviderDisplayInfo(String id, String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public AuthenticationProviderDisplayInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public AuthenticationProviderDisplayInfo setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getId() {
        return id;
    }

    public AuthenticationProviderDisplayInfo setId(String id) {
        this.id = id;
        return this;
    }
    
}
