package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailDomainGroupProviderTest {

    @Mock private MailDomainGroupService mailDomainGroupService;

    @InjectMocks MailDomainGroupProvider provider;

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should return groups for request with authenticated user")
    void groupsFor__dataverseRequest_authenticatedUser() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getAuthenticatedUser()).thenReturn(user);

        Set<MailDomainGroup> groups = Stream.of(
                MailDomainGroupTestUtil.createGroup("1", new String[] {".edu.pl"}, new String[0]),
                MailDomainGroupTestUtil.createGroup("2", new String[] {".pl"}, new String[0])
        ).collect(Collectors.toSet());
        when(mailDomainGroupService.getGroupsForUser(user)).thenReturn(groups);

        // when
        Set<MailDomainGroup> groupsForRequest = provider.groupsFor(request);

        // then
        assertThat(groupsForRequest).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should return empty set for request without user")
    void groupsFor__dataverseRequest_noUser() {

        // given
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getAuthenticatedUser()).thenReturn(null);

        // when
        Set<MailDomainGroup> groupsForRequest = provider.groupsFor(request);

        // then
        assertThat(groupsForRequest).isEmpty();
    }

    @Test
    @DisplayName("Should return groups for authenticated user as RoleAssignee")
    void groupsFor__roleAssignee_authenticatedUser() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);

        Set<MailDomainGroup> groups = Stream.of(
                MailDomainGroupTestUtil.createGroup("a1", new String[] {".edu.pl"}, new String[0]),
                MailDomainGroupTestUtil.createGroup("a2", new String[] {".pl"}, new String[0])
        ).collect(Collectors.toSet());
        when(mailDomainGroupService.getGroupsForUser(user)).thenReturn(groups);

        // when
        Set<MailDomainGroup> groupsForRequest = provider.groupsFor(user);

        // then
        assertThat(groupsForRequest).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should return empty set for RoleAssignee that is not a real, authenticated user")
    void groupsFor__roleAssignee_noRealAuthenticatedUser() {

        // given
        Set<RoleAssignee> roleAssignees = Stream.of(
                AuthenticatedUsers.get(),
                AllUsers.get(),
                new PrivateUrlUser(1L),
                null)
                .collect(Collectors.toSet());

        // when & then
        roleAssignees.forEach(
                ra -> assertThat(provider.groupsFor(ra)).isEmpty());
    }

    @Test
    @DisplayName("Should find group with the given alias if exists")
    void get() {

        // given
        String groupAlias = "abc";
        MailDomainGroup group = MailDomainGroupTestUtil.createGroup(groupAlias, new String[] {".edu.pl"}, new String[0]);
        when(mailDomainGroupService.getGroup(groupAlias)).thenReturn(Optional.of(group));

        // when
        MailDomainGroup found = provider.get(groupAlias);

        // then
        assertThat(found).isEqualTo(group);
    }

    @Test
    @DisplayName("Should return null for the given alias if there is no such group")
    void get__notFound() {

        // given
        when(mailDomainGroupService.getGroup(anyString())).thenReturn(Optional.empty());

        // when
        MailDomainGroup found = provider.get("abc");

        // then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should find all groups")
    void findGlobalGroups() {

        // given
        List<MailDomainGroup> groups = Stream.of(
                MailDomainGroupTestUtil.createGroup("a", new String[]{".edu.co.uk"}, new String[0]),
                MailDomainGroupTestUtil.createGroup("b", new String[]{".co.uk"}, new String[0])
        ).collect(Collectors.toList());
        when(mailDomainGroupService.getAllGroups()).thenReturn(groups);

        // when
        Set<MailDomainGroup> globalGroups = provider.findGlobalGroups();

        // then
        assertThat(globalGroups).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should check if groups for the given request contain or not the given group")
    void contains() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getAuthenticatedUser()).thenReturn(user);

        MailDomainGroup groupX = MailDomainGroupTestUtil.createGroup("x", new String[]{".edu.pl"}, new String[0]);
        MailDomainGroup groupZ = MailDomainGroupTestUtil.createGroup("z", new String[]{".edu.pl"}, new String[0]);

        Set<MailDomainGroup> groups = Stream.of(
                groupX,
                MailDomainGroupTestUtil.createGroup("y", new String[]{".pl"}, new String[0])
        ).collect(Collectors.toSet());
        when(mailDomainGroupService.getGroupsForUser(user)).thenReturn(groups);

        // when & then
        assertThat(provider.contains(request, groupX)).isTrue();
        assertThat(provider.contains(request, groupZ)).isFalse();
    }
}