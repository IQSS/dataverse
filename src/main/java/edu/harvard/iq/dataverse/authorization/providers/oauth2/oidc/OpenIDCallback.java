package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.io.IOException;

import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.servlet.ServletException;
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
 * I am not sure if we need the redirects implemented here, this entire code might be removed in the future. I think it depends if we want to allow cookies or not. If we assume that nobody wants to use httponly secure cookies as being OK for security and always implement local storage/in memory tokens, like many systems do, then we can delete this routing thing. I just like the simplicity of it. You cannot turn the cookies off anyway if you want to use the OIDC annotation on payara, so we can have some convenience code as well. I see it more like that: either we happily use what is possible on Payara/Jakarta, or we implement everything ourselves, like it was the case until now. If we do use the OIDC annotation, we might as well use the convenience of it and not hide it under a rug for some hacker to figure it out anyway.
 */

@WebServlet("/oidc/callback/*")
public class OpenIDCallback extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final String baseURL = SystemConfig.getDataverseSiteUrlStatic();
        final String target = request.getPathInfo();
        final String redirect;
        switch (target) {
            case "/JSF":
                redirect = baseURL + "/oauth2/callback.xhtml";
                break;
            case "/SPA":
                redirect = baseURL + "/spa/";
                break;
            case "/API":
                redirect = baseURL + "/api/v1/oidc/session";
                break;

            default:
                redirect = baseURL + "/oauth2/callback.xhtml";
                break;
        }
        response.sendRedirect(redirect);
    }
}
