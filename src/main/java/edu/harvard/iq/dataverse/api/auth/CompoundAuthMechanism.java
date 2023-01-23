package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompoundAuthMechanism implements AuthMechanism {

    protected List<AuthMechanism> authMechanisms = new ArrayList<>();

    public CompoundAuthMechanism(AuthMechanism... authMechanisms) {
        add(authMechanisms);
    }

    public void add(AuthMechanism... authMechanisms) {
        this.authMechanisms.addAll(Arrays.asList(authMechanisms));
    }

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        User user = null;
        for (AuthMechanism authMechanism : authMechanisms) {
            User userFromRequest = authMechanism.findUserFromRequest(containerRequestContext);
            if (userFromRequest != null) {
                user = userFromRequest;
                break;
            }
        }
        if (user == null) {
            user = GuestUser.get();
        }
        return user;
    }
}
