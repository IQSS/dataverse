package edu.harvard.iq.dataverse.search.dataversestree;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Allows to navigate and perform operations on a {@link TreeNode} based tree.
 */
public class TreeNodeBrowser {
    private final Function<Long, Optional<Long>> loadParentId;
    private final Function<Long, List<NodeData>> fetchChildren;
    private TreeNode rootNode = new DefaultTreeNode(new NodeData(null, "TreeRoot", true, false));

    // -------------------- CONSTRUCTORS --------------------

    public TreeNodeBrowser(Dataverse rootDataverse, NodesInfo nodesInfo, Function<Long, Optional<Long>> loadParentId, Function<Long, List<NodeData>> fetchChildren) {
        this.loadParentId = loadParentId;
        this.fetchChildren = fetchChildren;

        Long rootId = rootDataverse.getId();
        TreeNode firstNode = new DefaultTreeNode(new NodeData(rootId, rootDataverse.getDisplayName(), true,
                nodesInfo.isSelectable(rootId)), rootNode);
        firstNode.setSelectable(nodesInfo.isSelectable(rootId));
        resetRoot();
    }

    // -------------------- GETTERS --------------------

    public TreeNode getRootNode() {
        return rootNode;
    }

    // -------------------- LOGIC --------------------

    public void resetRoot() {
        rootNode.getChildren().forEach(node -> {
            fetchChildNodes(node);
            node.setExpanded(true);
            node.setSelected(false);
        });
    }

    /**
     * Expands a path in the tree to the node with the provided id and returns it, if found.
     * The tree is rebuild to only contain the minimal amount of expanded nodes up to nodeId.
     */
    public Optional<TreeNode> expandTreeTo(Long nodeId) {
        List<TreeNode> siblings = expandNodes(rootNode.getChildren(), getParents(nodeId), true);

        return siblings.stream()
                .filter(n -> nodeId.equals(((NodeData) n.getData()).getId()))
                .findFirst();
    }

    /**
     * Search for all the provided (nodeId, parentId) pairs and expand all the necessary nodes in the path
     * from the root to them.
     * Returns the (nodeId, parentId) pairs in the path from the root node to the nodeIds.
     */
    public Map<Long, Long> expandTreeTo(Map<Long, Long> nodeIds) {
        Map<Long, Long> nodeIdsCache = new HashMap<>(nodeIds);
        for (Long nodeId : nodeIds.keySet()) {
            expandNodes(rootNode.getChildren(), getParents(nodeId, nodeIdsCache), false);
        }
        return nodeIdsCache;
    }

    /**
     * Remove any nodes from the tree with ids not present in the idsToKeep set.
     */
    public void trimTree(Set<Long> idsToKeep) {
        trimTree(rootNode.getChildren(), idsToKeep);
    }

    public void fetchChildNodes(TreeNode parentNode) {
        parentNode.getChildren().clear();
        NodeData data = (NodeData) parentNode.getData();
        List<NodeData> childNodes = fetchChildren.apply(data.getId());
        for (NodeData child : childNodes) {
            DefaultTreeNode treeNode = new DefaultTreeNode(child);
            treeNode.setSelectable(child.isSelectable());
            if (child.isExpandable()) {
                treeNode.getChildren().add(new DefaultTreeNode(NodeData.PLACEHOLDER));
            }
            parentNode.getChildren().add(treeNode);
        }
    }

    // -------------------- PRIVATE --------------------

    private void trimTree(List<TreeNode> nodes, Set<Long> idsToKeep) {
        List<TreeNode> nodesToRemove = nodes.stream()
                .filter(n -> !idsToKeep.contains(((NodeData) n.getData()).getId()))
                .collect(Collectors.toList());

        if (nodesToRemove.size() < nodes.size()) {
            nodesToRemove.forEach(nodes::remove);
        }

        nodes.forEach(n -> trimTree(n.getChildren(), idsToKeep));
    }

    private List<TreeNode> expandNodes(List<TreeNode> nodes, List<Long> expandNodeIds, boolean rebuild) {
        if (expandNodeIds.isEmpty()) {
            return nodes;
        }

        Long nodeId = expandNodeIds.remove(0);
        List<TreeNode> nextNodes = nodes.stream()
                .filter(node -> {
                    NodeData nodeData = (NodeData) node.getData();
                    return nodeId.equals(nodeData.getId()) && nodeData.isExpandable();
                }).findFirst().map(node -> {
                    if (!node.isExpanded() || rebuild) {
                        fetchChildNodes(node);
                        node.setExpanded(true);
                    }
                    return node.getChildren();
                }).orElse(Collections.emptyList());

        return expandNodes(nextNodes, expandNodeIds, rebuild);
    }

    private List<Long> getParents(Long dataverseId) {
        List<Long> parents = new ArrayList<>();
        Optional<Long> parentId = loadParentId.apply(dataverseId);
        while (parentId.isPresent()) {
            parents.add(0, parentId.get());
            parentId = loadParentId.apply(parentId.get());
        }
        return parents;
    }

    private List<Long> getParents(Long dataverseId, Map<Long, Long> parentIdsCache) {
        List<Long> parents = new ArrayList<>();
        Long currentId = dataverseId;
        while (currentId >= 0) {
            if (parentIdsCache.containsKey(currentId)) {
                Long parentId = parentIdsCache.get(currentId);
                if (parentId != -1) {
                    parents.add(0, parentId);
                }
                currentId = parentId;
            } else {
                Optional<Long> parentId = loadParentId.apply(currentId);
                if (parentId.isPresent()) {
                    parentIdsCache.put(currentId, parentId.get());
                    parents.add(0, parentId.get());
                    currentId = parentId.get();
                } else {
                    parentIdsCache.put(currentId, -1L);
                    currentId = -1L;
                }
            }
        }
        return parents;
    }
}
