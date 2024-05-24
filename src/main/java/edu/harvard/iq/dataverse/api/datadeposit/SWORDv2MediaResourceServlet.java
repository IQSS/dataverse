package edu.harvard.iq.dataverse.api.datadeposit;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swordapp.server.MediaResourceAPI;
import org.swordapp.server.servlets.SwordServlet;

public class SWORDv2MediaResourceServlet extends SwordServlet {

    @Inject
    MediaResourceManagerImpl mediaResourceManagerImpl;
    /**
     * @todo Should we inject this in all the SWORDv2 Servlets? Added here so
     * that we can inject SettingsServiceBean in SwordConfigurationImpl.
     */
    @Inject
    SwordConfigurationImpl swordConfigurationImpl;

    protected MediaResourceAPI api;
    
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void init() throws ServletException {
        super.init();

        // load the api
        this.api = new MediaResourceAPI(mediaResourceManagerImpl, swordConfigurationImpl);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            mediaResourceManagerImpl.setHttpRequest(req);
            this.api.get(req, resp);
            mediaResourceManagerImpl.setHttpRequest(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            mediaResourceManagerImpl.setHttpRequest(req);
            this.api.head(req, resp);
            mediaResourceManagerImpl.setHttpRequest(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            mediaResourceManagerImpl.setHttpRequest(req);
            this.api.post(req, resp);
            mediaResourceManagerImpl.setHttpRequest(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            mediaResourceManagerImpl.setHttpRequest(req);
            this.api.put(req, resp);
            mediaResourceManagerImpl.setHttpRequest(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            lock.lock();
            mediaResourceManagerImpl.setHttpRequest(req);
            this.api.delete(req, resp);
            mediaResourceManagerImpl.setHttpRequest(null);
        } finally {
            lock.unlock();
        }
    }
}
