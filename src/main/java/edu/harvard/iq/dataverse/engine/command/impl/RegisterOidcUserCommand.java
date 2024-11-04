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
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.EJBException;

@RequiredPermissions({})
public class RegisterOidcUserCommand extends AbstractVoidCommand {

    private final String bearerToken;
    private final UserDTO userDTO;

    public RegisterOidcUserCommand(DataverseRequest aRequest, String bearerToken, UserDTO userDTO) {
        super(aRequest, (DvObject) null);
        this.bearerToken = bearerToken;
        this.userDTO = userDTO;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (!userDTO.termsAccepted) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"), this);
        }
        try {
            UserRecordIdentifier userRecordIdentifier = ctxt.authentication().verifyOidcBearerTokenAndGetUserIdentifier(bearerToken);
            User user = ctxt.authentication().lookupUser(userRecordIdentifier);
            if (user != null) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"), this);
            }
            AuthenticatedUserDisplayInfo authenticatedUserDisplayInfo = new AuthenticatedUserDisplayInfo(
                    userDTO.firstName,
                    userDTO.lastName,
                    userDTO.emailAddress,
                    "",
                    ""
            );
            ctxt.authentication().createAuthenticatedUser(userRecordIdentifier, userDTO.username, authenticatedUserDisplayInfo, true);
        } catch (AuthorizationException authorizationException) {
            throw new PermissionException(authorizationException.getMessage(), this, null, null);
        } catch (EJBException ejbException) {
            throw new CommandException(ejbException.getMessage(), this);
        }
    }
}
