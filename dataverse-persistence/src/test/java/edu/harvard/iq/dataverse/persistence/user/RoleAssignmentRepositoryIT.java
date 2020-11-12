package edu.harvard.iq.dataverse.persistence.user;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Test;

import javax.inject.Inject;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class RoleAssignmentRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private RoleAssignmentRepository roleAssignmentRepository;

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

}
