package edu.harvard.iq.dataverse.jsf.support.login;

import com.microsoft.playwright.Page;

/**
 * A LoginAdapter encapsulates one complete end-to-end authentication flow,
 * starting from the Dataverse login page (after the "Log In" link has been
 * clicked) and finishing with the browser landed on an authenticated
 * Dataverse page.
 */
public interface LoginAdapter {

    void login(Page page, LoginCredentials credentials);
}
