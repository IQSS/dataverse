package edu.harvard.iq.dataverse.persistence.user;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;

public class DataverseRoleRepositoryIT extends DBItegrationTest {

    private DataverseRoleRepository dataverseRoleRepository = new DataverseRoleRepository(getEntityManager());

    //-------------------- TESTS --------------------

    @Test
    public void findByOwnerId() {
        // when
        List<DataverseRole> dataverseRoles = dataverseRoleRepository.findByOwnerId(51L);

        // then
        assertThat(
                dataverseRoles.stream().map(DataverseRole::getAlias).collect(toList()),
                containsInAnyOrder("unreleased_dv_test_role"));
    }

    @Test
    public void findWithoutOwner() {
        // when
        List<DataverseRole> dataverseRoles = dataverseRoleRepository.findWithoutOwner();

        // then
        assertThat(
                dataverseRoles.stream().map(DataverseRole::getAlias).collect(toList()),
                containsInAnyOrder(Arrays.asList(BuiltInRole.values())
                        .stream()
                        .map(builtInRole -> builtInRole.getAlias())
                        .collect(toList())
                        .toArray()));
    }

    @Test
    public void findByAlias() {
        // when
        Optional<DataverseRole> dataverseRole = dataverseRoleRepository.findByAlias("admin");

        // then
        assertTrue(dataverseRole.isPresent());
        assertEquals("admin", dataverseRole.get().getAlias());
    }

    @Test
    public void findByAlias_no_such_role() {
        // when
        Optional<DataverseRole> dataverseRole = dataverseRoleRepository.findByAlias("not_existing_role_alias");

        // then
        assertFalse(dataverseRole.isPresent());
    }
}
