package edu.harvard.iq.dataverse.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;

/**
 * Normalizes URLs to their web origin form for same-origin comparisons. Shared by
 * {@code SessionCookieAuthMechanism} and {@code ConfigCheckService}.
 */
public final class UrlOriginUtil {

    private UrlOriginUtil() {
    }

    /**
     * Normalizes {@code url} to its origin form ({@code scheme://host[:port]}), lowercasing
     * scheme and host and omitting the scheme's default port. Returns {@code null} if the
     * input is missing a scheme or host, or is unparseable.
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
     * The scheme's default port per the JDK's protocol handlers, or -1 when the JDK has no
     * handler for the scheme, in which case an explicit port always stays part of the origin.
     */
    private static int defaultPort(URI uri) {
        try {
            return uri.toURL().getDefaultPort();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return -1;
        }
    }
}
