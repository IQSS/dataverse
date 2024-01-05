package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swordapp.server.StatementAPI;
import org.swordapp.server.StatementManager;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2StatementServlet extends SwordServlet {

    @Inject
    StatementManagerImpl statementManagerImpl;
    private StatementManager sm;
    private StatementAPI statementApi;

    @Override
    public void init() throws ServletException {
        super.init();

        // load the container manager implementation
        this.sm = statementManagerImpl;

        // initialise the underlying servlet processor
        this.statementApi = new StatementAPI(this.sm, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.statementApi.get(req, resp);
    }

}
