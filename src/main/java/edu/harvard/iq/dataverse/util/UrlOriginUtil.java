package edu.harvard.iq.dataverse.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;

/**
 * Normalizes URLs to their web-origin form for same-origin comparisons.
 * Lives in the util layer because it is shared by request-time checks
 * ({@code AuthFilter}'s session-cookie CSRF hardening) and startup
 * configuration validation ({@code ConfigCheckService}) — neither of which
 * should depend on the other.
 */
public final class UrlOriginUtil {

    private UrlOriginUtil() {
    }

    /**
     * Normalizes {@code url} to its origin form ({@code scheme://host[:port]}),
     * lowercasing scheme and host and omitting the scheme's default port.
     * Returns {@code null} if the input is missing scheme or host, or is
     * unparseable.
     */
    public static String toOrigin(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (port == -1 || port == defaultPort(uri)) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The scheme's default port per the JDK's protocol handlers (80 for http,
     * 443 for https, ...), or -1 when the JDK has no handler for the scheme —
     * in which case no port is ever considered "default" and an explicit port
     * always stays part of the origin.
     */
    private static int defaultPort(URI uri) {
        try {
            return uri.toURL().getDefaultPort();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return -1;
        }
    }
}
