package edu.harvard.iq.dataverse.authorization;

/**
 * Contains display info for an assignee.
 * @author michael
 */

public class RoleAssigneeDisplayInfo {
    
    private String title;
    private String emailAddress;
    private String affiliation;

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

    public RoleAssigneeDisplayInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    public RoleAssigneeDisplayInfo setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public RoleAssigneeDisplayInfo setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    @Override
    public String toString() {
        return "RoleAssigneeDisplayInfo{" + "title=" + title + ", emailAddress=" + emailAddress + ", affiliation=" + affiliation + '}';
    }
    
}
