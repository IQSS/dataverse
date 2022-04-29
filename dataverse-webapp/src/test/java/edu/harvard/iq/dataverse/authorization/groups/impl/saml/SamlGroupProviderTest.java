package edu.harvard.iq.dataverse.authorization.groups.impl.saml;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamlGroupProviderTest {

    @Mock
    private SamlGroupService samlGroupService;

    @InjectMocks
    SamlGroupProvider provider;

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should return groups for request with authenticated user")
    void groupsFor__dataverseRequest_authenticatedUser() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getUser()).thenReturn(user);

        Set<SamlGroup> groups = Stream.of(
                new SamlGroup("1", "entityId1"),
                new SamlGroup("2", "entityId2"))
                .collect(Collectors.toSet());
        when(samlGroupService.findFor(user)).thenReturn(groups);

        // when
        Set<SamlGroup> groupsForRequest = provider.groupsFor(request);

        // then
        assertThat(groupsForRequest).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should return empty set for request without user")
    void groupsFor__dataverseRequest_noUser() {

        // given
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getUser()).thenReturn(null);

        // when
        Set<SamlGroup> groupsForRequest = provider.groupsFor(request);

        // then
        assertThat(groupsForRequest).isEmpty();
    }

    @Test
    @DisplayName("Should return groups for authenticated user as RoleAssignee")
    void groupsFor__roleAssignee_authenticatedUser() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);

        Set<SamlGroup> groups = Stream.of(
                new SamlGroup("a1", "entityId1"),
                new SamlGroup("a2", "entityId2"))
                .collect(Collectors.toSet());
        when(samlGroupService.findFor(user)).thenReturn(groups);

        // when
        Set<SamlGroup> groupsForRequest = provider.groupsFor(user);

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
    @DisplayName("Should find group with the given alias (id) if exists")
    void get() {

        // given
        String groupAlias = "1";
        SamlGroup group = new SamlGroup("a1", "entityId");
        when(samlGroupService.findById(1L)).thenReturn(group);

        // when
        SamlGroup found = provider.get(groupAlias);

        // then
        assertThat(found).isEqualTo(group);
    }

    @Test
    @DisplayName("Should return null for the given alias (id) if there is no such group")
    void get__notFound() {

        // given
        when(samlGroupService.findById(anyLong())).thenReturn(null);

        // when
        SamlGroup found = provider.get("123");

        // then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should find all groups")
    void findGlobalGroups() {

        // given
        List<SamlGroup> groups = Stream.of(
                new SamlGroup("a", "entityIdA"),
                new SamlGroup("b", "entityIdB"))
                .collect(Collectors.toList());
        when(samlGroupService.findAll()).thenReturn(groups);

        // when
        Set<SamlGroup> globalGroups = provider.findGlobalGroups();

        // then
        assertThat(globalGroups).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should check if groups for the given request contain or not the given group")
    void contains() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        DataverseRequest request = mock(DataverseRequest.class);
        when(request.getUser()).thenReturn(user);

        SamlGroup groupX = new SamlGroup("x", "entityIdX");
        SamlGroup groupZ = new SamlGroup("z", "entityIdZ");

        Set<SamlGroup> groups = Stream.of(
                groupX,
                new SamlGroup("y", "entityIdY"))
                .collect(Collectors.toSet());
        when(samlGroupService.findFor(user)).thenReturn(groups);

        // when & then
        assertThat(provider.contains(request, groupX)).isTrue();
        assertThat(provider.contains(request, groupZ)).isFalse();
    }
}