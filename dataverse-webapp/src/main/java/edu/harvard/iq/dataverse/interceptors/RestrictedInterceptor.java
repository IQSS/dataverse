package edu.harvard.iq.dataverse.interceptors;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.annotations.processors.permissions.PermissionDataProcessor;
import edu.harvard.iq.dataverse.annotations.processors.permissions.RestrictedObject;
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

    @EJB
    private PermissionServiceBean permissionService;

    @Inject
    private DataverseRequestServiceBean requestService;

    //-------------------- LOGIC --------------------

    @AroundInvoke
    public Object callRestricted(InvocationContext ctx) throws Exception {
        DataverseRequest request = requestService.getDataverseRequest();
        Method method = ctx.getMethod();
        Set<RestrictedObject> restrictedObjects = PermissionDataProcessor.gatherPermissionRequirements(method, ctx.getParameters());
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
                .map(r -> "[" + r.name + " : " + extractSafelyObjectNames(r) + "]")
                .collect(Collectors.joining(" "));
        String currentInfo = logRecord.getInfo() != null ? logRecord.getInfo() : StringUtils.EMPTY;
        logRecord.setInfo(currentInfo + " " + restrictedObjectsLog);
    }

    private static String extractSafelyObjectNames(RestrictedObject restrictedObject) {
        return restrictedObject.objects.stream()
                .map(RestrictedInterceptor::extractSafelyObjectName)
                .collect(Collectors.joining(","));
    }

    private static String extractSafelyObjectName(DvObject dvObject) {
        return dvObject != null ? dvObject.accept(DvObject.NamePrinter) : "<null>";
    }

    private List<MissingPermissions> checkPermissions(Set<RestrictedObject> restrictedObjects, DataverseRequest request) {
        List<MissingPermissions> missingPermissions = new ArrayList<>();

        return restrictedObjects.stream()
                .flatMap(r -> r.objects.stream()
                        .map(d -> new DvObjectWithUserPermissions(d, r, fetchGrantedPermissions(d, request))))
                .filter(this::isAnyPermissionMissing)
                .map(MissingPermissions::new)
                .collect(Collectors.toList());
    }

    private Set<Permission> fetchGrantedPermissions(DvObject dvObject, DataverseRequest request) {
        return dvObject != null
                ? permissionService.permissionsFor(request, dvObject)
                : EnumSet.allOf(Permission.class);
    }

    private boolean isAnyPermissionMissing(DvObjectWithUserPermissions toCheck) {
        return (!toCheck.allRequired && !CollectionUtils.containsAny(toCheck.granted, toCheck.required))
                || (toCheck.allRequired && !toCheck.granted.containsAll(toCheck.required));
    }

    // -------------------- INNER CLASSES --------------------

    private static class DvObjectWithUserPermissions {
        public final DvObject dvObject;
        public final Set<Permission> required;
        public final Set<Permission> granted;
        public final boolean allRequired;

        public DvObjectWithUserPermissions(DvObject dvObject, RestrictedObject restrictedObject, Set<Permission> granted) {
            this.dvObject = dvObject;
            this.required = restrictedObject.permissions;
            this.granted = granted;
            this.allRequired = restrictedObject.allRequired;
        }
    }

    private static class MissingPermissions {
        public final DvObject dvObject;
        public final String dvObjectName;
        public final Set<Permission> required;
        public final Set<Permission> missing;

        public MissingPermissions(DvObjectWithUserPermissions source) {
            this.dvObject = source.dvObject;
            this.dvObjectName = extractSafelyObjectName(source.dvObject);
            this.required = source.required;
            this.missing = SetUtils.difference(source.required, source.granted);
        }

        @Override
        public String toString() {
            return "[Dataverse object:\t" + dvObjectName + ", missing permissions:\t" + missing + "]";
        }
    }
}
