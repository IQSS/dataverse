package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateUrlDTOConverterTest {

    @Test
    void convert() {
        // given
        AuthenticatedUser assignee = new AuthenticatedUser();
        assignee.setUserIdentifier("User");

        DataverseRole role = new DataverseRole();
        role.setId(1L);
        role.setAlias("Role 1");

        Dataset dvObject = new Dataset();
        dvObject.setId(3L);

        RoleAssignment assignment = new RoleAssignment(role, assignee, dvObject, "token");
        assignment.setId(21L);

        PrivateUrl siteUrl = new PrivateUrl(assignment, dvObject, "siteUrl");

        // when
        PrivateUrlDTO converted = new PrivateUrlDTO.Converter().convert(siteUrl);

        // then
        assertThat(converted)
                .extracting(PrivateUrlDTO::getToken, PrivateUrlDTO::getLink)
                .contains("token", "siteUrl/privateurl.xhtml?token=token");
        assertThat(converted.getRoleAssignment())
                .extracting(RoleAssignmentDTO::getId, RoleAssignmentDTO::getAssignee, RoleAssignmentDTO::getRoleId,
                        RoleAssignmentDTO::getRoleAlias, RoleAssignmentDTO::getPrivateUrlToken, RoleAssignmentDTO::getDefinitionPointId)
                .containsExactly(21L, "@User", 1L,
                        "Role 1", "token", 3L);
    }
}