package edu.harvard.iq.dataverse.util;

/**
 * System-wide configuration
 */
public class SystemConfig {

    /**
     * A JVM option for the advertised fully qualified domain name (hostname) of
     * the Dataverse installation, such as "dataverse.example.com", which may
     * differ from the hostname that the server knows itself as.
     *
     * The equivalent in DVN 3.x was "dvn.inetAddress".
     */
    public static final String FQDN = "dataverse.fqdn";

}
