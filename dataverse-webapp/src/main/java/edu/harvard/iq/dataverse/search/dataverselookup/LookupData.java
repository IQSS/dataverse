package edu.harvard.iq.dataverse.search.dataverselookup;

public class LookupData {
    private final Long id;
    private final String identifier;
    private final String name;
    private final Long parentId;
    private final String parentName;
    private final String upperParentName;

    public LookupData(Long id, String identifier, String name, Long parentId, String parentName, String upperParentName) {
        this.id = id;
        this.identifier = identifier;
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.upperParentName = upperParentName;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getUpperParentName() {
        return upperParentName;
    }
}
