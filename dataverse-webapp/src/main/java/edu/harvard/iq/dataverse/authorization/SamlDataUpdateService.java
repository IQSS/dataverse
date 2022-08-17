package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.SamlLoginIssue.Type;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlUserData;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Either;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
public class SamlDataUpdateService {

    private ActionLogServiceBean actionLog;
    private AuthenticationServiceBean authenticationService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public SamlDataUpdateService() { }

    @Inject
    public SamlDataUpdateService(ActionLogServiceBean actionLog, AuthenticationServiceBean authenticationService) {
        this.actionLog = actionLog;
        this.authenticationService = authenticationService;
    }

    // -------------------- LOGIC --------------------

    /**
     * Updates user with data received from SAML IdP, if the received data is different from ours.
     */
    public Either<SamlLoginIssue, AuthenticatedUser> updateUserIfNeeded(AuthenticatedUser user, SamlUserData samlData) {
        if (!samlData.isCompleteForLogin()) {
            return Either.left(new SamlLoginIssue(Type.INCOMPLETE_DATA).addMessage(samlData.printLoginData()));
        }
        boolean updated = updateDifferingFields(user, samlData);
        if (!updated) {
            return Either.right(user);
        }
        Optional<SamlLoginIssue> updateIssueIssue = validate(user);
        if (updateIssueIssue.isPresent()) {
            return Either.left(updateIssueIssue.get());
        }
        AuthenticatedUser updatedUser = authenticationService.update(user);
        actionLog.log(new ActionLogRecord(ActionLogRecord.ActionType.Auth, "updateUser")
                .setInfo(user.getIdentifier()));
        return Either.right(updatedUser);
    }

    // -------------------- PRIVATE --------------------

    private boolean updateDifferingFields(AuthenticatedUser user, SamlUserData samlData) {
        boolean updated = false;
        if (!user.getFirstName().equals(samlData.getName())) {
            user.setFirstName(samlData.getName());
            updated = true;
        }
        if (!user.getLastName().equals(samlData.getSurname())) {
            user.setLastName(samlData.getSurname());
            updated = true;
        }
        if (!user.getEmail().equals(samlData.getEmail())) {
            String samlEmail = samlData.getEmail();
            user.setEmail(samlEmail);
            user.setEmailConfirmed(new Timestamp(new Date().getTime()));
            updated = true;
        }
        return updated;
    }

    private Optional<SamlLoginIssue> validate(AuthenticatedUser user) {
        if (!canUpdateEmail(user)) {
            return Optional.of(new SamlLoginIssue(Type.DUPLICATED_EMAIL));
        }

        Set<ConstraintViolation<AuthenticatedUser>> violations = Validation.buildDefaultValidatorFactory()
                .getValidator()
                .validate(user);
        if (!violations.isEmpty()) {
            return Optional.of(
                    new SamlLoginIssue(Type.INVALID_DATA)
                        .addMessages(violations.stream()
                                .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                                .collect(Collectors.toSet())));
        }

        return Optional.empty();
    }

    private boolean canUpdateEmail(AuthenticatedUser toUpdate) {
        String email = toUpdate.getEmail();
        AuthenticatedUser userFoundByMail = authenticationService.getAuthenticatedUserByEmail(email);
        return userFoundByMail == null || userFoundByMail.getIdentifier().equals(toUpdate.getIdentifier());
    }
}
