package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
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
            OAuth2UserRecord oAuth2UserRecord = ctxt.authentication().verifyOIDCBearerTokenAndGetOAuth2UserRecord(bearerToken);
            UserRecordIdentifier userRecordIdentifier = oAuth2UserRecord.getUserRecordIdentifier();

            if (ctxt.authentication().lookupUser(userRecordIdentifier) != null) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("registerOidcUserCommand.errors.userAlreadyRegisteredWithToken"), this);
            }

            boolean provideMissingClaimsEnabled = FeatureFlags.API_BEARER_AUTH_PROVIDE_MISSING_CLAIMS.enabled();

            updateUserDTO(oAuth2UserRecord, provideMissingClaimsEnabled);

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

    private void updateUserDTO(OAuth2UserRecord oAuth2UserRecord, boolean provideMissingClaimsEnabled) throws InvalidFieldsCommandException {
        if (provideMissingClaimsEnabled) {
            Map<String, String> fieldErrors = validateConflictingClaims(oAuth2UserRecord);
            throwInvalidFieldsCommandExceptionIfErrorsExist(fieldErrors);
            updateUserDTOWithClaims(oAuth2UserRecord);
        } else {
            Map<String, String> fieldErrors = validateUserDTOHasNoClaims();
            throwInvalidFieldsCommandExceptionIfErrorsExist(fieldErrors);
            overwriteUserDTOWithClaims(oAuth2UserRecord);
        }
    }

    private Map<String, String> validateConflictingClaims(OAuth2UserRecord oAuth2UserRecord) {
        Map<String, String> fieldErrors = new HashMap<>();

        addFieldErrorIfConflict(FIELD_USERNAME, oAuth2UserRecord.getUsername(), userDTO.getUsername(), fieldErrors);
        addFieldErrorIfConflict(FIELD_FIRST_NAME, oAuth2UserRecord.getDisplayInfo().getFirstName(), userDTO.getFirstName(), fieldErrors);
        addFieldErrorIfConflict(FIELD_LAST_NAME, oAuth2UserRecord.getDisplayInfo().getLastName(), userDTO.getLastName(), fieldErrors);
        addFieldErrorIfConflict(FIELD_EMAIL_ADDRESS, oAuth2UserRecord.getDisplayInfo().getEmailAddress(), userDTO.getEmailAddress(), fieldErrors);

        return fieldErrors;
    }

    private void addFieldErrorIfConflict(String fieldName, String claimValue, String existingValue, Map<String, String> fieldErrors) {
        if (claimValue != null && !claimValue.trim().isEmpty() && existingValue != null && !claimValue.equals(existingValue)) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsEnabled.fieldAlreadyPresentInProvider",
                    List.of(fieldName)
            );
            fieldErrors.put(fieldName, errorMessage);
        }
    }

    private Map<String, String> validateUserDTOHasNoClaims() {
        Map<String, String> fieldErrors = new HashMap<>();
        if (userDTO.getUsername() != null) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON",
                    List.of(FIELD_USERNAME)
            );
            fieldErrors.put(FIELD_USERNAME, errorMessage);
        }
        if (userDTO.getEmailAddress() != null) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON",
                    List.of(FIELD_EMAIL_ADDRESS)
            );
            fieldErrors.put(FIELD_EMAIL_ADDRESS, errorMessage);
        }
        if (userDTO.getFirstName() != null) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON",
                    List.of(FIELD_FIRST_NAME)
            );
            fieldErrors.put(FIELD_FIRST_NAME, errorMessage);
        }
        if (userDTO.getLastName() != null) {
            String errorMessage = BundleUtil.getStringFromBundle(
                    "registerOidcUserCommand.errors.provideMissingClaimsDisabled.unableToSetFieldViaJSON",
                    List.of(FIELD_LAST_NAME)
            );
            fieldErrors.put(FIELD_LAST_NAME, errorMessage);
        }
        return fieldErrors;
    }

    private void updateUserDTOWithClaims(OAuth2UserRecord oAuth2UserRecord) {
        userDTO.setUsername(getValueOrDefault(oAuth2UserRecord.getUsername(), userDTO.getUsername()));
        userDTO.setFirstName(getValueOrDefault(oAuth2UserRecord.getDisplayInfo().getFirstName(), userDTO.getFirstName()));
        userDTO.setLastName(getValueOrDefault(oAuth2UserRecord.getDisplayInfo().getLastName(), userDTO.getLastName()));
        userDTO.setEmailAddress(getValueOrDefault(oAuth2UserRecord.getDisplayInfo().getEmailAddress(), userDTO.getEmailAddress()));
    }

    private void overwriteUserDTOWithClaims(OAuth2UserRecord oAuth2UserRecord) {
        userDTO.setUsername(oAuth2UserRecord.getUsername());
        userDTO.setFirstName(oAuth2UserRecord.getDisplayInfo().getFirstName());
        userDTO.setLastName(oAuth2UserRecord.getDisplayInfo().getLastName());
        userDTO.setEmailAddress(oAuth2UserRecord.getDisplayInfo().getEmailAddress());
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
        return (oidcValue == null || oidcValue.trim().isEmpty()) ? dtoValue : oidcValue;
    }

    private void validateUserFields(CommandContext ctxt, boolean provideMissingClaimsEnabled) throws InvalidFieldsCommandException {
        Map<String, String> fieldErrors = new HashMap<>();

        if (!FeatureFlags.API_BEARER_AUTH_HANDLE_TOS_ACCEPTANCE_IN_IDP.enabled()) {
            validateTermsAccepted(fieldErrors);
        }

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
