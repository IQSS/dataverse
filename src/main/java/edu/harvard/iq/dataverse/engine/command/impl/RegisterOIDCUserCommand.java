package edu.harvard.iq.dataverse.engine.command.impl;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.OIDCUserInfo;
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
public class RegisterOIDCUserCommand extends AbstractVoidCommand {

    private final String bearerToken;
    private final UserDTO userDTO;

    public RegisterOIDCUserCommand(DataverseRequest aRequest, String bearerToken, UserDTO userDTO) {
        super(aRequest, (DvObject) null);
        this.bearerToken = bearerToken;
        this.userDTO = userDTO;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        try {
            OIDCUserInfo oidcUserInfo = ctxt.authentication().verifyOIDCBearerTokenAndGetUserIdentifier(bearerToken);
            UserRecordIdentifier userRecordIdentifier = oidcUserInfo.getUserRecordIdentifier();

            if (ctxt.authentication().lookupUser(userRecordIdentifier) != null) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"), this);
            }

            UserInfo userClaimsInfo = oidcUserInfo.getUserClaimsInfo();

            // Update the UserDTO object with available OIDC user claims; keep existing values if claims are absent
            userDTO.setUsername(getValueOrDefault(userClaimsInfo.getPreferredUsername(), userDTO.getUsername()));
            userDTO.setFirstName(getValueOrDefault(userClaimsInfo.getGivenName(), userDTO.getFirstName()));
            userDTO.setLastName(getValueOrDefault(userClaimsInfo.getFamilyName(), userDTO.getLastName()));
            userDTO.setEmailAddress(getValueOrDefault(userClaimsInfo.getEmailAddress(), userDTO.getEmailAddress()));

            AuthenticatedUserDisplayInfo userDisplayInfo = new AuthenticatedUserDisplayInfo(
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getEmailAddress(),
                    userDTO.getAffiliation() != null ? userDTO.getAffiliation() : "",
                    userDTO.getPosition() != null ? userDTO.getPosition() : ""
            );

            Map<String, String> fieldErrors = validateUserFields(ctxt);
            if (!fieldErrors.isEmpty()) {
                throw new InvalidFieldsCommandException(
                        BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.invalidFields"),
                        this,
                        fieldErrors
                );
            }

            ctxt.authentication().createAuthenticatedUser(userRecordIdentifier, userDTO.getUsername(), userDisplayInfo, true);

        } catch (AuthorizationException ex) {
            throw new PermissionException(ex.getMessage(), this, null, null, true);
        }
    }

    private String getValueOrDefault(String oidcValue, String dtoValue) {
        return (oidcValue == null || oidcValue.isEmpty()) ? dtoValue : oidcValue;
    }

    private Map<String, String> validateUserFields(CommandContext ctxt) {
        Map<String, String> fieldErrors = new HashMap<>();

        validateTermsAccepted(fieldErrors);
        validateEmailAddress(ctxt, fieldErrors);
        validateUsername(ctxt, fieldErrors);

        validateRequiredField("firstName", userDTO.getFirstName(), "registerOidcUserCommand.errors.firstNameFieldRequired", fieldErrors);
        validateRequiredField("lastName", userDTO.getLastName(), "registerOidcUserCommand.errors.lastNameFieldRequired", fieldErrors);

        return fieldErrors;
    }

    private void validateTermsAccepted(Map<String, String> fieldErrors) {
        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"));
        }
    }

    private void validateEmailAddress(CommandContext ctxt, Map<String, String> fieldErrors) {
        String emailAddress = userDTO.getEmailAddress();
        if (emailAddress == null || emailAddress.isEmpty()) {
            fieldErrors.put("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.emailFieldRequired"));
        } else if (isEmailInUse(ctxt, emailAddress)) {
            fieldErrors.put("emailAddress", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.emailAddressInUse"));
        }
    }

    private void validateUsername(CommandContext ctxt, Map<String, String> fieldErrors) {
        String username = userDTO.getUsername();
        if (username == null || username.isEmpty()) {
            fieldErrors.put("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.usernameFieldRequired"));
        } else if (isUsernameInUse(ctxt, username)) {
            fieldErrors.put("username", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.usernameInUse"));
        }
    }

    private void validateRequiredField(String fieldName, String fieldValue, String bundleKey, Map<String, String> fieldErrors) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            fieldErrors.put(fieldName, BundleUtil.getStringFromBundle(bundleKey));
        }
    }

    private boolean isEmailInUse(CommandContext ctxt, String emailAddress) {
        return ctxt.authentication().getAuthenticatedUserByEmail(emailAddress) != null;
    }

    private boolean isUsernameInUse(CommandContext ctxt, String username) {
        return ctxt.authentication().getAuthenticatedUser(username) != null;
    }
}
