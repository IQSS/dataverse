package edu.harvard.iq.dataverse.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.settings.JvmSettings;

/**
 * CorsFilter is a servlet filter that handles Cross-Origin Resource Sharing (CORS) for the Dataverse application.
 * It configures and applies CORS headers to HTTP responses based on application settings.
 * 
 * This filter:
 * 1. Reads CORS configuration from JVM options/Microprofile settings (e.g. dataverse.cors.*).
 * 2. Determines whether CORS should be allowed based on these settings.
 * 3. If CORS is allowed, it adds the appropriate CORS headers to all HTTP responses. The JvmSettings allow customization of the header contents if desired.
 * 
 * The filter is applied to all paths ("/*") in the application.
 */

@WebFilter("/*")
public class CorsFilter implements Filter {
    private boolean allowCors;
    private String origin; // raw configured origin value
    private String methods;
    private String allowHeaders;
    private String exposeHeaders;
    private Set<String> allowedOrigins = Collections.emptySet();
    private boolean allowAllOrigins = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        origin = sanitize(JvmSettings.CORS_ORIGIN.lookupOptional().orElse(null));
        allowCors = origin != null && !origin.trim().isEmpty();

        if (allowCors) {
            methods = sanitizeCsv(JvmSettings.CORS_METHODS.lookupOptional().orElse("GET, POST, OPTIONS, PUT, DELETE"));
            allowHeaders = sanitizeCsv(JvmSettings.CORS_ALLOW_HEADERS.lookupOptional()
                    .orElse("Accept, Content-Type, X-Dataverse-key, Range"));
            exposeHeaders = sanitizeCsv(JvmSettings.CORS_EXPOSE_HEADERS.lookupOptional()
                    .orElse("Accept-Ranges, Content-Range, Content-Encoding"));

            // Initialize allowed origins (documented as comma-separated list)
            String configured = origin != null ? origin.trim() : null;
            if (configured == null || configured.isEmpty() || "*".equals(configured)) {
                allowAllOrigins = true;
                allowedOrigins = Collections.emptySet();
            } else {
                // Parse configured origins; code is tolerant of whitespace but
                // docs recommend a comma-separated list (no quotes)
                allowedOrigins = Arrays.stream(configured.split(","))
                        .flatMap(s -> Arrays.stream(s.split("[\n\r\t ]+")))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(HashSet::new));
                // handle a single '"*"' that might slip through incorrectly
                if (allowedOrigins.size() == 1 && allowedOrigins.contains("*")) {
                    allowAllOrigins = true;
                    allowedOrigins = Collections.emptySet();
                }
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (allowCors) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String requestOrigin = sanitize(request.getHeader("Origin"));

            // Decide ACAO value
            if (allowAllOrigins) {
                // Note: Browsers will reject '*' for credentialed requests; this is by design.
                response.setHeader("Access-Control-Allow-Origin", "*");
            } else if (requestOrigin != null && allowedOrigins.contains(requestOrigin)) {
                response.setHeader("Access-Control-Allow-Origin", requestOrigin);
                // Help caches vary based on Origin
                response.setHeader("Vary", appendVary(response.getHeader("Vary"), "Origin"));
            }

            response.setHeader("Access-Control-Allow-Methods", methods);
            response.setHeader("Access-Control-Allow-Headers", allowHeaders);
            response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    /** Remove surrounding quotes and collapse internal quotes. */
    private String sanitize(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\"", "").trim();
    }

    /** Remove quotes from CSV-like header value and trim spaces around commas. */
    private String sanitizeCsv(String value) {
        String v = sanitize(value);
        // Normalize separators and trim tokens
        return Arrays.stream(v.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String appendVary(String existing, String token) {
        if (existing == null || existing.isEmpty()) {
            return token;
        }
        // Avoid duplicate tokens in Vary
        Set<String> tokens = Arrays.stream(existing.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        tokens.add(token);
        return String.join(", ", tokens);
    }
}
