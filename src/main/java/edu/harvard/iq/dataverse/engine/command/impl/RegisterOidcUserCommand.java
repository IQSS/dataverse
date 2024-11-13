package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.OidcUserInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidFieldsCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> fieldErrors = validateUserFields(ctxt);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidFieldsCommandException(
                    BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.invalidFields"),
                    this,
                    fieldErrors
            );
        }

        createUser(ctxt);
    }

    private Map<String, String> validateUserFields(CommandContext ctxt) {
        Map<String, String> fieldErrors = new HashMap<>();

        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"));
        }

        if (isEmailInUse(ctxt, userDTO.getEmailAddress())) {
            fieldErrors.put("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.emailAddressInUse"));
        }

        if (isUsernameInUse(ctxt, userDTO.getUsername())) {
            fieldErrors.put("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.usernameInUse"));
        }

        return fieldErrors;
    }

    private boolean isEmailInUse(CommandContext ctxt, String emailAddress) {
        return ctxt.authentication().getAuthenticatedUserByEmail(emailAddress) != null;
    }

    private boolean isUsernameInUse(CommandContext ctxt, String username) {
        return ctxt.authentication().getAuthenticatedUser(username) != null;
    }

    private void createUser(CommandContext ctxt) throws CommandException {
        try {
            OidcUserInfo oidcUserInfo = ctxt.authentication().verifyOidcBearerTokenAndGetUserIdentifier(bearerToken);
            UserRecordIdentifier userRecordIdentifier = oidcUserInfo.getUserRecordIdentifier();

            if (ctxt.authentication().lookupUser(userRecordIdentifier) != null) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"), this);
            }

            AuthenticatedUserDisplayInfo userInfo = new AuthenticatedUserDisplayInfo(
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getEmailAddress(),
                    userDTO.getAffiliation() != null ? userDTO.getAffiliation() : "",
                    userDTO.getPosition() != null ? userDTO.getPosition() : ""
            );

            ctxt.authentication().createAuthenticatedUser(userRecordIdentifier, userDTO.getUsername(), userInfo, true);

        } catch (AuthorizationException ex) {
            throw new PermissionException(ex.getMessage(), this, null, null, true);
        }
    }
}
