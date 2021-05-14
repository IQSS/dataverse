package edu.harvard.iq.dataverse.interceptors;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationException;
import edu.harvard.iq.dataverse.persistence.user.User;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@SuperuserRequired
public class SuperuserRequiredInterceptor {
    @Inject
    private DataverseSession session;

    // -------------------- LOGIC --------------------

    @AroundInvoke
    public Object checkIfSuperuser(InvocationContext ctx) throws Exception {
        User user = session.getUser();
        if (user.isSuperuser()) {
            return ctx.proceed();
        } else {
            throw new AuthorizationException(
                    "User is not authorized to call this method. Only superuser is allowed to do it.");
        }
    }
}
