package edu.harvard.iq.dataverse.authorization.providers.saml;

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.logout.LogoutRequest;
import com.onelogin.saml2.logout.LogoutResponse;
import com.onelogin.saml2.servlet.ServletUtils;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.util.Constants;
import com.onelogin.saml2.util.Util;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.users.SamlSessionRegistry;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamlAuthenticationServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SamlAuthenticationServlet.class);

    private static final String SAML_DISABLED = "SAML authentication is currently disabled.";

    public static final String NEW_USER_SESSION_PARAM = "NewUser";

    private AuthenticationServiceBean authenticationService;
    private DataverseSession session;
    private SystemConfig systemConfig;
    private SamlConfigurationService samlConfigurationService;
    private SamlSessionRegistry samlSessionRegistry;

    // -------------------- CONSTURCTORS --------------------

    @Inject
    public SamlAuthenticationServlet(AuthenticationServiceBean authenticationService, DataverseSession session,
                                     SystemConfig systemConfig, SamlConfigurationService samlConfigurationService,
                                     SamlSessionRegistry samlSessionRegistry) {
        this.authenticationService = authenticationService;
        this.session = session;
        this.systemConfig = systemConfig;
        this.samlConfigurationService = samlConfigurationService;
        this.samlSessionRegistry = samlSessionRegistry;
    }

    // -------------------- LOGIC --------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!samlConfigurationService.isSamlLoginEnabled()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, SAML_DISABLED);
            return;
        }
        String path = request.getPathInfo();
        switch (path) {
            case "/metadata":
                metadata(response);
                break;
            case "/sls":
                logout(request, response);
                break;
            default:
                super.doGet(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!samlConfigurationService.isSamlLoginEnabled()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, SAML_DISABLED);
            return;
        }
        String path = request.getPathInfo();
        switch (path) {
            case "/acs":
                login(request, response);
                break;
            case "/sls":
                logout(request, response);
                break;
            default:
                super.doPost(request, response);
        }
    }

    // -------------------- PRIVATE --------------------

    private void metadata(HttpServletResponse response) throws IOException {
        Saml2Settings settings = samlConfigurationService.buildSpSettings();
        List<String> errors = settings.checkSPSettings();
        if (errors.isEmpty()) {
            try (ServletOutputStream out = response.getOutputStream();
                 final OutputStreamWriter writer = new OutputStreamWriter(out)) {
                writer.write(settings.getSPMetadata());
            } catch (CertificateEncodingException cee) {
                logger.warn("There was an issue while producing metadata", cee);
            }
        } else {
            logger.warn("Errors while getting metadata for Dataverse SP: " + errors);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "");
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) {
        try {
            SamlResponse samlResponse =
                    new SamlResponse(samlConfigurationService.buildSpSettings(), ServletUtils.makeHttpRequest(request));
            String entityId = samlResponse.getResponseIssuer();

            Auth auth = new Auth(samlConfigurationService.buildSettings(entityId), request, response);
            auth.processResponse();
            if (auth.isAuthenticated()) {
                SamlUserData userData = SamlUserDataFactory.create(auth);
                ExternalIdpUserRecord userRecord = userData.toExternalIdpUserRecord();
                UserRecordIdentifier userRecordId = userRecord.toUserRecordIdentifier();
                AuthenticatedUser user = authenticationService.lookupUser(userRecordId);
                if (user != null) {
                    session.setUser(user);
                    samlSessionRegistry.register(session);
                    user.setSamlIdPEntityId(userData.getIdpEntityId());
                    String relayState = request.getParameter("RelayState");
                    response.sendRedirect(StringUtils.isNotBlank(relayState)
                            ? relayState
                            : systemConfig.getDataverseSiteUrl());
                } else {
                    request.getSession().setAttribute(NEW_USER_SESSION_PARAM, userRecord);
                    response.sendRedirect("/firstLogin.xhtml");
                }
            }
        } catch (Exception e) {
            logger.warn("SAML Authentication exception: ", e);
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpRequest httpRequest = ServletUtils.makeHttpRequest(request);

            String samlRequestParameter = httpRequest.getParameter("SAMLRequest");
            if (StringUtils.isBlank(samlRequestParameter)) {
                throw new RuntimeException("Invalid logout request");
            }
            Document requestXml = Util.loadXML(decode(samlRequestParameter));
            String issuer = LogoutRequest.getIssuer(requestXml);
            Saml2Settings settings = samlConfigurationService.buildSettings(issuer);
            String nameId = LogoutRequest.getNameId(requestXml, settings.getSPkey());

            LogoutRequest logoutRequest = new LogoutRequest(settings, httpRequest);
            if (!logoutRequest.isValid()) {
                throw new RuntimeException("Invalid logout request:\t" + logoutRequest.getError(),
                        logoutRequest.getValidationException());
            }

            AuthenticatedUser user = findUserForLogout(issuer, nameId);
            if (user == null) {
                throw new RuntimeException("User [" + nameId + "] was not found");
            }

            clearActiveSessionsForUser(user);
            Map<String, String> parameters = buildParamsWithLogoutResponse(settings, httpRequest, logoutRequest);
            request.getSession().invalidate();
            ServletUtils.sendRedirect(response, settings.getIdpSingleLogoutServiceResponseUrl().toString(), parameters, false);
        } catch (Exception e) {
            logger.warn("SAML logout exception: ", e);
        }
    }

    private String decode(String requestParam) {
        String decoded = Util.base64decodedInflated(requestParam);
        if (isProperlyDecoded(decoded)) {
            return decoded;
        }
        decoded = new String(Util.base64decoder(requestParam), StandardCharsets.UTF_8);
        if (!isProperlyDecoded(decoded)) {
            throw new IllegalArgumentException("There was a problem with request decoding.");
        }
        return decoded;
    }

    private boolean isProperlyDecoded(String decoded) {
        return decoded != null && decoded.trim().startsWith("<");
    }

    private AuthenticatedUser findUserForLogout(String issuer, String nameId) {
        UserRecordIdentifier userRecordIdentifier = new SamlUserData(nameId, issuer)
                .toExternalIdpUserRecord()
                .toUserRecordIdentifier();
        return authenticationService.lookupUser(userRecordIdentifier);
    }

    private void clearActiveSessionsForUser(AuthenticatedUser user) {
        List<DataverseSession> sessions = samlSessionRegistry.unregister(user);
        for (DataverseSession session : sessions) {
            session.setUser(null);
        }
    }

    private Map<String, String> buildParamsWithLogoutResponse(
            Saml2Settings settings, HttpRequest httpRequest, LogoutRequest logoutRequest) throws IOException, SettingsException {
        LogoutResponse logoutResponseBuilder = new LogoutResponse(settings, httpRequest);
        logoutResponseBuilder.build(logoutRequest.getId(), Constants.STATUS_SUCCESS);
        String samlLogoutResponse = logoutResponseBuilder.getEncodedLogoutResponse();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("SAMLResponse", samlLogoutResponse);

        String relayState = httpRequest.getParameter("RelayState");
        if (relayState != null) {
            parameters.put("RelayState", relayState);
        }

        if (settings.getLogoutResponseSigned()) {
            String sigAlg = settings.getSignatureAlgorithm();
            String signature = new Auth(settings, null, null)
                    .buildResponseSignature(samlLogoutResponse, relayState, sigAlg);
            parameters.put("SigAlg", sigAlg);
            parameters.put("Signature", signature);
        }
        return parameters;
    }
}
