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
    public static final String FIELD_ORCID = "orcid";

    private final UserDTO userDTO;
    private final String password;
    private final String orcidUrl;

    public SignUpBuiltinUserCommand(DataverseRequest aRequest, UserDTO userDTO, String password) {
        this(aRequest, userDTO, password, null);
    }

    public SignUpBuiltinUserCommand(DataverseRequest aRequest, UserDTO userDTO, String password, String orcidUrl) {
        super(aRequest, (DvObject) null);
        this.userDTO = userDTO;
        this.password = password;
        this.orcidUrl = orcidUrl;
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

        if (orcidUrl != null) {
            authenticatedUser.setAuthenticatedOrcid(orcidUrl);
            authenticatedUser = ctxt.em().merge(authenticatedUser);
        }

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

        putIfError(fieldErrors, FIELD_USERNAME, validateUsername(ctxt, userDTO.getUsername()));

        if (isBlank(userDTO.getFirstName())) {
            fieldErrors.put(FIELD_FIRST_NAME, requiredMessage(FIELD_FIRST_NAME));
        }
        if (isBlank(userDTO.getLastName())) {
            fieldErrors.put(FIELD_LAST_NAME, requiredMessage(FIELD_LAST_NAME));
        }

        putIfError(fieldErrors, FIELD_EMAIL_ADDRESS, validateEmail(ctxt, userDTO.getEmailAddress()));
        putIfError(fieldErrors, FIELD_PASSWORD, validatePassword(ctxt));

        if (orcidUrl != null && ctxt.authentication().lookupUserByOrcid(orcidUrl) != null) {
            fieldErrors.put(FIELD_ORCID, BundleUtil.getStringFromBundle("signUpBuiltinUserCommand.errors.orcidInUse"));
        }

        if (!userDTO.isTermsAccepted()) {
            fieldErrors.put(FIELD_TERMS_ACCEPTED, BundleUtil.getStringFromBundle("user.acccountterms.required"));
        }

        return fieldErrors;
    }

    private static String validateUsername(CommandContext ctxt, String username) {
        if (isBlank(username)) {
            return requiredMessage(FIELD_USERNAME);
        }
        if (!UserNameValidator.isUserNameValid(username)) {
            return BundleUtil.getStringFromBundle("user.username.invalid");
        }
        if (ctxt.authentication().identifierExists(username) || ctxt.builtinUsers().findByUserName(username) != null) {
            return BundleUtil.getStringFromBundle("user.username.taken");
        }
        return null;
    }

    private static String validateEmail(CommandContext ctxt, String email) {
        if (isBlank(email)) {
            return requiredMessage(FIELD_EMAIL_ADDRESS);
        }
        if (!EMailValidator.isEmailValid(email)) {
            return BundleUtil.getStringFromBundle("oauth2.newAccount.emailInvalid");
        }
        if (ctxt.authentication().getAuthenticatedUserByEmail(email) != null) {
            return BundleUtil.getStringFromBundle("user.email.taken");
        }
        return null;
    }

    private String validatePassword(CommandContext ctxt) {
        if (isBlank(password)) {
            return requiredMessage(FIELD_PASSWORD);
        }
        List<String> passwordErrors = ctxt.passwordValidation().validate(password, new Date(), false);
        return passwordErrors.isEmpty() ? null : String.join(" ", passwordErrors);
    }

    private static void putIfError(Map<String, String> fieldErrors, String field, String error) {
        if (error != null) {
            fieldErrors.put(field, error);
        }
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
