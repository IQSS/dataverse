package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetUserPermittedCollectionsCommandTest {

    private DataverseRequest dataverseRequest;
    private AuthenticatedUser authenticatedUser;
    private CommandContext commandContext;
    private PermissionServiceBean permissionsServiceBean;

    @BeforeEach
    public void setUp() {
        dataverseRequest = Mockito.mock(DataverseRequest.class);
        authenticatedUser = Mockito.mock(AuthenticatedUser.class);
        commandContext = Mockito.mock(CommandContext.class);
        permissionsServiceBean = Mockito.mock(PermissionServiceBean.class);
        Mockito.when(commandContext.permissions()).thenReturn(permissionsServiceBean);
    }

    @Test
    public void execute_shouldReturnCollections_whenAnyPermissionIsRequested() throws CommandException {
        // Arrange
        Dataverse dv1 = new Dataverse();
        Dataverse dv2 = new Dataverse();
        List<Dataverse> expectedDataverses = Arrays.asList(dv1, dv2);

        Mockito.when(permissionsServiceBean.findPermittedCollections(
                Mockito.any(DataverseRequest.class),
                Mockito.any(AuthenticatedUser.class),
                Mockito.eq(Integer.MAX_VALUE),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(expectedDataverses);

        GetUserPermittedCollectionsCommand sut = new GetUserPermittedCollectionsCommand(
                dataverseRequest,
                authenticatedUser,
                GetUserPermittedCollectionsCommand.ANY_PERMISSION
        );

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertEquals(expectedDataverses.size(), result.size());
        assertEquals(expectedDataverses, result);
        Mockito.verify(permissionsServiceBean).findPermittedCollections(
                dataverseRequest,
                authenticatedUser,
                Integer.MAX_VALUE,
                null,
                null
        );
    }

    @Test
    public void execute_shouldReturnCollections_whenSpecificPermissionIsRequested() throws CommandException {
        // Arrange
        Dataverse dv = new Dataverse();
        List<Dataverse> expectedDataverses = Collections.singletonList(dv);

        Mockito.when(permissionsServiceBean.findPermittedCollections(
                Mockito.any(DataverseRequest.class),
                Mockito.any(AuthenticatedUser.class),
                Mockito.eq(1 << Permission.AddDataset.ordinal()),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(expectedDataverses);

        GetUserPermittedCollectionsCommand sut = new GetUserPermittedCollectionsCommand(
                dataverseRequest,
                authenticatedUser,
                Permission.AddDataset.name(),
                null,
                null
        );

        // Act
        List<Dataverse> result = sut.execute(commandContext);

        // Assert
        assertEquals(expectedDataverses.size(), result.size());
        assertEquals(expectedDataverses, result);
        Mockito.verify(permissionsServiceBean).findPermittedCollections(
                dataverseRequest,
                authenticatedUser,
                1 << Permission.AddDataset.ordinal(),
                null,
                null
        );
    }

    @Test
    public void execute_shouldThrowException_whenUserIsNotFound() {
        // Arrange
        AuthenticatedUser nullUser = null;
        GetUserPermittedCollectionsCommand sut = new GetUserPermittedCollectionsCommand(
                dataverseRequest,
                nullUser,
                GetUserPermittedCollectionsCommand.ANY_PERMISSION
        );

        // Act & Assert
        CommandException exception = assertThrows(CommandException.class, () -> {
            sut.execute(commandContext);
        });
        assertEquals(BundleUtil.getStringFromBundle("getUserPermittedCollectionsCommand.errors.userNotFound"), exception.getMessage());
    }

    @Test
    public void execute_shouldThrowException_whenPermissionIsNotValid() {
        // Arrange
        String invalidPermission = "invalid_permission_name";
        GetUserPermittedCollectionsCommand sut = new GetUserPermittedCollectionsCommand(
                dataverseRequest,
                authenticatedUser,
                invalidPermission
        );

        // Act & Assert
        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> {
            sut.execute(commandContext);
        });
        assertEquals(BundleUtil.getStringFromBundle("getUserPermittedCollectionsCommand.errors.permissionNotValid"), exception.getMessage());
    }
}
