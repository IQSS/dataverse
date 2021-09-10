package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.converters.MailGroupConverter;
import edu.harvard.iq.dataverse.api.dto.MailDomainGroupDTO;
import edu.harvard.iq.dataverse.api.dto.ApiResponseDTO;
import edu.harvard.iq.dataverse.authorization.groups.impl.mail.MailDomainGroupService;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;
import edu.harvard.iq.dataverse.persistence.group.MailDomainProcessingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.ejb.EJBException;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailGroupsTest {

    private MailDomainGroupService groupService;

    private MailGroups endpoint;


    @BeforeEach
    void setUp() {
        groupService = mock(MailDomainGroupService.class);
        endpoint = new MailGroups(new MailGroupConverter(), groupService);
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should return all groups")
    void getAllGroups() {

        // given
        MailDomainGroup group = new MailDomainGroup();
        group.setPersistedGroupAlias("abc");
        group.getDomainItems().add(new MailDomainItem(".edu.pl", MailDomainProcessingType.INCLUDE, group));
        when(groupService.getAllGroups()).thenReturn(Collections.singletonList(group));

        // when
        Response response = endpoint.getAllGroups();

        // then
        ApiResponseDTO<List<MailDomainGroupDTO>> entity = (ApiResponseDTO<List<MailDomainGroupDTO>>) response.getEntity();
        assertThat(entity.getData()).extracting(MailDomainGroupDTO::getAlias)
                .containsExactly("abc");
    }

    @Test
    @DisplayName("Should add group")
    void addOrUpdateGroup() {

        // given
        MailDomainGroupDTO toSave = new MailDomainGroupDTO();
        toSave.setAlias("toSave");
        toSave.setInclusions(Stream.of("icm.edu.pl").collect(toList()));

        // when
        Response response = endpoint.addOrUpdateGroup(toSave);

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("Should return BAD REQUEST if there is a validation exception")
    void addOrUpdateGroup__validationFail() {

        // given
        MailDomainGroupDTO toSave = new MailDomainGroupDTO();
        toSave.setAlias("toSave");
        toSave.setInclusions(Stream.of("icm.edu.pl").collect(toList()));

        ConstraintViolationException cve = new ConstraintViolationException(Collections.emptySet());
        EJBException ee = new EJBException(cve);
        doThrow(ee).when(groupService).saveOrUpdateGroup(any());

        // when
        Response response = endpoint.addOrUpdateGroup(toSave);

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Should return group for the given alias")
    void getGroup() {

        // given
        String groupAlias = "icm";
        MailDomainGroup icmGroup = new MailDomainGroup();
        icmGroup.setPersistedGroupAlias(groupAlias);
        icmGroup.getDomainItems().add(new MailDomainItem(".edu.pl", MailDomainProcessingType.INCLUDE, icmGroup));

        when(groupService.getGroup(groupAlias)).thenReturn(Optional.of(icmGroup));

        // when
        Response response = endpoint.getGroup(groupAlias);

        // then
        ApiResponseDTO<MailDomainGroupDTO> entity = (ApiResponseDTO<MailDomainGroupDTO>) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getData().getAlias()).isEqualTo(groupAlias);
    }

    @Test
    @DisplayName("Should return NOT FOUND for the given alias if there is no such group")
    void getGroup__notFound() {

        // given
        when(groupService.getGroup(any())).thenReturn(Optional.empty());

        // when
        Response response = endpoint.getGroup("abc");

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Should delete a group with the given alias")
    void deleteGroup() {

        // given
        String groupAlias = "abc";
        MailDomainGroup deleted = new MailDomainGroup();
        deleted.setPersistedGroupAlias(groupAlias);
        when(groupService.deleteGroup(groupAlias)).thenReturn(Optional.of(deleted));

        // when
        Response response = endpoint.deleteGroup(groupAlias);

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("Should return NOT FOUND when trying to delete group that does not exist")
    void deleteGroup__groupDoesNotExist() {

        // given
        when(groupService.deleteGroup(any())).thenReturn(Optional.empty());

        // when
        Response response = endpoint.deleteGroup("toDelete");

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}