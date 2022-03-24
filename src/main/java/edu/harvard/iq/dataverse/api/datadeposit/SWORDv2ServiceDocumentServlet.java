package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swordapp.server.ServiceDocumentAPI;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2ServiceDocumentServlet extends SwordServlet {

    @Inject
    ServiceDocumentManagerImpl serviceDocumentManagerImpl;
    protected ServiceDocumentAPI api;

    /**
     * @todo Should we inject this in all the SWORDv2 Servlets? Added here so
     * that we can inject SettingsServiceBean in SwordConfigurationImpl.
     */
    @Inject
    SwordConfigurationImpl swordConfigurationImpl;

    @Override
    public void init() throws ServletException {
        super.init();
        this.api = new ServiceDocumentAPI(serviceDocumentManagerImpl, swordConfigurationImpl);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.api.get(req, resp);
    }

}
