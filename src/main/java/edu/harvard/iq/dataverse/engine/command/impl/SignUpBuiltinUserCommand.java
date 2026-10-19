package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.api.dto.UserDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidFieldsCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import edu.harvard.iq.dataverse.validation.UserNameValidator;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredPermissions({})
public class SignUpBuiltinUserCommand extends AbstractCommand<AuthenticatedUser> {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_EMAIL_ADDRESS = "emailAddress";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_TERMS_ACCEPTED = "termsAccepted";

    private final UserDTO userDTO;
    private final String password;

    public SignUpBuiltinUserCommand(DataverseRequest aRequest, UserDTO userDTO, String password) {
        super(aRequest, (DvObject) null);
        this.userDTO = userDTO;
        this.password = password;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) throws CommandException {
        Map<String, String> fieldErrors = validate(ctxt);
        throwIfErrors(fieldErrors);

        String username = userDTO.getUsername();

        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName(username);
        builtinUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(password), PasswordEncryption.getLatestVersionNumber());

        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmailAddress(),
                userDTO.getAffiliation() != null ? userDTO.getAffiliation() : "",
                userDTO.getPosition() != null ? userDTO.getPosition() : ""
        );

        AuthenticatedUser authenticatedUser = ctxt.authentication().createAuthenticatedUser(
                new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, username),
                username,
                displayInfo,
                false
        );
        if (authenticatedUser == null) {
            throwIfErrors(Map.of(FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.taken")));
        }

        ctxt.builtinUsers().save(builtinUser);

        ctxt.notifications().sendNotification(
                authenticatedUser,
                new Timestamp(new Date().getTime()),
                UserNotification.Type.CREATEACC,
                null
        );

        return authenticatedUser;
    }

    private Map<String, String> validate(CommandContext ctxt) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String username = userDTO.getUsername();
        if (isBlank(username)) {
            fieldErrors.put(FIELD_USERNAME, requiredMessage(FIELD_USERNAME));
        } else if (!UserNameValidator.isUserNameValid(username)) {
            fieldErrors.put(FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.invalid"));
        } else if (ctxt.authentication().identifierExists(username) || ctxt.builtinUsers().findByUserName(username) != null) {
            fieldErrors.put(FIELD_USERNAME, BundleUtil.getStringFromBundle("user.username.taken"));
        }

        if (isBlank(userDTO.getFirstName())) {
            fieldErrors.put(FIELD_FIRST_NAME, requiredMessage(FIELD_FIRST_NAME));
        }
        if (isBlank(userDTO.getLastName())) {
            fieldErrors.put(FIELD_LAST_NAME, requiredMessage(FIELD_LAST_NAME));
        }

        String email = userDTO.getEmailAddress();
        if (isBlank(email)) {
            fieldErrors.put(FIELD_EMAIL_ADDRESS, requiredMessage(FIELD_EMAIL_ADDRESS));
        } else if (!EMailValidator.isEmailValid(email)) {
            fieldErrors.put(FIELD_EMAIL_ADDRESS, BundleUtil.getStringFromBundle("oauth2.newAccount.emailInvalid"));
        } else if (ctxt.authentication().getAuthenticatedUserByEmail(email) != null) {
            fieldErrors.put(FIELD_EMAIL_ADDRESS, BundleUtil.getStringFromBundle("user.email.taken"));
        }

        if (isBlank(password)) {
            fieldErrors.put(FIELD_PASSWORD, requiredMessage(FIELD_PASSWORD));
        } else {
            List<String> passwordErrors = ctxt.passwordValidation().validate(password, new Date(), false);
            if (!passwordErrors.isEmpty()) {
                fieldErrors.put(FIELD_PASSWORD, String.join(" ", passwordErrors));
            }
        }

        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put(FIELD_TERMS_ACCEPTED, BundleUtil.getStringFromBundle("user.acccountterms.required"));
        }

        return fieldErrors;
    }

    private void throwIfErrors(Map<String, String> fieldErrors) throws InvalidFieldsCommandException {
        if (!fieldErrors.isEmpty()) {
            throw new InvalidFieldsCommandException(
                    BundleUtil.getStringFromBundle("signUpBuiltinUserCommand.errors.invalidFields"),
                    this,
                    fieldErrors
            );
        }
    }

    private static String requiredMessage(String fieldName) {
        return BundleUtil.getStringFromBundle("signUpBuiltinUserCommand.errors.fieldRequired", List.of(fieldName));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
