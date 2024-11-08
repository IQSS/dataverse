package edu.harvard.iq.dataverse.persistence.user;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.common.DBItegrationTest;

public class RoleAssignmentRepositoryIT extends DBItegrationTest {

    private RoleAssignmentRepository roleAssignmentRepository = new RoleAssignmentRepository(getEntityManager());

    //-------------------- TESTS --------------------

    @Test
    public void findByDefinitionPointId() {
        // when
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByDefinitionPointId(1L);

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 32L, 33L));
    }

    @Test
    public void findByDefinitionPointIds() {
        // given
        ArrayList<Long> definitionPointIds = Lists.newArrayList(1L, 19L, 51L);

        // when
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByDefinitionPointIds(definitionPointIds);

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 7L, 29L, 32L, 33L, 101L, 102L));
    }

    @Test
    public void findByAssigneeIdentifier() {
        // when
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByAssigneeIdentifier("@dataverseAdmin");

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 7L, 29L));
    }

    @Test
    public void findByRoleId() {
        // when
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByRoleId(2L);

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(30L, 31L));
    }

    @Test
    public void findByAssigneeIdentifiersAndDefinitionPointIds() {
        // when
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByAssigneeIdentifiersAndDefinitionPointIds(
                Lists.newArrayList("@dataverseAdmin", "@superuser"),
                Lists.newArrayList(1L, 51L));

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 29L, 33L));
    }

    @Test
    public void deleteAllByAssigneeIdentifier() {

        // when
        int deletedCount = roleAssignmentRepository.deleteAllByAssigneeIdentifier("&mail/toDelete");

        // then
        assertThat(deletedCount, is(2));
    }
}
