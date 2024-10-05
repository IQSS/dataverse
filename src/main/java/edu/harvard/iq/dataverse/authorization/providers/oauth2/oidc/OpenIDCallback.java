package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.io.IOException;

import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
