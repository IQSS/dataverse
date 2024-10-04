package edu.harvard.iq.dataverse.api;

import java.io.IOException;

import fish.payara.security.annotations.LogoutDefinition;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import fish.payara.security.openid.api.OpenIdConstant;
import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OIDC login implementation
 */
@WebServlet("/oidc/login")
@OpenIdAuthenticationDefinition(
    providerURI = "#{openIdConfigBean.providerURI}",
    clientId = "#{openIdConfigBean.clientId}",
    clientSecret = "#{openIdConfigBean.clientSecret}",
    redirectURI = "#{openIdConfigBean.redirectURI}",
    scope = {OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE},
    logout = @LogoutDefinition(redirectURI = "#{openIdConfigBean.logoutURI}")
)
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class OpenIDAuthentication extends HttpServlet {
    @EJB
    OpenIDConfigBean openIdConfigBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("This content is unreachable as the required role is not assigned to anyone, therefore, this content should never become visible in a browser");
    }
}
