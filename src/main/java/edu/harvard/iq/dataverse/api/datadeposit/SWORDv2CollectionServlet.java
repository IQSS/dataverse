package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swordapp.server.CollectionAPI;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2CollectionServlet extends SwordServlet {

    @Inject
    CollectionDepositManagerImpl collectionDepositManagerImpl;
    @Inject
    CollectionListManagerImpl collectionListManagerImpl;
    protected CollectionAPI api;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void init() throws ServletException {
        super.init();
        this.api = new CollectionAPI(collectionListManagerImpl, collectionDepositManagerImpl, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            setRequest(req);
            this.api.get(req, resp);
            setRequest(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            setRequest(req);
            this.api.post(req, resp);
            setRequest(null);
        } finally {
            lock.unlock();
        }
    }

    private void setRequest(HttpServletRequest r) {
        collectionDepositManagerImpl.setRequest(r);
        collectionListManagerImpl.setRequest(r);
    }
}
