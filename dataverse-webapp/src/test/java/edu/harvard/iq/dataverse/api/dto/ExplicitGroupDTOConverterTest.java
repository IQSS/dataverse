package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExplicitGroupDTOConverterTest {

    @Test
    void convert() {
        // given
        ExplicitGroup group = new ExplicitGroup();
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        group.setOwner(dataset);
        group.setDisplayName("Explicit Group");
        group.setGroupAliasInOwner("explicit-group");
        group.setDescription("Description");
        group.getContainedAuthenticatedUsers()
                .addAll(Stream.of("2", "1", "3")
                        .map(s -> {
                            AuthenticatedUser user = new AuthenticatedUser();
                            user.setId(Long.valueOf(s));
                            user.setUserIdentifier("User " + s);
                            return user;
                        })
                        .collect(Collectors.toList()));

        // when
        ExplicitGroupDTO converted = new ExplicitGroupDTO.Converter().convert(group);

        // then
        assertThat(converted)
                .extracting(ExplicitGroupDTO::getIdentifier, ExplicitGroupDTO::getGroupAliasInOwner, ExplicitGroupDTO::getOwner,
                        ExplicitGroupDTO::getDisplayName, ExplicitGroupDTO::getDescription)
                .contains("&explicit/null", "explicit-group", 1L,
                        "Explicit Group", "Description");
        assertThat(converted.getContainedRoleAssignees())
                .containsExactly("@User 1", "@User 2", "@User 3");
    }
}