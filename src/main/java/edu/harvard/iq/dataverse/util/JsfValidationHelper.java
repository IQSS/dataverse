package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.validation.ValidationException;

import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.google.common.collect.Sets.newHashSet;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;

/**
 * Translate validation exception to message at given clientId field.
 * @author tjanek
 */
public final class JsfValidationHelper {

    private static final Logger logger = Logger.getLogger(JsfValidationHelper.class.getCanonicalName());

    public static String execute(Supplier<String> action, ValidationCondition...conditions) {
        try {
            logger.info("Executing action with validation translator");
            return action.get();
        } catch (EJBException ex) {
            if (isValidationException(ex)) {
                logger.warning("Validation exception occured: " + ex.getCause());
                ValidationCondition condition = null;
                Set<ValidationCondition> validationConditions = newHashSet(conditions);
                for (ValidationCondition vc : validationConditions) {
                    if (ex.getCause().getClass().getCanonicalName().equals(vc.exception.getCanonicalName())) {
                        condition = vc;
                    }
                }
                if (condition != null) {
                    logger.info("Find validation exception translate for: " + condition);
                    FacesContext.getCurrentInstance().addMessage(condition.clientId,
                            new FacesMessage(SEVERITY_ERROR, null,
                                    BundleUtil.getStringFromBundle(condition.message)));
                    return null;
                }
            }
            return unknownValidation();
        } catch (Exception e) {
            return unknownValidation();
        }
    }

    private static boolean isValidationException(EJBException ex) {
        return ex.getCause() instanceof ValidationException;
    }

    private static String unknownValidation() {
        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("error.general.message"));
        return null;
    }

    public static class ValidationCondition {
        private final Class<? extends ValidationException> exception;
        private final String clientId;
        private final String message;

        private ValidationCondition(Class<? extends ValidationException> exception, String clientId, String message) {
            this.exception = exception;
            this.clientId = clientId;
            this.message = message;
        }

        public static ValidationCondition on(Class<? extends ValidationException> exception, String clientId, String message) {
            return new ValidationCondition(exception, clientId, message);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationCondition condition = (ValidationCondition) o;
            return Objects.equals(exception, condition.exception) &&
                    Objects.equals(clientId, condition.clientId) &&
                    Objects.equals(message, condition.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exception, clientId, message);
        }

        @Override
        public String toString() {
            return "ValidationCondition{" +
                    "exception=" + exception +
                    ", clientId='" + clientId + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
