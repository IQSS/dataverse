package edu.harvard.iq.dataverse.interceptors;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord.ActionType;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord.Result;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.interceptors.InterceptorCommons.createName;

@Interceptor
@LoggedCall
public class LoggedCallInterceptor {
    private static final Logger logger = Logger.getLogger(LoggedCallInterceptor.class.getSimpleName());

    @Inject
    private ActionLogServiceBean logService;

    @Inject
    private DataverseRequestServiceBean requestService;

    @Resource
    EJBContext ejbContext;

    // -------------------- LOGIC --------------------

    @AroundInvoke
    public Object callRestricted(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        DataverseRequest request = requestService.getDataverseRequest();
        ActionLogRecord logRecord = new ActionLogRecord(ActionType.Command, createName(method))
                .setUserIdentifier(request.getUser().getIdentifier());
        ctx.getContextData().put(InterceptorCommons.LOG_RECORD_KEY, logRecord);
        try {
            return ctx.proceed();
        } catch(PermissionException pe) {
            logRecord.setActionResult(Result.PermissionError);
            addExceptionMessage(logRecord, pe::getMessage);
            throw pe;
        } catch (CommandException ce) {
            logRecord.setActionResult(Result.InternalError);
            addExceptionMessage(logRecord, ce::getMessage);
            throw ce;
        } catch (RuntimeException re) {
            logRecord.setActionResult(Result.InternalError);
            addExceptionMessage(logRecord, re::getMessage);
            logConstraintViolations(logRecord, re);
            throw re;
        } finally {
            if (logRecord.getActionResult() == null) {
                logRecord.setActionResult(Result.OK);
            } else {
                ejbContext.setRollbackOnly();
            }
            logRecord.setEndTime(new Date());
            logService.log(logRecord);
        }
    }

    // -------------------- PRIVATE --------------------

    private void logConstraintViolations(ActionLogRecord logRecord, RuntimeException re) {
        Throwable cause = re;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                logViolation(logRecord, (ConstraintViolationException) cause);
            }
            cause = cause.getCause();
        }
    }

    private void logViolation(ActionLogRecord logRecord, ConstraintViolationException constraintViolation) {
        StringBuilder violations = constraintViolation.getConstraintViolations()
                .stream()
                .collect(
                        () -> new StringBuilder("Unexpected bean validation constraint exception:"),
                        (sb, v) -> sb.append(" Invalid value: <<<").append(v.getInvalidValue())
                                        .append(">>> for ").append(v.getPropertyPath())
                                        .append(" at ").append(v.getLeafBean())
                                        .append(" - ").append(v.getMessage()),
                        StringBuilder::append
                );
        logger.log(Level.SEVERE, violations.toString());
        addExceptionMessage(logRecord, violations::toString);
    }

    private void addExceptionMessage(ActionLogRecord logRecord, Supplier<String> messageSupplier) {
        logRecord.setInfo(logRecord.getInfo() + " (" + messageSupplier.get() + ")");
    }
}
