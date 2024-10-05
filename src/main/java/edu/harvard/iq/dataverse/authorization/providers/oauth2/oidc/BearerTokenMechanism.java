package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import fish.payara.security.openid.api.AccessTokenCallerPrincipal;
import fish.payara.security.openid.api.BearerGroupsIdentityStore;
import fish.payara.security.openid.api.JwtClaims;
import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

@ApplicationScoped
@DeclareRoles({ "all" })
public class BearerTokenMechanism extends BearerGroupsIdentityStore {
    private static final Logger logger = Logger.getLogger(OIDCLoginBackingBean.class.getName());

    @Inject
    HttpServletRequest request;
    
    @EJB
    AuthenticationServiceBean authenticationSvc;

    @Override
    protected Set<String> getCallerGroups(AccessTokenCallerPrincipal callerPrincipal) {
        try {
            final JwtClaims claims = callerPrincipal.getClaims();
            final String issuer = claims.getIssuer().get();
            final String subject = claims.getSubject().get();
            final OIDCAuthProvider provider = authenticationSvc
                    .getAuthenticationProviderIdsOfType(OIDCAuthProvider.class).stream()
                    .map(providerId -> (OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(providerId))
                    .filter(providerCandidate -> issuer.equals(providerCandidate.getIssuerEndpointURL()))
                    .collect(Collectors.toUnmodifiableList()).get(0);
            final UserRecordIdentifier userRecordIdentifier = new UserRecordIdentifier(provider.getId(), subject);
            final AuthenticatedUser dvUser = authenticationSvc.lookupUser(userRecordIdentifier);
            request.setAttribute(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, dvUser);
            logger.log(Level.FINE, "user found: " + dvUser.toJson());
            return Set.of("all");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Getting user from bearer token failed: " + e.getMessage());
        }
        return Set.of();
    }
}