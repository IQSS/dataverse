package edu.harvard.iq.dataverse.persistence.user;

import java.util.Objects;

/**
 * Contains display info for an assignee.
 *
 * @author michael
 */

public class RoleAssigneeDisplayInfo implements java.io.Serializable {

    private String title;
    private String emailAddress;
    private String affiliation;
    private String affiliationROR;

    public RoleAssigneeDisplayInfo(String title, String emailAddress) {
        this(title, emailAddress, null, null);
    }

    public RoleAssigneeDisplayInfo(String title, String emailAddress, String anAffiliation, String affiliationROR) {
        this.title = title;
        this.emailAddress = emailAddress;
        this.affiliation = anAffiliation;
        this.affiliationROR = affiliationROR;
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

    public String getAffiliationROR() {
        return affiliationROR;
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

    public void setAffiliationROR(String affiliationROR) {
        this.affiliationROR = affiliationROR;
    }

    @Override
    public String toString() {
        return "RoleAssigneeDisplayInfo{" + "title=" + title + ", emailAddress=" + emailAddress +
                ", affiliation=" + affiliation + ", affiliationROR=" + affiliationROR + '}';
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
        if (!(obj instanceof RoleAssigneeDisplayInfo)) {
            return false;
        }
        final RoleAssigneeDisplayInfo other = (RoleAssigneeDisplayInfo) obj;
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.emailAddress, other.emailAddress)) {
            return false;
        }
        return Objects.equals(this.affiliation, other.affiliation)
                && Objects.equals(this.affiliationROR, other.affiliationROR);
    }

}
