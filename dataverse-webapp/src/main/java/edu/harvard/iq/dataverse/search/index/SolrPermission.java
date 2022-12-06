package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.List;

public class SolrPermission {
    private List<String> permittedEntities;
    private Permission type;

    // -------------------- CONSTRUCTORS --------------------

    public SolrPermission(Permission type, List<String> permittedEntities) {
        this.permittedEntities = permittedEntities;
        this.type = type;
    }

    // -------------------- GETTERS --------------------

    public List<String> getPermittedEntities() {
        return permittedEntities;
    }

    public Permission getType() {
        return type;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "SolrPermission{type=" + type + ", permittedEntities=" + permittedEntities + '}';
    }
}