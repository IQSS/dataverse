package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import com.github.sleroy.fakesmtp.core.ServerConfiguration;
import com.github.sleroy.fakesmtp.model.EmailModel;
import com.github.sleroy.junit.mail.server.MailServer;

import java.util.List;

/**
 * Wrapper around a fake mail server.
 */
public class FakeSmtpServer {

    /** The server configuration. */
    private final ServerConfiguration serverConfiguration;

    /** The fake mail server. */
    private MailServer mailServer;

    public FakeSmtpServer(final ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    public void shutdownServer() {
        try {
            if (mailServer != null) {
                mailServer.close();
                mailServer = null;
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startServer() throws Throwable {
        mailServer = new MailServer(serverConfiguration);
        mailServer.start();
    }

    public List<EmailModel> getMails() {
        return mailServer.getMails();
    }

    public void clearMails() {
        mailServer.getMails().clear();
        mailServer.getRejectedMails().clear();
    }
}
