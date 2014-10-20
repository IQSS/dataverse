package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.abdera.parser.ParseException;
import org.swordapp.server.CollectionAPI;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2CollectionServlet extends SwordServlet {

    @Inject
    CollectionDepositManagerImpl collectionDepositManagerImpl;
    @Inject
    CollectionListManagerImpl collectionListManagerImpl;
    protected CollectionAPI api;

    public void init() throws ServletException {
        super.init();
        this.api = new CollectionAPI(collectionListManagerImpl, collectionDepositManagerImpl, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.api.get(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            this.api.post(req, resp);
        } catch (ParseException ex) {
            /**
             * @todo close https://github.com/IQSS/dataverse/issues/893 if/when
             * https://github.com/swordapp/JavaServer2.0/issues/6 is closed
             */
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Attempt to upload an empty Atom entry? org.apache.abdera.parser.ParseException caught. See also https://github.com/IQSS/dataverse/issues/893 and https://github.com/swordapp/JavaServer2.0/issues/6");
        }
    }

}
