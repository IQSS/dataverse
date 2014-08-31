package edu.harvard.iq.dataverse.authorization;

/**
 * Display information for an {@link AuthenticationProvider}.
 * 
 * @author michael
 */
public class AuthenticationProviderDisplayInfo {
   
    private String title;
    private String subtitle;

    public AuthenticationProviderDisplayInfo() {
    }

    public AuthenticationProviderDisplayInfo(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
    
}
