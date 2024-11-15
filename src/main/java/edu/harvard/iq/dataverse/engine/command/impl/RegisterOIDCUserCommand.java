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
import edu.harvard.iq.dataverse.settings.FeatureFlags;
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
            boolean provideMissingClaimsEnabled = FeatureFlags.API_BEARER_AUTH_PROVIDE_MISSING_CLAIMS.enabled();

            updateUserDTO(userClaimsInfo, provideMissingClaimsEnabled);

            AuthenticatedUserDisplayInfo userDisplayInfo = new AuthenticatedUserDisplayInfo(
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getEmailAddress(),
                    userDTO.getAffiliation() != null ? userDTO.getAffiliation() : "",
                    userDTO.getPosition() != null ? userDTO.getPosition() : ""
            );

            Map<String, String> fieldErrors = validateUserFields(ctxt, provideMissingClaimsEnabled);
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

    private void updateUserDTO(UserInfo userClaimsInfo, boolean provideMissingClaimsEnabled) {
        if (provideMissingClaimsEnabled) {
            // Update with available OIDC claims, keep existing values if claims are absent
            userDTO.setUsername(getValueOrDefault(userClaimsInfo.getPreferredUsername(), userDTO.getUsername()));
            userDTO.setFirstName(getValueOrDefault(userClaimsInfo.getGivenName(), userDTO.getFirstName()));
            userDTO.setLastName(getValueOrDefault(userClaimsInfo.getFamilyName(), userDTO.getLastName()));
            userDTO.setEmailAddress(getValueOrDefault(userClaimsInfo.getEmailAddress(), userDTO.getEmailAddress()));
        } else {
            // Always use the claims provided by the OIDC provider, regardless of whether they are null or not
            userDTO.setUsername(userClaimsInfo.getPreferredUsername());
            userDTO.setFirstName(userClaimsInfo.getGivenName());
            userDTO.setLastName(userClaimsInfo.getFamilyName());
            userDTO.setEmailAddress(userClaimsInfo.getEmailAddress());
        }
    }

    private String getValueOrDefault(String oidcValue, String dtoValue) {
        return (oidcValue == null || oidcValue.isEmpty()) ? dtoValue : oidcValue;
    }

    private Map<String, String> validateUserFields(CommandContext ctxt, boolean provideMissingClaimsEnabled) {
        Map<String, String> fieldErrors = new HashMap<>();

        validateTermsAccepted(fieldErrors);
        validateField(fieldErrors, "emailAddress", userDTO.getEmailAddress(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, "username", userDTO.getUsername(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, "firstName", userDTO.getFirstName(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, "lastName", userDTO.getLastName(), ctxt, provideMissingClaimsEnabled);

        return fieldErrors;
    }

    private void validateTermsAccepted(Map<String, String> fieldErrors) {
        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put("termsAccepted", BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"));
        }
    }

    private void validateField(Map<String, String> fieldErrors, String fieldName, String fieldValue, CommandContext ctxt, boolean provideMissingClaimsEnabled) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            String errorKey = provideMissingClaimsEnabled ?
                    "registerOidcUserCommand.errors.provideMissingClaimsEnabled." + fieldName + "FieldRequired" :
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled." + fieldName + "FieldRequired";
            fieldErrors.put(fieldName, BundleUtil.getStringFromBundle(errorKey));
        } else if (isFieldInUse(ctxt, fieldName, fieldValue)) {
            fieldErrors.put(fieldName, BundleUtil.getStringFromBundle("registerOidcUserCommand.errors." + fieldName + "InUse"));
        }
    }

    private boolean isFieldInUse(CommandContext ctxt, String fieldName, String value) {
        if ("emailAddress".equals(fieldName)) {
            return ctxt.authentication().getAuthenticatedUserByEmail(value) != null;
        } else if ("username".equals(fieldName)) {
            return ctxt.authentication().getAuthenticatedUser(value) != null;
        }
        return false;
    }
}
