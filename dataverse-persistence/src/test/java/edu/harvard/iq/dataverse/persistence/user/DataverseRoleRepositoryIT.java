package edu.harvard.iq.dataverse.persistence.user;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import org.junit.Test;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataverseRoleRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private DataverseRoleRepository dataverseRoleRepository;

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
