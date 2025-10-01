package edu.harvard.iq.dataverse.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CorsFilter is a servlet filter that handles Cross-Origin Resource Sharing
 * (CORS) for the Dataverse application.
 * It configures and applies CORS headers to HTTP responses based on application
 * settings.
 * 
 * This filter:
 * 1. Reads CORS configuration from JVM options/Microprofile settings (e.g.
 * dataverse.cors.*).
 * 2. Determines whether CORS should be allowed based on these settings.
 * 3. If CORS is allowed, it adds the appropriate CORS headers to all HTTP
 * responses. The JvmSettings allow customization of the header contents if
 * desired.
 * 
 * The filter is applied to all paths ("/*") in the application.
 */

@WebFilter("/*")
public class CorsFilter implements Filter {

    private boolean allowCors;
    private String methods;
    private String allowHeaders;
    private String exposeHeaders;
    private Set<String> allowedOrigins = Collections.emptySet();
    private boolean allowAllOrigins = false;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Parse allowed origins list (optional)
        // Treat CORS origin list as optional: when absent, CORS is disabled (see CorsFilterTest.disabledCors_skipsHeaders)
        final List<String> originTokens = JvmSettings.CORS_ORIGIN.lookupCsvListOptional().orElse(List.of());
        allowCors = !originTokens.isEmpty();

        if (allowCors) {
            // '*' anywhere means all origins
            if (originTokens.contains("*")) {
                allowAllOrigins = true;
                allowedOrigins = Collections.emptySet();
            } else {
                // Origin tokens already had surrounding comma-whitespace removed by CsvUtil; we only trim here when reading the header.
                allowedOrigins = Set.copyOf(originTokens);
            }

            methods = JvmSettings.CORS_METHODS.lookupCsvListOptional()
                    .map(l -> String.join(", ", l))
                    .orElse("GET, POST, OPTIONS, PUT, DELETE");
            allowHeaders = JvmSettings.CORS_ALLOW_HEADERS.lookupCsvListOptional()
                    .map(l -> String.join(", ", l))
                    .orElse("Accept, Content-Type, X-Dataverse-key, Range");
            exposeHeaders = JvmSettings.CORS_EXPOSE_HEADERS.lookupCsvListOptional()
                    .map(l -> String.join(", ", l))
                    .orElse("Accept-Ranges, Content-Range, Content-Encoding");
        }
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain)
            throws IOException, ServletException {
        if (allowCors) {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            final HttpServletResponse response = (HttpServletResponse) servletResponse;

            final String originHeader = request.getHeader("Origin");
            final String requestOrigin = originHeader == null ? null : originHeader.trim();

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

    private String appendVary(final String existing, final String token) {
        if (existing == null || existing.isEmpty()) {
            return token;
        }
        // Avoid duplicate tokens in Vary
        final Set<String> tokens = Arrays.stream(existing.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        tokens.add(token);
        return String.join(", ", tokens);
    }
}
