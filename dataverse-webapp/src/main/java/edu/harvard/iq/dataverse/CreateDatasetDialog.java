package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataverselookup.LookupData;
import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.NodesInfo;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import edu.harvard.iq.dataverse.search.dataversestree.TreeNodeBrowser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.TreeNode;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ViewScoped
@Named("CreateDatasetDialog")
public class CreateDatasetDialog implements Serializable {
    private SolrTreeService solrTreeService;
    private DataverseRequestServiceBean dataverseRequestService;
    private DataverseLookupService dataverseLookupService;
    private DataverseDao dataverseDao;

    private boolean initialized = false;

    private NodesInfo nodesInfo = new NodesInfo(Collections.emptyMap(), Collections.emptySet());
    private TreeNode selectedNode;

    private String permissionFilterQuery;
    private String treeFilter;
    private String prevTreeFilter;
    private TreeNodeBrowser treeNodeBrowser;
    private DataverseSession session;
    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    public CreateDatasetDialog() { }

    @Inject
    public CreateDatasetDialog(SolrTreeService solrTreeService, DataverseRequestServiceBean dataverseRequestService,
                               DataverseLookupService dataverseLookupService, DataverseDao dataverseDao,
                               DataverseSession session, SystemConfig systemConfig) {
        this.solrTreeService = solrTreeService;
        this.dataverseRequestService = dataverseRequestService;
        this.dataverseLookupService = dataverseLookupService;
        this.dataverseDao = dataverseDao;
        this.session = session;
        this.systemConfig = systemConfig;
    }

    // -------------------- GETTERS --------------------

    public TreeNode getRootNode() {
        return treeNodeBrowser != null ? treeNodeBrowser.getRootNode() : null;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public String getTreeFilter() {
        return treeFilter;
    }

    // -------------------- LOGIC --------------------

    public void init() {
        if (initialized) {
            return;
        }
        permissionFilterQuery = dataverseLookupService.buildFilterQuery(dataverseRequestService.getDataverseRequest());

        Dataverse rootDataverse = dataverseDao.findRootDataverse();
        nodesInfo = solrTreeService.fetchNodesInfo(dataverseRequestService.getDataverseRequest());
        treeNodeBrowser = new TreeNodeBrowser(rootDataverse, nodesInfo, this::loadParentDataverseId, this::fetchChildren);
        initialized = true;
    }

    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        treeNodeBrowser.fetchChildNodes(selectedNode);
    }

    public void executeTreeFilter() {
        if (prevTreeFilter != null && prevTreeFilter.equals(treeFilter)) {
            return;
        }

        treeNodeBrowser.resetRoot();

        if (treeFilter == null || treeFilter.length() < 3) {
            return;
        }

        List<LookupData> results = dataverseLookupService.fetchLookupDataByNameAndExtraDescription(treeFilter, permissionFilterQuery);
        if (results.isEmpty()) {
            return;
        }

        Map<Long, Long> parentIdsCache = treeNodeBrowser.expandTreeTo(results.stream()
                .collect(Collectors.toMap(LookupData::getId, LookupData::getParentId)));
        treeNodeBrowser.trimTree(parentIdsCache.keySet());

        prevTreeFilter = treeFilter;
    }

    public String createDataset() {
        return "/createDataset.xhtml?ownerId=" + ((NodeData) selectedNode.getData()).getId()
                + "&faces-redirect=true";
    }

    public String getSelectDataverseInfo() {
        return systemConfig.getSelectDataverseInfo(session.getLocale());
    }

    // -------------------- PRIVATE --------------------

    private Optional<Long> loadParentDataverseId(Long id) {
        return Optional.ofNullable(dataverseDao.find(id))
                .map(Dataverse::getOwner)
                .map(Dataverse::getId);
    }

    private List<NodeData> fetchChildren(Long id) {
        return solrTreeService.fetchNodes(id, nodesInfo);
    }

    // -------------------- SETTERS --------------------

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public void setTreeFilter(String treeFilter) {
        this.treeFilter = treeFilter;
    }
}