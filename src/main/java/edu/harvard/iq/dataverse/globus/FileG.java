package edu.harvard.iq.dataverse.globus;

public class FileG {
    private String DATA_TYPE;
    private String group;
    private String name;
    private String permissions;
    private String size;
    private String type;
    private String user;

    public String getDATA_TYPE() {
        return DATA_TYPE;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public void setDATA_TYPE(String DATA_TYPE) {
        this.DATA_TYPE = DATA_TYPE;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
