package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

@RequiredPermissions({})
public class RegisterOidcUserCommand extends AbstractVoidCommand {

    private final String bearerToken;
    private final UserDTO userDTO;

    private static final String ERROR_USER_ALREADY_REGISTERED_WITH_TOKEN = "User is already registered with this token";

    public RegisterOidcUserCommand(DataverseRequest aRequest, String bearerToken, UserDTO userDTO) {
        super(aRequest, (DvObject) null);
        this.bearerToken = bearerToken;
        this.userDTO = userDTO;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        try {
            UserRecordIdentifier userRecordIdentifier = ctxt.authentication().verifyOidcBearerTokenAndGetUserIdentifier(bearerToken);
            User user = ctxt.authentication().lookupUser(userRecordIdentifier);
            if (user != null) {
                throw new IllegalCommandException(ERROR_USER_ALREADY_REGISTERED_WITH_TOKEN, this);
            }
            AuthenticatedUserDisplayInfo authenticatedUserDisplayInfo = new AuthenticatedUserDisplayInfo(
                    userDTO.firstName,
                    userDTO.lastName,
                    userDTO.emailAddress,
                    "",
                    ""
            );
            ctxt.authentication().createAuthenticatedUser(userRecordIdentifier, userDTO.username, authenticatedUserDisplayInfo, true);
        } catch (AuthorizationException e) {
            throw new RuntimeException(e);
        }
    }
}
