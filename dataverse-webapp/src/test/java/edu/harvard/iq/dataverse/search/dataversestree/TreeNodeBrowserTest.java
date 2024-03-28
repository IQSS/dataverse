package edu.harvard.iq.dataverse.search.dataversestree;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.primefaces.model.TreeNode;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeNodeBrowserTest {

    private final Map<Long, Pair<NodeData, Long>> testData = ImmutableMap.<Long, Pair<NodeData, Long>>builder()
            .put(1L, Pair.of(new NodeData(1L, "Root", true, true), -1L))
            .put(2L, Pair.of(new NodeData(2L, "0.1", true, true), 1L))
            .put(20L, Pair.of(new NodeData(20L, "0.1.1", true, true), 2L))
            .put(21L, Pair.of(new NodeData(21L, "0.1.2", true, true), 2L))
            .put(210L, Pair.of(new NodeData(210L, "0.1.2.1", true, true), 21L))
            .put(211L, Pair.of(new NodeData(211L, "0.1.2.2", true, true), 21L))
            .put(212L, Pair.of(new NodeData(212L, "0.1.2.3", true, true), 21L))
            .put(22L, Pair.of(new NodeData(22L, "0.1.3", true, true), 2L))
            .put(3L, Pair.of(new NodeData(3L, "0.2", true, true), 1L))
            .put(30L, Pair.of(new NodeData(30L, "0.2.1", true, true), 3L))
            .put(31L, Pair.of(new NodeData(31L, "0.2.2", true, true), 3L))
            .put(310L, Pair.of(new NodeData(310L, "0.2.2.1", true, true), 31L))
            .put(311L, Pair.of(new NodeData(311L, "0.2.2.2", true, true), 31L))
            .put(312L, Pair.of(new NodeData(312L, "0.2.2.3", true, true), 31L))
            .put(3120L, Pair.of(new NodeData(3120L, "0.2.2.3.1", true, true), 312L))
            .put(313L, Pair.of(new NodeData(313L, "0.2.2.4", true, true), 31L))
            .put(4L, Pair.of(new NodeData(4L, "0.3", true, true), 1L))
            .build();

    // -------------------- TESTS --------------------

    @Test
    void expandTreeTo() {
        // given
        Long nodeId = 312L;

        // when
        TreeNodeBrowser treeNodeBrowser = newTreeBrowser();
        Optional<TreeNode> node = treeNodeBrowser.expandTreeTo(nodeId);

        // then
        assertThat(node)
                .isPresent()
                .map(n -> (NodeData) n.getData())
                .map(NodeData::getId)
                .hasValue(nodeId);

        assertThat(getParentNodeNames(node.get())).containsExactly("TreeRoot", "Root", "0.2", "0.2.2");
        assertThat(treeNodeBrowser.getRootNode().getChildren()).hasSize(1);

        TreeNode root = getParentWithName(node.get(), "Root");
        assertThat(getChildrenNodeNames(root)).containsExactly("0.1", "0.2", "0.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(root, "0.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(root, "0.3"));

        TreeNode n_0_2 = getChildWithName(root, "0.2");
        assertThat(getChildrenNodeNames(n_0_2)).containsExactly("0.2.1", "0.2.2");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2, "0.2.1"));

        TreeNode n_0_2_2 = getChildWithName(n_0_2, "0.2.2");
        assertThat(getChildrenNodeNames(n_0_2_2)).containsExactly("0.2.2.1", "0.2.2.2", "0.2.2.3", "0.2.2.4");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.2"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.3"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.4"));
    }

    @Test
    void expandTreeTo__node_under_root() {
        // given
        Long nodeId = 4L;

        // when
        TreeNodeBrowser treeNodeBrowser = newTreeBrowser();
        Optional<TreeNode> node = treeNodeBrowser.expandTreeTo(nodeId);

        // then
        assertThat(node)
                .isPresent()
                .map(n -> (NodeData) n.getData())
                .map(NodeData::getId)
                .hasValue(nodeId);

        assertThat(getParentNodeNames(node.get())).containsExactly("TreeRoot", "Root");
        assertThat(treeNodeBrowser.getRootNode().getChildren()).hasSize(1);

        TreeNode root = getParentWithName(node.get(), "Root");
        assertThat(getChildrenNodeNames(root)).containsExactly("0.1", "0.2", "0.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(root, "0.3"));

    }

    @Test
    void expandTreeTo__root() {
        // given
        Long nodeId = 1L;

        // when
        TreeNodeBrowser treeNodeBrowser = newTreeBrowser();
        Optional<TreeNode> node = treeNodeBrowser.expandTreeTo(nodeId);

        // then
        assertThat(node)
                .isPresent()
                .map(n -> (NodeData) n.getData())
                .map(NodeData::getId)
                .hasValue(nodeId);

        assertThat(getParentNodeNames(node.get())).containsExactly("TreeRoot");
        assertThat(treeNodeBrowser.getRootNode().getChildren()).hasSize(1);

        assertThat(getChildrenNodeNames(treeNodeBrowser.getRootNode())).containsExactly("Root");
        assertThat(getChildrenNodeNames(node.get())).containsExactly("0.1", "0.2", "0.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(node.get(), "0.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(node.get(), "0.2"));
        assertContainsOnlyPlaceholderChild(getChildWithName(node.get(), "0.3"));
    }

    @Test
    void expandTreeTo__multiple_nodes() throws Exception {
        // given
        Map<Long, Long> nodes = ImmutableMap.<Long, Long>builder()
                .put(211L, 21L)
                .put(310L, 31L)
                .build();

        // when
        TreeNodeBrowser treeNodeBrowser = newTreeBrowser();
        Map<Long, Long> paths = treeNodeBrowser.expandTreeTo(nodes);

        // then
        assertThat(paths.entrySet()).isEqualTo(ImmutableMap.builder()
                .put(1L, -1L)
                .put(3L, 1L)
                .put(31L, 3L)
                .put(310L, 31L)
                .put(2L, 1L)
                .put(21L, 2L)
                .put(211L, 21L)
                .build().entrySet());

        assertThat(getChildrenNodeNames(treeNodeBrowser.getRootNode())).containsExactly("Root");

        TreeNode root = getChildWithName(treeNodeBrowser.getRootNode(), "Root");
        assertThat(getChildrenNodeNames(root)).containsExactly("0.1", "0.2", "0.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(root, "0.3"));

        TreeNode n_0_1 = getChildWithName(root, "0.1");
        assertThat(getChildrenNodeNames(n_0_1)).containsExactly("0.1.1", "0.1.2", "0.1.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1, "0.1.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1, "0.1.3"));

        TreeNode n_0_1_2 = getChildWithName(n_0_1, "0.1.2");
        assertThat(getChildrenNodeNames(n_0_1_2)).containsExactly("0.1.2.1", "0.1.2.2", "0.1.2.3");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1_2, "0.1.2.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1_2, "0.1.2.2"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1_2, "0.1.2.3"));

        TreeNode n_0_2 = getChildWithName(root, "0.2");
        assertThat(getChildrenNodeNames(n_0_2)).containsExactly("0.2.1", "0.2.2");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2, "0.2.1"));

        TreeNode n_0_2_2 = getChildWithName(n_0_2, "0.2.2");
        assertThat(getChildrenNodeNames(n_0_2_2)).containsExactly("0.2.2.1", "0.2.2.2", "0.2.2.3", "0.2.2.4");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.1"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.2"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.3"));
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.4"));
    }

    @Test
    void trimTree() {
        // given
        Map<Long, Long> nodes = ImmutableMap.<Long, Long>builder()
                .put(211L, 21L)
                .put(310L, 31L)
                .build();
        TreeNodeBrowser treeNodeBrowser = newTreeBrowser();
        Map<Long, Long> paths = treeNodeBrowser.expandTreeTo(nodes);

        // when
        treeNodeBrowser.trimTree(paths.keySet());

        // then
        assertThat(getChildrenNodeNames(treeNodeBrowser.getRootNode())).containsExactly("Root");

        TreeNode root = getChildWithName(treeNodeBrowser.getRootNode(), "Root");
        assertThat(getChildrenNodeNames(root)).containsExactly("0.1", "0.2");

        TreeNode n_0_1 = getChildWithName(root, "0.1");
        assertThat(getChildrenNodeNames(n_0_1)).containsExactly("0.1.2");

        TreeNode n_0_1_2 = getChildWithName(n_0_1, "0.1.2");
        assertThat(getChildrenNodeNames(n_0_1_2)).containsExactly("0.1.2.2");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_1_2, "0.1.2.2"));

        TreeNode n_0_2 = getChildWithName(root, "0.2");
        assertThat(getChildrenNodeNames(n_0_2)).containsExactly("0.2.2");

        TreeNode n_0_2_2 = getChildWithName(n_0_2, "0.2.2");
        assertThat(getChildrenNodeNames(n_0_2_2)).containsExactly("0.2.2.1");
        assertContainsOnlyPlaceholderChild(getChildWithName(n_0_2_2, "0.2.2.1"));
    }

    // -------------------- PRIVATE --------------------

    private TreeNodeBrowser newTreeBrowser() {
        Dataverse root = new Dataverse();
        root.setId(1L);
        root.setName("Root");
        NodesInfo info = new NodesInfo(
                testData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getKey().isSelectable() ? NodePermission.SELECT : NodePermission.VIEW)),
                testData.entrySet().stream()
                        .filter(e -> e.getValue().getKey().isExpandable())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet())
        );
        return new TreeNodeBrowser(root, info,
                nodeId -> Optional.ofNullable(testData.get(nodeId)).map(Pair::getValue).filter(id -> id >= 0),
                nodeId -> testData.values().stream()
                        .filter(pair -> pair.getValue().equals(nodeId))
                        .map(Pair::getKey)
                        .collect(Collectors.toList()));
    }

    private NodeData getNodeData(TreeNode node) {
        return (NodeData) node.getData();
    }

    private List<String> getParentNodeNames(TreeNode node) {
        List<String> parents = new ArrayList<>();
        while (node.getParent() != null) {
            parents.add(0, getNodeData(node.getParent()).getName());
            node = node.getParent();
        }
        return parents;
    }

    private TreeNode getParentWithName(TreeNode node, String parentNodeName) {
        while (node.getParent() != null) {
            if (getNodeData(node.getParent()).getName().equals(parentNodeName)) {
                return node.getParent();
            }
            node = node.getParent();
        }
        return null;
    }

    private TreeNode getChildWithName(TreeNode node, String childNodeName) {
        return node.getChildren().stream().filter(n -> getNodeData(n).getName().equals(childNodeName))
                .findFirst()
                .orElse(null);
    }

    private List<String> getChildrenNodeNames(TreeNode node) {
        if (node == null) {
            return Collections.emptyList();
        }
        return node.getChildren().stream().map(this::getNodeData).map(NodeData::getName).collect(Collectors.toList());
    }

    private void assertContainsOnlyPlaceholderChild(TreeNode node) {
        assertThat(node.getChildren()).hasSize(1);
        assertThat(getNodeData(node.getChildren().get(0)).getName()).isEqualTo("PLACEHOLDER");
    }

}
