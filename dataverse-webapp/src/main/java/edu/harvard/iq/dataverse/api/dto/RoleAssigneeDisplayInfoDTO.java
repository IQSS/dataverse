package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoleAssigneeDisplayInfoDTO {

    private String title;
    private String email;
    private String affiliation;

    // -------------------- GETTERS --------------------

    public String getAffiliation() {
        return affiliation;
    }

    public String getTitle() {
        return title;
    }

    public String getEmail() {
        return email;
    }

    // -------------------- SETTERS --------------------

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {

        public RoleAssigneeDisplayInfoDTO convert(RoleAssigneeDisplayInfo info) {
            RoleAssigneeDisplayInfoDTO converted = new RoleAssigneeDisplayInfoDTO();
            converted.setTitle(info.getTitle());
            converted.setEmail(info.getEmailAddress());
            converted.setAffiliation(info.getAffiliation());
            return converted;
        }
    }
}
