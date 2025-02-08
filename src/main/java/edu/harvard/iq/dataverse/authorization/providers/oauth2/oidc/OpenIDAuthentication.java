package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.io.IOException;

import edu.harvard.iq.dataverse.util.SystemConfig;
import fish.payara.security.annotations.LogoutDefinition;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import fish.payara.security.openid.api.OpenIdConstant;
import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
* This code is a part of an OpenID Connect solutions using Jakarta security annotations.
* The main building blocks are:
* - @OpenIdAuthenticationDefinition added on the authentication HttpServlet edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OpenIDAuthentication, see https://docs.payara.fish/enterprise/docs/Technical%20Documentation/Public%20API/OpenID%20Connect%20Support.html
* - IdentityStoreHandler and HttpAuthenticationMechanism, as provided on the server (no custom implementation involved here), see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-auth
* - SecurityContext injected in AbstractAPIBean to handle authentication, see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-context
*/

/**
 * If we want to use the OIDC annotation, it assumes we also use the security annotations, HttpAuthenticationMechanism, IdentityStoreHandler and the IdentityStore (which we do not use...) and that the endpoints security relies on SecurityContext.
 * If I do not add the security annotation (@ServletSecurity(@HttpConstraint(rolesAllowed = "nobodyHasAccess"))) to this authentication servlet, it will not be secured and will not force you to log in, you will just see its content, OIDC annotation has no effect in that situation.
 * The authentication servlet is the only location that is really secured with OIDC and security annotations, SecurityContext, IdentityStoreHandler, HttpAuthenticationMechanism and the bearer token IdentityStore. But we do not use it anywhere else (and I assume we do not want to), so this servlet has content that is unreachable (because we do not use security annotations anywhere else in authentication mechanisms we implemented, so you cannot gain access to it by any means).
 * You can only make calls to the API with the bearer tokens in this implementation because the bearer token IdentityStore passes the user to the AbstractApiBean, so it can just rely on the "standard" Dataverse implementation, without any security annotations. The authentication servlet is there only to force the use of the OIDC annotation to do its work, so it is not important that it is not reachable. After logging in you are redirected somewhere else anyway.
 */

/**
 * OIDC login implementation
 */
@WebServlet("/oidc/login")
@OpenIdAuthenticationDefinition(
    providerURI = "#{openIdConfigBean.providerURI}",
    clientId = "#{openIdConfigBean.clientId}",
    clientSecret = "#{openIdConfigBean.clientSecret}",
    redirectURI = "#{openIdConfigBean.redirectURI}",
    logout = @LogoutDefinition(redirectURI = "#{openIdConfigBean.logoutURI}"),
    scope = {OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE},
    useSession = true // If enabled state & nonce value stored in session otherwise in cookies.
    // I do not know if useSession should default to true. I made it explicit because of the comments about the Payara jsession being insecure and that we do not want to use cookies. It looks like these requirements would make this implementation unusable: either jsession cookie or token cookie is used. We might end up not using this code at all.
)
@DeclareRoles("nobodyHasAccess")
@ServletSecurity(@HttpConstraint(rolesAllowed = "nobodyHasAccess"))
public class OpenIDAuthentication extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // as explained above, this content is unreachable
        final String baseURL = SystemConfig.getDataverseSiteUrlStatic();
        final String target = request.getParameter("target");
        final String redirect = "SPA".equals(target) ? baseURL + "/spa/" : baseURL;
        response.sendRedirect(redirect);
    }
}
