package edu.harvard.iq.dataverse.search.dataversestree;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.search.MockSolrResponseUtil.createSolrResponse;
import static edu.harvard.iq.dataverse.search.MockSolrResponseUtil.document;
import static edu.harvard.iq.dataverse.search.MockSolrResponseUtil.field;
import static edu.harvard.iq.dataverse.search.MockSolrResponseUtil.list;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolrTreeServiceTest {

    @InjectMocks
    private SolrTreeService service;

    @Mock
    private SolrClient solrClient;
    @Mock
    private PermissionFilterQueryBuilder permissionFilterQueryBuilder;
    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private PermissionServiceBean.RequestPermissionQuery permissionQuery;

    // -------------------- TESTS --------------------

    @Test
    void fetchNodesInfo() throws Exception {
        // given
        when(solrClient.query(Mockito.any())).thenReturn(createSolrResponse(
                document(field("entityId", 321L),
                        field("parentId", "22"),
                        field("subtreePaths", list("/1", "/1/323", "/1/323/22"))),
                document(field("entityId", 322L),
                        field("parentId", "323"),
                        field("subtreePaths", list("/1", "/1/323"))),
                document(field("entityId", 323L),
                        field("parentId", "1"),
                        field("subtreePaths", list("/1")))));
        DataverseRequest request = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"),
                (HttpServletRequest) null);

        // when
        NodesInfo nodesInfo = service.fetchNodesInfo(request);

        // then
        assertThat(nodesInfo.getPermissions().entrySet())
                .extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(
                        tuple(323L, NodePermission.SELECT),
                        tuple(322L, NodePermission.SELECT),
                        tuple(321L, NodePermission.SELECT),
                        tuple(1L, NodePermission.VIEW),
                        tuple(22L, NodePermission.VIEW));
        assertThat(nodesInfo.getExpandableNodes())
                .containsExactlyInAnyOrder(1L, 22L, 323L);
    }

    @Test
    @DisplayName("Only path containing parentId of the dataverse is taken into account when creating intermediate expandable nodes")
    void fetchNodesInfo__linkedDataverse() throws Exception {
        // given
        when(solrClient.query(Mockito.any())).thenReturn(createSolrResponse(
                document(field("entityId", 321L),
                        field("parentId", "121"),
                        field("subtreePaths", list(
                                "/1", "/1/323", "/1/323/121",
                                "/1", "/1/841", "/1/841/9203", "/1/841/9203/67",
                                "/1", "/1/543", "/1/543/12")))));
        DataverseRequest request = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"),
                (HttpServletRequest) null);

        // when
        NodesInfo nodesInfo = service.fetchNodesInfo(request);

        // then
        assertThat(nodesInfo.getPermissions().entrySet())
                .extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(
                        tuple(321L, NodePermission.SELECT),
                        tuple(323L, NodePermission.VIEW),
                        tuple(121L, NodePermission.VIEW),
                        tuple(1L, NodePermission.VIEW));
        assertThat(nodesInfo.getExpandableNodes())
                .containsExactlyInAnyOrder(1L, 323L, 121L);
    }

    @Test
    void fetchNodes() throws Exception {
        // given
        NodesInfo nodesInfo = new NodesInfo(
                permissions(viewable(1L), selectable(11L), selectable(12L), selectable(13L), viewable(21L)),
                expandableNodes(1L, 11L, 21L));
        when(solrClient.query(Mockito.any())).thenReturn(createSolrResponse(
                document(field("entityId", 1L), field("name", "Root")),
                document(field("entityId", 11L), field("name", "Collection 1")),
                document(field("entityId", 12L), field("name", "Collection 2")),
                document(field("entityId", 13L), field("name", "Collection 3")),
                document(field("entityId", 14L), field("name", "Collection 4"))));

        // when
        List<NodeData> nodeData = service.fetchNodes(1L, nodesInfo);

        // then
        assertThat(nodeData)
                .extracting(NodeData::getId, NodeData::getName, NodeData::isSelectable, NodeData::isExpandable)
                .containsExactlyInAnyOrder(
                        tuple(1L, "Root", false, true),
                        tuple(11L, "Collection 1", true, true),
                        tuple(12L, "Collection 2", true, false),
                        tuple(13L, "Collection 3", true, false));
    }

    // -------------------- PRIVATE --------------------

    private static Tuple2<Long, NodePermission> viewable(Long id) {
        return Tuple.of(id, NodePermission.VIEW);
    }

    private static Tuple2<Long, NodePermission> selectable(Long id) {
        return Tuple.of(id, NodePermission.SELECT);
    }

    @SafeVarargs
    private static Map<Long, NodePermission> permissions(Tuple2<Long, NodePermission>... permissions) {
        return Arrays.stream(permissions)
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
    }

    private static Set<Long> expandableNodes(Long... ids) {
        return Arrays.stream(ids).collect(Collectors.toSet());
    }
}