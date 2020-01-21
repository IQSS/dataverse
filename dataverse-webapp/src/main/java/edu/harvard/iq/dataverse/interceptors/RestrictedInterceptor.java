package edu.harvard.iq.dataverse.interceptors;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.annotations.processors.PermissionDataProcessor;
import edu.harvard.iq.dataverse.annotations.processors.RestrictedObject;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.interceptors.InterceptorCommons.createName;

@Interceptor
@Restricted
public class RestrictedInterceptor {
    private static final Logger logger = Logger.getLogger(RestrictedInterceptor.class.getSimpleName());

    private PermissionDataProcessor permissionDataProcessor = new PermissionDataProcessor();

    @EJB
    private PermissionServiceBean permissionService;

    @Inject
    private DataverseRequestServiceBean requestService;

    //-------------------- LOGIC --------------------

    @AroundInvoke
    public Object callRestricted(InvocationContext ctx) throws Exception {
        DataverseRequest request = requestService.getDataverseRequest();
        Method method = ctx.getMethod();
        Set<RestrictedObject> restrictedObjects = permissionDataProcessor.gatherPermissionRequirements(method, ctx.getParameters());
        if (restrictedObjects.isEmpty()) {
            throw new RuntimeException("Service method: " + createName(method) + " does not define required permissions.");
        }

        logBasicInfo(ctx, restrictedObjects);
        List<MissingPermissions> missingPermissions = checkPermissions(restrictedObjects, request);
        if (!missingPermissions.isEmpty()) {
            throw createMissingPermissionException(missingPermissions, method, request);
        }

        try {
            return ctx.proceed();
        } catch (EJBException ee) {
            throw new CommandException("Service method: " + createName(method) + " failed: " + ee.getMessage(),
                    ee.getCausedByException(), null);
        }
    }

    // -------------------- PRIVATE ---------------------

    private PermissionException createMissingPermissionException(
            List<MissingPermissions> missingPermissions, Method method, DataverseRequest request) {
        MissingPermissions firstMissing = missingPermissions.get(0);
        return new PermissionException("Can't execute method " + method
                + ", because request: " + request
                + " is missing permissions: " + firstMissing.missing
                + " on Object: [" + firstMissing.dvObjectName
                + "]. All missing permissions: " + missingPermissions,
                null, firstMissing.required, firstMissing.dvObject);
    }

    private void logBasicInfo(InvocationContext ctx, Collection<RestrictedObject> restrictedObjects) {
        ActionLogRecord logRecord  = (ActionLogRecord) ctx.getContextData().get(InterceptorCommons.LOG_RECORD_KEY);
        if (logRecord == null) {
            logger.warning("Cannot find action log record in invocation context. Following data won't be saved " +
                    "into database: " + restrictedObjects);
            return;
        }
        String restrictedObjectsLog = restrictedObjects.stream()
                .map(r -> "[" + r.name + " : " + extractSafelyObjectName(r) + "]")
                .collect(Collectors.joining(" "));
        String currentInfo = logRecord.getInfo() != null ? logRecord.getInfo() : StringUtils.EMPTY;
        logRecord.setInfo(currentInfo + " " + restrictedObjectsLog);
    }

    private static String extractSafelyObjectName(RestrictedObject restrictedObject) {
        return restrictedObject.object != null
                ? restrictedObject.object.accept(DvObject.NamePrinter)
                : "<null>";
    }

    private List<MissingPermissions> checkPermissions(Set<RestrictedObject> restrictedObjects, DataverseRequest request) {
        List<MissingPermissions> missingPermissions = new ArrayList<>();

        for (RestrictedObject restrictedObject : restrictedObjects) {
            DvObject dvObject = restrictedObject.object;
            Set<Permission> granted = dvObject != null
                    ? permissionService.permissionsFor(request, dvObject)
                    : EnumSet.allOf(Permission.class);
            Set<Permission> required = restrictedObject.permissions;
            if (isAnyPermissionMissing(required, granted, restrictedObject.allRequired)) {
                missingPermissions.add(new MissingPermissions(restrictedObject, SetUtils.difference(required, granted)));
            }
        }

        return missingPermissions;
    }

    private boolean isAnyPermissionMissing(Set<Permission> required, Set<Permission> granted, boolean allRequired) {
        return (!allRequired && !CollectionUtils.containsAny(granted, required))
                || (allRequired && !granted.containsAll(required));
    }

    // -------------------- INNER CLASSES --------------------

    private static class MissingPermissions {
        public final DvObject dvObject;
        public final String dvObjectName;
        public final Set<Permission> required;
        public final Set<Permission> missing;

        public MissingPermissions(RestrictedObject restrictedObject, Set<Permission> missing) {
            this.dvObject = restrictedObject.object;
            this.dvObjectName = extractSafelyObjectName(restrictedObject);
            this.required = restrictedObject.permissions;
            this.missing = missing;
        }

        @Override
        public String toString() {
            return "[Dataverse object:\t" + dvObjectName + ", missing permissions:\t" + missing + "]";
        }
    }
}
