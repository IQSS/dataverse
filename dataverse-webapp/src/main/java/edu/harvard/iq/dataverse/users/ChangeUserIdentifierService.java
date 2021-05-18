package edu.harvard.iq.dataverse.users;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

@Stateless
public class ChangeUserIdentifierService {

    @EJB
    private AuthenticationServiceBean authenticationService;

    @EJB
    private BuiltinUserServiceBean builtinUserService;

    @EJB
    private RoleAssigneeServiceBean roleAssigneeService;

    // -------------------- LOGIC --------------------

    @LoggedCall
    @SuperuserRequired
    public void changeUserIdentifier(String oldIdentifier, String newIdentifier)
        throws IllegalStateException, IllegalArgumentException {

        if(oldIdentifier == null || oldIdentifier.isEmpty()) {
            throw new IllegalArgumentException("Old identifier provided to change is empty.");
        } else if(newIdentifier == null || newIdentifier.isEmpty()) {
            throw new IllegalArgumentException("New identifier provided to change is empty.");
        } else if(newIdentifier.equals(oldIdentifier)) {
            throw new IllegalArgumentException("New identifier must differ from the old.");
        }

        AuthenticatedUser authenticatedUser = authenticationService.getAuthenticatedUser(oldIdentifier);
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("User " + oldIdentifier + " not found in AuthenticatedUser");
        }

        AuthenticatedUser authenticatedUserTestNewIdentifier = authenticationService.getAuthenticatedUser(newIdentifier);
        if (authenticatedUserTestNewIdentifier != null) {
            String logMsg = " User " + newIdentifier + " already exists. Cannot use this as new identifier";
            throw new IllegalArgumentException("Validation of submitted data failed. Details: " + logMsg);
        }

        BuiltinUser builtinUser = builtinUserService.findByUserName(oldIdentifier);

        if (builtinUser != null) {
            builtinUser.setUserName(newIdentifier);
            //Validate the BuiltinUser change. Username validations are there.
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<BuiltinUser>> violations = validator.validate(builtinUser);
            if (violations.size() > 0) {
                StringBuilder logMsg = new StringBuilder();
                for (ConstraintViolation<?> violation : violations) {
                    logMsg.append(" Invalid value: >>>")
                            .append(violation.getInvalidValue())
                            .append("<<< for ")
                            .append(violation.getPropertyPath())
                            .append(" at ")
                            .append(violation.getLeafBean())
                            .append(" - ")
                            .append(violation.getMessage());
                }
                throw new IllegalStateException("Validation of submitted data failed. Details: " + logMsg);
            }
        }

        AuthenticatedUserLookup authenticatedUserLookup = authenticatedUser.getAuthenticatedUserLookup();
        authenticatedUserLookup.setPersistentUserId(newIdentifier);

        List<RoleAssignment> roleAssignments = roleAssigneeService.getAssignmentsFor(authenticatedUser.getIdentifier()); //only AuthenticatedUser supported
        for(RoleAssignment roleAssignment : roleAssignments) {
            roleAssignment.setAssigneeIdentifier("@" + newIdentifier);
        }

        authenticatedUser.setUserIdentifier(newIdentifier);
    }

}
