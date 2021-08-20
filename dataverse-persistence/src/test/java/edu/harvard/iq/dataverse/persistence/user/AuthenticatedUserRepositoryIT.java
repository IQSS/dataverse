package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import javax.inject.Inject;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticatedUserRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private AuthenticatedUserRepository authenticatedUserRepository;

    //-------------------- TESTS --------------------

    @Test
    public void findSearchedAuthenticatedUsers() {
        // given
        SortKey sortKey = SortKey.ID;
        int resultLimit = 10;
        int offset = 0;
        String searchTerm = StringUtils.EMPTY;
        boolean isSortAscending = true;

        // when
        List<AuthenticatedUser> users = authenticatedUserRepository.findSearchedAuthenticatedUsers(sortKey, resultLimit, offset, searchTerm, isSortAscending);

        // then
        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(1L, 2L, 3L, 4L);
    }

    @Test
    public void findSearchedAuthenticatedUsers_sortedByUserIdentifier_descending() {
        // given
        SortKey sortKey = SortKey.USER_IDENTIFIER;
        int resultLimit = 10;
        int offset = 0;
        String searchTerm = StringUtils.EMPTY;
        boolean isSortAscending = false;

        // when
        List<AuthenticatedUser> users = authenticatedUserRepository.findSearchedAuthenticatedUsers(sortKey, resultLimit, offset, searchTerm, isSortAscending);

        // then
        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getUserIdentifier).containsSequence("superuser", "rootGroupMember", "filedownloader", "dataverseAdmin");
    }

    @Test
    public void findSearchedAuthenticatedUsers_limitedTo_2_offset_1() {
        // given
        SortKey sortKey = SortKey.ID;
        int resultLimit = 2;
        int offset = 1;
        String searchTerm = StringUtils.EMPTY;
        boolean isSortAscending = true;

        // when
        List<AuthenticatedUser> users = authenticatedUserRepository.findSearchedAuthenticatedUsers(sortKey, resultLimit, offset, searchTerm, isSortAscending);

        // then
        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(2L, 3L);
    }

    @Test
    public void findSearchedAuthenticatedUsers_filtered() {
        // given
        SortKey sortKey = SortKey.ID;
        int resultLimit = 10;
        int offset = 0;
        String searchTerm = "some";
        boolean isSortAscending = true;

        // when
        List<AuthenticatedUser> users = authenticatedUserRepository.findSearchedAuthenticatedUsers(sortKey, resultLimit, offset, searchTerm, isSortAscending);

        // then
        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(3L, 4L);
        assertThat(users).extracting(AuthenticatedUser::getAffiliation).containsExactly("some affiliation", "some affiliation");
    }

    @Test
    public void findSearchedAuthenticatedUsers_filtered_noResults() {
        // given
        SortKey sortKey = SortKey.ID;
        int resultLimit = 10;
        int offset = 0;
        String searchTerm = "XXX Non-existent";
        boolean isSortAscending = true;

        // when
        List<AuthenticatedUser> users = authenticatedUserRepository.findSearchedAuthenticatedUsers(sortKey, resultLimit, offset, searchTerm, isSortAscending);

        // then
        assertThat(users.size()).isEqualTo(0);
    }

    @Test
    public void countSearchedAuthenticatedUsers() {
        // given
        String searchTerm = StringUtils.EMPTY;

        // when
        Long usersCounter = authenticatedUserRepository.countSearchedAuthenticatedUsers(searchTerm);

        // then
        assertThat(usersCounter).isEqualTo(4);
    }

    @Test
    public void countSearchedAuthenticatedUsers_filtered() {
        // given
        String searchTerm = "lastname";

        // when
        Long usersCounter = authenticatedUserRepository.countSearchedAuthenticatedUsers(searchTerm);

        // then
        assertThat(usersCounter).isEqualTo(3);
    }

    @Test
    public void countSearchedAuthenticatedUsers_filtered_noResults() {
        // given
        String searchTerm = "XXX Non-existent";

        // when
        Long usersCounter = authenticatedUserRepository.countSearchedAuthenticatedUsers(searchTerm);

        // then
        assertThat(usersCounter).isEqualTo(0);
    }
}
