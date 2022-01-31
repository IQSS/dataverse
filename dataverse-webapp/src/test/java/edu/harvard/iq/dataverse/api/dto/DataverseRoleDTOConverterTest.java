package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DataverseRoleDTOConverterTest {

    @Test
    void convert() {
        // given
        DataverseRole role = new DataverseRole();
        role.setId(1L);
        role.setAlias("role-alias");
        role.setName("role-name");
        role.setDescription("description");
        role.addPermissions(Arrays.asList(Permission.EditDataset, Permission.AddDataset));

        // when
        DataverseRoleDTO converted = new DataverseRoleDTO.Converter().convert(role);

        // then
        assertThat(converted).extracting(DataverseRoleDTO::getId, DataverseRoleDTO::getOwnerId, DataverseRoleDTO::getName,
                DataverseRoleDTO::getAlias, DataverseRoleDTO::getDescription)
                .containsExactly(1L, null, "role-name",
                        "role-alias", "description");
        assertThat(converted.getPermissions())
                .containsExactlyInAnyOrder("EditDataset", "AddDataset");
    }
}