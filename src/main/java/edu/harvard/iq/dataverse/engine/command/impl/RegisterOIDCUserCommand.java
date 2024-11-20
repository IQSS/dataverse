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
import java.util.List;
import java.util.Map;

@RequiredPermissions({})
public class RegisterOIDCUserCommand extends AbstractVoidCommand {

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final String FIELD_EMAIL_ADDRESS = "emailAddress";
    private static final String FIELD_TERMS_ACCEPTED = "termsAccepted";

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

            validateUserFields(ctxt, provideMissingClaimsEnabled);

            ctxt.authentication().createAuthenticatedUser(userRecordIdentifier, userDTO.getUsername(), userDisplayInfo, true);

        } catch (AuthorizationException ex) {
            throw new PermissionException(ex.getMessage(), this, null, null, true);
        }
    }

    private void updateUserDTO(UserInfo userClaimsInfo, boolean provideMissingClaimsEnabled) throws InvalidFieldsCommandException {
        if (provideMissingClaimsEnabled) {
            Map<String, String> fieldErrors = validateConflictingClaims(userClaimsInfo);
            throwInvalidFieldsCommandExceptionIfErrorsExist(fieldErrors);
            updateUserDTOWithClaims(userClaimsInfo);
        } else {
            overwriteUserDTOWithClaims(userClaimsInfo);
        }
    }

    private Map<String, String> validateConflictingClaims(UserInfo userClaimsInfo) {
        Map<String, String> fieldErrors = new HashMap<>();

        addFieldErrorIfConflict(FIELD_USERNAME, userClaimsInfo.getPreferredUsername(), userDTO.getUsername(), fieldErrors);
        addFieldErrorIfConflict(FIELD_FIRST_NAME, userClaimsInfo.getGivenName(), userDTO.getFirstName(), fieldErrors);
        addFieldErrorIfConflict(FIELD_LAST_NAME, userClaimsInfo.getFamilyName(), userDTO.getLastName(), fieldErrors);
        addFieldErrorIfConflict(FIELD_EMAIL_ADDRESS, userClaimsInfo.getEmailAddress(), userDTO.getEmailAddress(), fieldErrors);

        return fieldErrors;
    }

    private void addFieldErrorIfConflict(String fieldName, String claimValue, String existingValue, Map<String, String> fieldErrors) {
        if (claimValue != null && existingValue != null && !claimValue.equals(existingValue)) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider",
                    List.of(fieldName)
            );
            fieldErrors.put(fieldName, errorMessage);
        }
    }

    private void updateUserDTOWithClaims(UserInfo userClaimsInfo) {
        userDTO.setUsername(getValueOrDefault(userClaimsInfo.getPreferredUsername(), userDTO.getUsername()));
        userDTO.setFirstName(getValueOrDefault(userClaimsInfo.getGivenName(), userDTO.getFirstName()));
        userDTO.setLastName(getValueOrDefault(userClaimsInfo.getFamilyName(), userDTO.getLastName()));
        userDTO.setEmailAddress(getValueOrDefault(userClaimsInfo.getEmailAddress(), userDTO.getEmailAddress()));
    }

    private void overwriteUserDTOWithClaims(UserInfo userClaimsInfo) {
        userDTO.setUsername(userClaimsInfo.getPreferredUsername());
        userDTO.setFirstName(userClaimsInfo.getGivenName());
        userDTO.setLastName(userClaimsInfo.getFamilyName());
        userDTO.setEmailAddress(userClaimsInfo.getEmailAddress());
    }

    private void throwInvalidFieldsCommandExceptionIfErrorsExist(Map<String, String> fieldErrors) throws InvalidFieldsCommandException {
        if (!fieldErrors.isEmpty()) {
            throw new InvalidFieldsCommandException(
                    BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.invalidFields"),
                    this,
                    fieldErrors
            );
        }
    }

    private String getValueOrDefault(String oidcValue, String dtoValue) {
        return (oidcValue == null || oidcValue.isEmpty()) ? dtoValue : oidcValue;
    }

    private void validateUserFields(CommandContext ctxt, boolean provideMissingClaimsEnabled) throws InvalidFieldsCommandException {
        Map<String, String> fieldErrors = new HashMap<>();

        validateTermsAccepted(fieldErrors);
        validateField(fieldErrors, FIELD_EMAIL_ADDRESS, userDTO.getEmailAddress(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, FIELD_USERNAME, userDTO.getUsername(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, FIELD_FIRST_NAME, userDTO.getFirstName(), ctxt, provideMissingClaimsEnabled);
        validateField(fieldErrors, FIELD_LAST_NAME, userDTO.getLastName(), ctxt, provideMissingClaimsEnabled);

        throwInvalidFieldsCommandExceptionIfErrorsExist(fieldErrors);
    }

    private void validateTermsAccepted(Map<String, String> fieldErrors) {
        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put(FIELD_TERMS_ACCEPTED, BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userShouldAcceptTerms"));
        }
    }

    private void validateField(Map<String, String> fieldErrors, String fieldName, String fieldValue, CommandContext ctxt, boolean provideMissingClaimsEnabled) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            String errorKey = provideMissingClaimsEnabled ?
                    "registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldRequired" :
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled.fieldRequired";
            fieldErrors.put(fieldName, BundleUtil.getStringFromBundle(errorKey, List.of(fieldName)));
        } else if (isFieldInUse(ctxt, fieldName, fieldValue)) {
            fieldErrors.put(fieldName, BundleUtil.getStringFromBundle("registerOidcUserCommand.errors." + fieldName + "InUse"));
        }
    }

    private boolean isFieldInUse(CommandContext ctxt, String fieldName, String value) {
        if (FIELD_EMAIL_ADDRESS.equals(fieldName)) {
            return ctxt.authentication().getAuthenticatedUserByEmail(value) != null;
        } else if (FIELD_USERNAME.equals(fieldName)) {
            return ctxt.authentication().getAuthenticatedUser(value) != null;
        }
        return false;
    }
}
