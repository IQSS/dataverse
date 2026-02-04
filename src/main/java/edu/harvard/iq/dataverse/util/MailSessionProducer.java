package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class MailSessionProducer {
    
    // NOTE: We do not allow "from" here, as we want the transport to get it from the message being sent, enabling
    //       matching addresses. If "from" in transport and "from" in the message differ, some MTAs may reject or
    //       classify as spam.
    // NOTE: Complete list including descriptions at https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html
    static final List<String> smtpStringProps = List.of(
        "host", "localhost", "localaddress", "auth.mechanisms", "auth.ntlm.domain", "submitter", "dsn.notify", "dsn.ret",
        "sasl.mechanisms", "sasl.authorizationid", "sasl.realm", "ssl.trust", "ssl.protocols", "ssl.ciphersuites",
        "proxy.host", "proxy.port", "proxy.user", "proxy.password", "socks.host", "socks.port", "mailextension"
    );
    static final List<String> smtpIntProps = List.of(
        "port", "connectiontimeout", "timeout", "writetimeout", "localport", "auth.ntlm.flag"
    );
    static final List<String> smtpBoolProps = List.of(
        "auth", "ehlo", "auth.login.disable", "auth.plain.disable", "auth.digest-md5.disable", "auth.ntlm.disable",
        "auth.xoauth2.disable", "allow8bitmime", "sendpartial", "sasl.enable", "sasl.usecanonicalhostname",
        "quitwait", "quitonsessionreject", "ssl.enable", "ssl.checkserveridentity", "starttls.enable",
        "starttls.required", "userset", "noop.strict"
    );
    
    private static final String PREFIX = "mail.smtp.";
    private static final Logger logger = Logger.getLogger(MailSessionProducer.class.getCanonicalName());
    
    static {
        if (Boolean.TRUE.equals(JvmSettings.MAIL_DEBUG.lookup(Boolean.class))) {
            logger.setLevel(Level.FINE);
        }
    }
    
    Session systemMailSession;
    
    /**
     * Cache the application server provided (user defined) javamail resource to enable backwards compatibility.
     * No direct JNDI lookup on the field to avoid deployment failures when not present.
     * @deprecated This should be removed with the next major release of Dataverse, as it would be a breaking change.
     */
    @Deprecated(forRemoval = true, since = "6.2")
    Session appserverProvidedSession;
    
    public MailSessionProducer() {
        try {
            // Do JNDI lookup of legacy mail session programmatically to avoid deployment errors when not found.
            Context initialContext = new InitialContext();
            this.appserverProvidedSession = (Session)initialContext.lookup("mail/notifyMailSession");
        } catch (NamingException e) {
            // This exception simply means the appserver did not provide the legacy mail session.
            // Debug level output is just fine.
            logger.log(Level.FINER, "Error during legacy appserver-level mail resource lookup", e);
        }
    }
    
    @Produces
    @Named("mail/systemSession")
    public Session getSession() {
        //  For backward compatibility, prefer to return the mail resource configured on the appserver.
        if (appserverProvidedSession != null) {
            logger.warning("The configuration of mail transfer agents using asadmin create-javamail-resource is" +
                " deprecated. Please migrate to using JVM options, see Dataverse guides for details");
            return appserverProvidedSession;
        }
        
        if (systemMailSession == null) {
            logger.fine("Setting up new mail session");
            
            // Initialize with null (= no authenticator) is a valid argument for the session factory method.
            Authenticator authenticator = null;
            
            // In case we want auth, create an authenticator (default = false from microprofile-config.properties)
            if (Boolean.TRUE.equals(JvmSettings.MAIL_MTA_AUTH.lookup(Boolean.class))) {
                logger.fine("Mail Authentication is enabled, building authenticator");
                authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        logger.fine(() ->
                            String.format("Returning PasswordAuthenticator with username='%s', password='%s'",
                                JvmSettings.MAIL_MTA_USER.lookup(),
                                "*".repeat(JvmSettings.MAIL_MTA_PASSWORD.lookup().length())));
                        return new PasswordAuthentication(JvmSettings.MAIL_MTA_USER.lookup(), JvmSettings.MAIL_MTA_PASSWORD.lookup());
                    }
                };
            }
            
            this.systemMailSession = Session.getInstance(getMailProperties(), authenticator);
        }
        return systemMailSession;
    }
    
    Properties getMailProperties() {
        Properties configuration = new Properties();
        
        // See https://jakarta.ee/specifications/mail/2.1/apidocs/jakarta.mail/jakarta/mail/package-summary
        configuration.put("mail.transport.protocol", "smtp");
        configuration.put("mail.debug", JvmSettings.MAIL_DEBUG.lookupOptional(Boolean.class).orElse(false).toString());
        // Only enable if your MTA properly supports UTF-8 mail addresses following RFC 6530/6531/6532.
        // Before, we used a hack to put the raw UTF-8 mail address into the system.
        // Now, make it proper, but make it possible to disable it - see also EMailValidator.
        // Default = true from microprofile-config.properties as most MTAs these days support SMTPUTF8 extension
        configuration.put("mail.mime.allowutf8", JvmSettings.MAIL_MTA_SUPPORT_UTF8.lookup(Boolean.class).toString());
        
        // Map properties 1:1 to mail.smtp properties for the mail session.
        smtpStringProps.forEach(
            prop -> JvmSettings.MAIL_MTA_SETTING.lookupOptional(prop).ifPresent(
                string -> configuration.put(PREFIX + prop, string)));
        smtpBoolProps.forEach(
            prop -> JvmSettings.MAIL_MTA_SETTING.lookupOptional(Boolean.class, prop).ifPresent(
                bool -> configuration.put(PREFIX + prop, bool.toString())));
        smtpIntProps.forEach(
            prop -> JvmSettings.MAIL_MTA_SETTING.lookupOptional(Integer.class, prop).ifPresent(
                number -> configuration.put(PREFIX + prop, number.toString())));
        
        logger.fine(() -> "Compiled properties:" + configuration.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
            .collect(Collectors.joining(",\n")));
        
        return configuration;
    }
    
    /**
     * Determine if the session returned by {@link #getSession()} has been provided by the application server
     * @return True if injected as resource from app server, false otherwise
     * @deprecated This is supposed to be removed when {@link #appserverProvidedSession} is removed.
     */
    @Deprecated(forRemoval = true, since = "6.2")
    public boolean hasSessionFromAppServer() {
        return this.appserverProvidedSession != null;
    }
    
}
