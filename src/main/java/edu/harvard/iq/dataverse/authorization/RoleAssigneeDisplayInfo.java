package edu.harvard.iq.dataverse.authorization;

/**
 * Contains display info for an assignee.
 * @author michael
 */
public class RoleAssigneeDisplayInfo {
    
    private final String title;
    private final String emailAddress;
    private final String affiliation;

    public RoleAssigneeDisplayInfo(String title, String emailAddress) {
        this( title, emailAddress, null );
    }
    
    public RoleAssigneeDisplayInfo(String title, String emailAddress, String anAffiliation) {
        this.title = title;
        this.emailAddress = emailAddress;
        affiliation = anAffiliation;
    }

    public String getTitle() {
        return title;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getAffiliation() {
        return affiliation;
    }
    
    
}
