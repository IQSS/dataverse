package edu.harvard.iq.dataverse.search.dataversestree;

public class NodeData {
    private final Long id;
    private final String name;
    private final boolean expandable;
    private final boolean selectable;

    public static NodeData PLACEHOLDER = new NodeData(null, "PLACEHOLDER", false, false);

    // -------------------- CONSTRUCTORS --------------------

    public NodeData(Long id, String name, boolean expandable, boolean selectable) {
        this.id = id;
        this.name = name;
        this.expandable = expandable;
        this.selectable = selectable;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public boolean isSelectable() {
        return selectable;
    }
}
