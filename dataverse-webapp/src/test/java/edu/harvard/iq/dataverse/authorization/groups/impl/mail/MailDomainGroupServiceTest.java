package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroupRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailDomainGroupServiceTest {

    @Mock private MailDomainGroupRepository mailGroupRepository;
    @Mock private RoleAssignmentRepository roleAssignmentRepository;
    @Mock private MailDomainCachedMatcherBean matcher;
    @Mock private ConfirmEmailServiceBean confirmEmailService;
    @Mock private ActionLogServiceBean actionLogService;

    @InjectMocks
    private MailDomainGroupService service;

    @Test
    @DisplayName("Should find all groups")
    void getAllGroups() {

        // given
        List<MailDomainGroup> groups = Stream.of(
                MailDomainGroupTestUtil.createGroup("a", new String[]{".edu.co.uk"}, new String[0]),
                MailDomainGroupTestUtil.createGroup("b", new String[]{".co.uk"}, new String[0])
        ).collect(Collectors.toList());
        when(mailGroupRepository.findAll()).thenReturn(groups);

        // when
        List<MailDomainGroup> allGroups = service.getAllGroups();

        // then
        assertThat(allGroups).containsExactlyInAnyOrderElementsOf(groups);
    }

    @Test
    @DisplayName("Should find group with the given alias if exists")
    void getGroup() {

        // given
        String groupAlias = "abc";
        MailDomainGroup group = MailDomainGroupTestUtil.createGroup(groupAlias, new String[] {".edu.pl"}, new String[0]);
        when(mailGroupRepository.findByAlias(groupAlias)).thenReturn(Optional.of(group));

        // when
        Optional<MailDomainGroup> found = service.getGroup(groupAlias);

        // then
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get()).isEqualTo(group);
    }

    @Test
    @DisplayName("Should return empty optional for the given alias if there is no such group")
    void getGroup__notFound() {

        // given
        when(mailGroupRepository.findByAlias(anyString())).thenReturn(Optional.empty());

        // when
        Optional<MailDomainGroup> found = service.getGroup("abc");

        // then
        assertThat(found.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should save the group, rebuild index and log")
    void saveOrUpdateGroup() {

        // given
        when(mailGroupRepository.findByAlias(anyString())).thenReturn(Optional.empty());
        MailDomainGroup toSave = MailDomainGroupTestUtil.createGroup("toSave", new String[]{"icm.edu.pl"}, new String[0]);
        when(mailGroupRepository.findAll()).thenReturn(Collections.singletonList(toSave));

        // when
        service.saveOrUpdateGroup(toSave);

        // then
        verify(mailGroupRepository).saveAndFlush(toSave);
        verify(matcher).rebuildIndex(anyCollection());
        verify(actionLogService).log(any());
    }

    @Test
    @DisplayName("Should delete group, rebuild index and log")
    void deleteGroup() {

        // given
        String groupAlias = "toDelete";
        MailDomainGroup toDelete = MailDomainGroupTestUtil.createGroup(groupAlias, new String[] {".edu.pl"}, new String[0]);
        when(mailGroupRepository.findByAlias(groupAlias)).thenReturn(Optional.of(toDelete));
        when(mailGroupRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        service.deleteGroup(groupAlias);

        // then
        verify(mailGroupRepository).mergeAndDelete(toDelete);
        verify(roleAssignmentRepository).deleteAllByAssigneeIdentifier(anyString());
        verify(matcher).rebuildIndex(anyCollection());
        verify(actionLogService).log(any());
    }

    @Test
    @DisplayName("Should return empty group set for a user with unverified email")
    void getGroupsForUser__emailNotVerified() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(confirmEmailService.hasVerifiedEmail(user)).thenReturn(false);

        // when
        Set<MailDomainGroup> groupsForUser = service.getGroupsForUser(user);

        // then
        assertThat(groupsForUser).isEmpty();
    }

    @Test
    @DisplayName("Should return empty group set for a user with null email")
    void getGroupsForUser__nullEmail() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(confirmEmailService.hasVerifiedEmail(user)).thenReturn(true);
        when(user.getEmail()).thenReturn(null);

        // when
        Set<MailDomainGroup> groupsForUser = service.getGroupsForUser(user);

        // then
        assertThat(groupsForUser).isEmpty();
    }

    @Test
    @DisplayName("Should return groups for user with proper, verified mail")
    void getGroupsForUser() {

        // given
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(confirmEmailService.hasVerifiedEmail(user)).thenReturn(true);
        when(user.getEmail()).thenReturn("user@icm.edu.pl");

        MailDomainGroup icmGroup = MailDomainGroupTestUtil.createGroup("icm", new String[]{"icm.edu.pl"}, new String[0]);

        when(matcher.matchGroupsForDomain("icm.edu.pl")).thenReturn(Collections.singleton(icmGroup));

        // when
        Set<MailDomainGroup> groupsForUser = service.getGroupsForUser(user);

        // then
        assertThat(groupsForUser).containsExactly(icmGroup);
    }
}