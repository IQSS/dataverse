package edu.harvard.iq.dataverse.search.dataversestree;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NodesInfo {
    private final Map<Long, NodePermission> permissions;
    private final Set<Long> expandableNodes;
    private final Long rootNodeId;

    // -------------------- CONSTRUCTORS --------------------

    public NodesInfo(Map<Long, NodePermission> permissions, Set<Long> expandableNodes, Long rootNodeId) {
        this.permissions = Objects.requireNonNull(permissions);
        this.expandableNodes = Objects.requireNonNull(expandableNodes);
        this.rootNodeId = rootNodeId;
    }

    // -------------------- GETTERS --------------------

    public Map<Long, NodePermission> getPermissions() {
        return permissions;
    }

    public Set<Long> getExpandableNodes() {
        return expandableNodes;
    }

    public Long getRootNodeId() {
        return rootNodeId;
    }

    // -------------------- LOGIC --------------------

    public boolean isViewable(Long id) {
        return permissions.containsKey(id);
    }

    public boolean isSelectable(Long id) {
        return permissions.get(id) == NodePermission.SELECT;
    }

    public boolean isExpandable(Long id) {
        return expandableNodes.contains(id);
    }
}
