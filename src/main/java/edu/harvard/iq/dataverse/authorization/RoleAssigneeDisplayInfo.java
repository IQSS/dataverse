package edu.harvard.iq.dataverse.authorization;

import java.util.Objects;

/**
 * Contains display info for an assignee.
 * @author michael
 */

public class RoleAssigneeDisplayInfo implements java.io.Serializable {
    
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public String toString() {
        return "RoleAssigneeDisplayInfo{" + "title=" + title + ", emailAddress=" + emailAddress + ", affiliation=" + affiliation + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.title);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof RoleAssigneeDisplayInfo)) {
            return false;
        }
        final RoleAssigneeDisplayInfo other = (RoleAssigneeDisplayInfo) obj;
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.emailAddress, other.emailAddress)) {
            return false;
        }
        return Objects.equals(this.affiliation, other.affiliation);
    }
    
}
