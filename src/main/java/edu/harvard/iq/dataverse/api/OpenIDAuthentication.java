package edu.harvard.iq.dataverse.api;

import java.io.IOException;

import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/oidc/login")
@OpenIdAuthenticationDefinition(
    providerURI="#{openIdConfigBean.providerURI}",
    clientId="#{openIdConfigBean.clientId}",
    clientSecret="#{openIdConfigBean.clientSecret}",
    redirectURI="#{openIdConfigBean.redirectURI}",
    scope="email"
)
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class OpenIDAuthentication extends HttpServlet {
    @EJB
    OpenIDConfigBean openIdConfigBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("...");
    }
}
