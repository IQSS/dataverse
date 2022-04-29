package edu.harvard.iq.dataverse.authorization.groups.impl.saml;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;
import edu.harvard.iq.dataverse.persistence.group.SamlGroupRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamlGroupServiceTest {

    @Mock
    private RoleAssigneeServiceBean roleAssigneeSvc;

    @Mock
    private ActionLogServiceBean actionLogSvc;

    @Mock
    private SamlGroupRepository repository;

    @InjectMocks
    private SamlGroupService service;

    // -------------------- TESTS --------------------

    @Test
    void findAll() {
        // given
        when(repository.findAll()).thenReturn(Arrays.asList(
                new SamlGroup("a1", "eid1"),
                new SamlGroup("a2", "eid2")));

        // when
        List<SamlGroup> groups = service.findAll();

        // then
        assertThat(groups)
                .extracting(SamlGroup::getName)
                .containsExactly("a1", "a2");
    }

    @Test
    void findById() {
        // given
        when(repository.findById(34L))
                .thenReturn(Optional.of(new SamlGroup("g34", "eid34")));

        // when
        SamlGroup group = service.findById(34L);

        // then
        assertThat(group)
                .extracting(SamlGroup::getName, SamlGroup::getEntityId)
                .containsExactly("g34", "eid34");
    }

    @Test
    void save() {
        // given
        SamlGroup group = new SamlGroup("gr1", "eid1");

        // when
        service.save(group.getName(), group.getEntityId());

        // then
        verify(repository, times(1)).saveAndFlush(group);
    }

    @Test
    void findFor() {
        // given
        SamlGroup group = new SamlGroup("abc", "abcId");
        AuthenticatedUser user = new AuthenticatedUser();
        user.setSamlIdPEntityId(group.getEntityId());
        when(repository.findByEntityId("abcId")).thenReturn(Collections.singletonList(group));

        // when
        Set<SamlGroup> groups = service.findFor(user);

        // then
        assertThat(groups).containsExactly(group);
    }

    @Test
    void delete() throws Exception {
        // given
        SamlGroup toDelete = new SamlGroup("toDelete", "eid");

        // when
        service.delete(toDelete);

        // then
        verify(repository, times(1)).mergeAndDelete(toDelete);
    }
}