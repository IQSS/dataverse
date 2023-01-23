package edu.harvard.iq.dataverse.search.dataversestree;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
public class SolrTreeService {
    private static final Logger logger = LoggerFactory.getLogger(SolrTreeService.class);

    private SolrClient solrClient;
    private PermissionFilterQueryBuilder permissionFilterQueryBuilder;
    private DataverseDao dataverseDao;
    private PermissionServiceBean permissionService;

    // -------------------- CONSTRUCTORS --------------------

    public SolrTreeService() { }

    @Inject
    public SolrTreeService(SolrClient solrClient, PermissionFilterQueryBuilder permissionFilterQueryBuilder,
                           DataverseDao dataverseDao, PermissionServiceBean permissionService) {
        this.solrClient = solrClient;
        this.permissionFilterQueryBuilder = permissionFilterQueryBuilder;
        this.dataverseDao = dataverseDao;
        this.permissionService = permissionService;
    }

    // -------------------- LOGIC --------------------

    public NodesInfo fetchNodesInfo(DataverseRequest dataverseRequest) {
        QueryResponse queryResponse;
        try {
            queryResponse = executeSolrQueryForNodeInfo(dataverseRequest);
        } catch (IOException | SolrServerException ex) {
            logger.warn("Error during permissions fetching: ", ex);
            return new NodesInfo(Collections.emptyMap(), Collections.emptySet(), null);
        }
        return createNodesInfo(dataverseRequest, queryResponse);
    }

    public List<NodeData> fetchNodes(Long nodeId, NodesInfo nodesInfo) {
        if (nodeId == null || nodesInfo == null || nodesInfo.getPermissions().isEmpty()) {
            return Collections.emptyList();
        }
        QueryResponse queryResponse;
        try {
            queryResponse = executeSolrQueryForNodes(nodeId);
        } catch (SolrServerException | IOException ex) {
            logger.warn("Error during node fetching: ", ex);
            return Collections.emptyList();
        }
        return createNodeData(nodesInfo, queryResponse);
    }

    // -------------------- PRIVATE --------------------

    private QueryResponse executeSolrQueryForNodeInfo(DataverseRequest dataverseRequest) throws IOException, SolrServerException {
        Integer dataversesCount = dataverseDao.countDataverses().intValue() - 1; // root dataverse is not indexed by solr
        String permissionQuery = permissionFilterQueryBuilder.buildPermissionFilterQueryForAddDataset(dataverseRequest);
        SolrQuery query = new SolrQuery()
                .setRows(dataversesCount)
                .setQuery(String.format("%s:%s", SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue()))
                .setFields(SearchFields.ENTITY_ID, SearchFields.PARENT_ID, SearchFields.SUBTREE)
                .setFilterQueries(permissionQuery);
        return solrClient.query(query);
    }

    private NodesInfo createNodesInfo(DataverseRequest dataverseRequest, QueryResponse queryResponse) {
        Set<Long> allowedToSelect = new HashSet<>();
        Set<Long> allowedToView = new HashSet<>();
        for (SolrDocument solrDocument : queryResponse.getResults()) {
            allowedToSelect.add((Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID));
            String parentId = (String) solrDocument.getFieldValue(SearchFields.PARENT_ID);
            List<String> paths = (List<String>) solrDocument.getFieldValue(SearchFields.SUBTREE);
            Set<Long> intermediatePaths = paths.stream()
                    .filter(p -> p.endsWith("/" + parentId))
                    .flatMap(p -> Arrays.stream(p.split("/"))
                            .filter(StringUtils::isNotBlank)
                            .map(Long::valueOf))
                    .collect(Collectors.toSet());
            allowedToView.addAll(intermediatePaths);
        }
        Set<Long> expandableNodes = new HashSet<>(allowedToView); // if node is expandable it's listed in subtreePath field
        allowedToView.removeAll(allowedToSelect); // SELECT node is more than only VIEW, so we remove selectable nodes
        Map<Long, NodePermission> result = new HashMap<>();
        for (Long id : allowedToSelect) {
            result.put(id, NodePermission.SELECT);
        }
        for (Long id : allowedToView) {
            result.put(id, NodePermission.VIEW);
        }
        Dataverse root = dataverseDao.findRootDataverse();
        if (permissionService.requestOn(dataverseRequest, root)
                .has(Permission.AddDataset)) {
            result.put(root.getId(), NodePermission.SELECT);
        }
        return new NodesInfo(result, expandableNodes, root.getId());
    }

    private QueryResponse executeSolrQueryForNodes(Long nodeId) throws IOException, SolrServerException {
        Integer rows = dataverseDao.countDataversesWithParent(nodeId).intValue();
        SolrQuery query = new SolrQuery()
                .setRows(rows)
                .setQuery(String.format("%s:%s AND %s:%d",
                        SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue(),
                        SearchFields.PARENT_ID, nodeId))
                .setFields(SearchFields.ENTITY_ID, SearchFields.NAME);
        return solrClient.query(query);
    }

    private List<NodeData> createNodeData(NodesInfo nodesInfo, QueryResponse queryResponse) {
        List<NodeData> result = new ArrayList<>();
        for (SolrDocument solrDocument : queryResponse.getResults()) {
            Long id = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            if (nodesInfo.isViewable(id)) {
                result.add(new NodeData(id, name, nodesInfo.isExpandable(id), nodesInfo.isSelectable(id)));
            }
        }
        return result;
    }
}