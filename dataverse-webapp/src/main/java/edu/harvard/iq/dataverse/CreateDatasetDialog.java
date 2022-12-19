package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataverselookup.LookupData;
import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.NodesInfo;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Currently this bean is only scaffolding for subsequent tasks.
 */
@ViewScoped
@Named("CreateDatasetDialog")
public class CreateDatasetDialog implements Serializable {
    private SolrTreeService solrTreeService;
    private DataverseRequestServiceBean dataverseRequestService;
    private DataverseLookupService dataverseLookupService;

    private Mode selectedMode = Mode.LOOKUP;
    private boolean initialized = false;

    private NodesInfo nodesInfo = new NodesInfo(Collections.emptyMap(), Collections.emptySet(), null);
    private TreeNode rootNode = new DefaultTreeNode(new NodeData(null, "TreeRoot", true, false));
    private TreeNode selectedNode;

    private DataverseLookupService.LookupPermissions lookupPermissions;
    private LookupData lookupSelection;

    // -------------------- CONSTRUCTORS --------------------

    public CreateDatasetDialog() { }

    @Inject
    public CreateDatasetDialog(SolrTreeService solrTreeService, DataverseRequestServiceBean dataverseRequestService,
                               DataverseLookupService dataverseLookupService) {
        this.solrTreeService = solrTreeService;
        this.dataverseRequestService = dataverseRequestService;
        this.dataverseLookupService = dataverseLookupService;
    }

    // -------------------- GETTERS --------------------

    public TreeNode getRootNode() {
        return rootNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public Mode getSelectedMode() {
        return selectedMode;
    }

    public LookupData getLookupSelection() {
        return lookupSelection;
    }

    // -------------------- LOGIC --------------------

    public void init() {
        if (initialized) {
            return;
        }
        lookupPermissions = dataverseLookupService.createLookupPermissions(dataverseRequestService.getDataverseRequest());
        nodesInfo = solrTreeService.fetchNodesInfo(dataverseRequestService.getDataverseRequest());
        TreeNode firstNode = new DefaultTreeNode(new NodeData(nodesInfo.getRootNodeId(), "Root", true,
                nodesInfo.isSelectable(nodesInfo.getRootNodeId())), rootNode);
        fetchChildNodes(firstNode);
        firstNode.setExpanded(true);
        initialized = true;
    }

    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectedNode.getChildren().clear(); // clear placeholders
        fetchChildNodes(selectedNode);
    }

    public List<LookupData> fetchLookupData(String query) {
        return dataverseLookupService.fetchLookupData(query, lookupPermissions);
    }

    public String stripHtml(String text) {
        return MarkupChecker.stripAllTags(text);
    }

    public String createDataset() {
        return "/createDataset.xhtml?ownerId=" + (selectedMode == Mode.LOOKUP
                ? lookupSelection.getId()
                : ((NodeData) selectedNode.getData()).getId())
                + "&faces-redirect=true";
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

    public void setSelectedMode(Mode selectedMode) {
        this.selectedMode = selectedMode;
    }

    public void setLookupSelection(LookupData lookupSelection) {
        this.lookupSelection = lookupSelection;
    }

    // -------------------- INNER CLASSES --------------------

    public enum Mode {
        TREE,
        LOOKUP
    }
}