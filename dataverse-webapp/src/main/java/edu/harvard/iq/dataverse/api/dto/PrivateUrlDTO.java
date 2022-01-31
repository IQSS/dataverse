package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.privateurl.PrivateUrl;

public class PrivateUrlDTO {
    private String token;
    private String link;
    private RoleAssignmentDTO roleAssignment;

    // -------------------- GETTERS --------------------

    public String getToken() {
        return token;
    }

    public String getLink() {
        return link;
    }

    public RoleAssignmentDTO getRoleAssignment() {
        return roleAssignment;
    }

    // -------------------- SETTERS --------------------

    public void setToken(String token) {
        this.token = token;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setRoleAssignment(RoleAssignmentDTO roleAssignment) {
        this.roleAssignment = roleAssignment;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public PrivateUrlDTO convert(PrivateUrl url) {
            PrivateUrlDTO converted = new PrivateUrlDTO();
            converted.setToken(url.getToken());
            converted.setLink(url.getLink());
            converted.setRoleAssignment(new RoleAssignmentDTO.Converter().convert(url.getRoleAssignment()));
            return converted;
        }
    }
}
