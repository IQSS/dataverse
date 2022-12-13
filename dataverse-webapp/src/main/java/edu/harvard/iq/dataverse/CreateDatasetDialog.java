package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.NodesInfo;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@ViewScoped
@Named("CreateDatasetDialog")
public class CreateDatasetDialog implements Serializable {
    private SolrTreeService solrTreeService;
    private DataverseRequestServiceBean dataverseRequestService;

    private NodesInfo nodesInfo = new NodesInfo(Collections.emptyMap(), Collections.emptySet(), null);
    private TreeNode rootNode = new DefaultTreeNode(new NodeData(null, "TreeRoot", true, false));
    private TreeNode selectedNode;

    // -------------------- CONSTRUCTORS --------------------

    public CreateDatasetDialog() { }

    @Inject
    public CreateDatasetDialog(SolrTreeService solrTreeService, DataverseRequestServiceBean dataverseRequestService) {
        this.solrTreeService = solrTreeService;
        this.dataverseRequestService = dataverseRequestService;
    }

    // -------------------- GETTERS --------------------

    public TreeNode getRootNode() {
        return rootNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void init() {
        nodesInfo = solrTreeService.fetchNodesInfo(dataverseRequestService.getDataverseRequest());
        TreeNode firstNode = new DefaultTreeNode(new NodeData(nodesInfo.getRootNodeId(), "Root", true,
                nodesInfo.isSelectable(nodesInfo.getRootNodeId())), rootNode);
        fetchChildNodes(firstNode);
        firstNode.setExpanded(true);
    }

    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectedNode.getChildren().clear(); // clear placeholders
        fetchChildNodes(selectedNode);
    }

    // -------------------- PRIVATE --------------------

    private void fetchChildNodes(TreeNode parentNode) {
        NodeData data = (NodeData) parentNode.getData();
        List<NodeData> childNodes = solrTreeService.fetchNodes(data.getId(), nodesInfo);
        for (NodeData child : childNodes) {
            DefaultTreeNode treeNode = new DefaultTreeNode(child);
            treeNode.setSelectable(child.isSelectable());
            if (child.isExpandable()) {
                treeNode.getChildren().add(new DefaultTreeNode(NodeData.PLACEHOLDER));
            }
            parentNode.getChildren().add(treeNode);
        }
    }

    // -------------------- SETTERS --------------------

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }
}
