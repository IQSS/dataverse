package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.dto.UserListResultDTO.UserDTO;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UserListResultDTOConverterTest {

    @Test
    void convert() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserIdentifier("user");
        user.setId(1L);
        user.setAuthenticatedUserLookup(new AuthenticatedUserLookup("lookup", "lookup"));
        UserListResult userList = new UserListResult(
                new Pager(10, 10, 1), Collections.singletonList(user));

        // when
        UserListResultDTO result = new UserListResultDTO.Converter().convert(userList);

        // then
        assertThat(result.getUsers())
                .extracting(UserDTO::getId, UserDTO::getUserIdentifier)
                .contains(tuple(1L, "user"));
        assertThat(result.getBundleStrings()).isNotEmpty();
        assertThat(result.getPagination())
                .extracting(PagerDTO::getNumResults, PagerDTO::getDocsPerPage)
                .contains(10, 10);
    }
}