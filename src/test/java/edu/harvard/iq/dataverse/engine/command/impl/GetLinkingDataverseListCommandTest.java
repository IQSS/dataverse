package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class GetLinkingDataverseListCommandTest {

    @Mock
    private DataverseRequest dataverseRequest;
    @Mock
    private CommandContext commandContext;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private DataverseServiceBean dataverseService;
    @Mock
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    public void setUp() {
        Mockito.when(dataverseRequest.getUser()).thenReturn(authenticatedUser);
    }

    @Test
    public void execute_shouldThrowException_whenUserIsNotAuthenticated() {
        // Arrange
        Mockito.when(authenticatedUser.isAuthenticated()).thenReturn(false);
        DvObject dvObject = new Dataset();
        GetLinkingDataverseListCommand sut = new GetLinkingDataverseListCommand(dataverseRequest, dvObject, "");

        // Act & Assert
        assertThrows(IllegalCommandException.class, () -> {
            sut.execute(commandContext);
        }, BundleUtil.getStringFromBundle("dataverse.link.user"));
    }

    @Test
    public void execute_shouldReturnPermittedList_forLinkingDataset() throws CommandException {
        // Arrange
        String searchTerm = "test";
        Dataset datasetToLink = new Dataset();
        List<Dataverse> permittedList = Arrays.asList(new Dataverse(), new Dataverse());

        Mockito.when(commandContext.permissions()).thenReturn(permissionService);
        Mockito.when(commandContext.dataverses()).thenReturn(dataverseService);
        Mockito.when(authenticatedUser.isAuthenticated()).thenReturn(true);
        Mockito.when(permissionService.findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataset, searchTerm))
                .thenReturn(permittedList);
        Mockito.when(dataverseService.removeUnlinkableDataverses(permittedList, datasetToLink))
                .thenReturn(permittedList); // Assume none are removed for this test

        GetLinkingDataverseListCommand sut = new GetLinkingDataverseListCommand(dataverseRequest, datasetToLink, searchTerm);

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertEquals(2, result.size());
        assertEquals(permittedList, result);
        Mockito.verify(permissionService).findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataset, searchTerm);
        Mockito.verify(dataverseService).removeUnlinkableDataverses(permittedList, datasetToLink);
    }

    @Test
    public void execute_shouldReturnPermittedList_forLinkingDataverse() throws CommandException {
        // Arrange
        String searchTerm = "test";
        Dataverse dataverseToLink = new Dataverse();
        List<Dataverse> permittedList = Collections.singletonList(new Dataverse());

        Mockito.when(commandContext.permissions()).thenReturn(permissionService);
        Mockito.when(commandContext.dataverses()).thenReturn(dataverseService);
        Mockito.when(authenticatedUser.isAuthenticated()).thenReturn(true);
        Mockito.when(permissionService.findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataverse, searchTerm))
                .thenReturn(permittedList);
        Mockito.when(dataverseService.removeUnlinkableDataverses(permittedList, dataverseToLink))
                .thenReturn(permittedList);

        GetLinkingDataverseListCommand sut = new GetLinkingDataverseListCommand(dataverseRequest, dataverseToLink, searchTerm);

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertEquals(1, result.size());
        assertEquals(permittedList, result);
        Mockito.verify(permissionService).findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataverse, searchTerm);
        Mockito.verify(dataverseService).removeUnlinkableDataverses(permittedList, dataverseToLink);
    }

    @Test
    public void execute_shouldUseEmptyString_whenSearchTermIsNull() throws CommandException {
        // Arrange
        String searchTerm = null;
        Dataset datasetToLink = new Dataset();
        List<Dataverse> expectedList = Collections.emptyList();

        Mockito.when(commandContext.permissions()).thenReturn(permissionService);
        Mockito.when(commandContext.dataverses()).thenReturn(dataverseService);
        Mockito.when(authenticatedUser.isAuthenticated()).thenReturn(true);
        // Note: verify that an empty string "" is passed instead of null
        Mockito.when(permissionService.findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataset, ""))
                .thenReturn(expectedList);
        Mockito.when(dataverseService.removeUnlinkableDataverses(expectedList, datasetToLink))
                .thenReturn(expectedList);

        GetLinkingDataverseListCommand sut = new GetLinkingDataverseListCommand(dataverseRequest, datasetToLink, searchTerm);

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertTrue(result.isEmpty());
        Mockito.verify(permissionService).findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataset, "");
    }

    @Test
    public void execute_shouldReturnEmptyList_whenNoPermittedCollectionsFound() throws CommandException {
        // Arrange
        String searchTerm = "nonexistent";
        Dataset datasetToLink = new Dataset();
        List<Dataverse> emptyList = Collections.emptyList();

        Mockito.when(commandContext.permissions()).thenReturn(permissionService);
        Mockito.when(commandContext.dataverses()).thenReturn(dataverseService);
        Mockito.when(authenticatedUser.isAuthenticated()).thenReturn(true);
        Mockito.when(permissionService.findPermittedCollections(dataverseRequest, authenticatedUser, Permission.LinkDataset, searchTerm))
                .thenReturn(emptyList);
        Mockito.when(dataverseService.removeUnlinkableDataverses(emptyList, datasetToLink))
                .thenReturn(emptyList);

        GetLinkingDataverseListCommand sut = new GetLinkingDataverseListCommand(dataverseRequest, datasetToLink, searchTerm);

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertTrue(result.isEmpty());
    }
}
