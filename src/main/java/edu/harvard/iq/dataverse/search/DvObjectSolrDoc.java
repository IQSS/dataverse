package edu.harvard.iq.dataverse.search;

import java.util.List;

public class DvObjectSolrDoc {

    private String nameOrTitle;
    private List<String> permissions;

    public DvObjectSolrDoc(String nameOrTitle, List<String> permissions) {
        this.nameOrTitle = nameOrTitle;
        this.permissions = permissions;
    }

    public String getNameOrTitle() {
        return nameOrTitle;
    }

    public void setNameOrTitle(String nameOrTitle) {
        this.nameOrTitle = nameOrTitle;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

}
