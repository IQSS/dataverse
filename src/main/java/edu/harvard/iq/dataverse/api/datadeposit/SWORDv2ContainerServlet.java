package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.abdera.parser.ParseException;
import org.swordapp.server.ContainerAPI;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.StatementManager;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2ContainerServlet extends SwordServlet {

    @Inject
    ContainerManagerImpl containerManagerImpl;
    @Inject
    StatementManagerImpl statementManagerImpl;
    private ContainerManager cm;
    private ContainerAPI api;
    private StatementManager sm;

    public void init() throws ServletException {
        super.init();

        // load the container manager implementation
        this.cm = containerManagerImpl;

        // load the statement manager implementation
        this.sm = statementManagerImpl;

        // initialise the underlying servlet processor
        this.api = new ContainerAPI(this.cm, this.sm, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.api.get(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.api.head(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            this.api.put(req, resp);
        } catch (ParseException ex) {
            /**
             * @todo close https://github.com/IQSS/dataverse/issues/893 if/when
             * https://github.com/swordapp/JavaServer2.0/issues/6 is closed
             */
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Attempt to upload an empty Atom entry? org.apache.abdera.parser.ParseException caught. See also https://github.com/IQSS/dataverse/issues/893 and https://github.com/swordapp/JavaServer2.0/issues/6");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.api.post(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.api.delete(req, resp);
    }

}
