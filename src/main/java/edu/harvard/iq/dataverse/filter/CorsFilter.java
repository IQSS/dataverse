package edu.harvard.iq.dataverse.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.ListSplitUtil;
import jakarta.servlet.DispatcherType;
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
 * CorsFilter is a servlet filter that handles Cross-Origin Resource Sharing (CORS) for the Dataverse application.
 * It configures and applies CORS headers to HTTP responses based on application settings.
 * 
 * This filter:
 * 1. Reads CORS configuration from JVM settings (dataverse.cors.*). See the Dataverse Configuration Guide for more details.
 * 2. Determines whether CORS should be allowed based on these settings.
 * 3. If CORS is allowed, it adds the appropriate CORS headers to all HTTP responses. The JVMSettings allow customization of the header contents if desired.
 * 
 * The filter is applied to all paths ("/*") in the application.
 */

@WebFilter(value = "/*", dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD})
public class CorsFilter implements Filter {

    private boolean allowCors;
    private boolean allowAllOrigins;
    private Set<String> allowedOrigins = Collections.emptySet();
    private String methods;
    private String allowHeaders;
    private String exposeHeaders;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        List<String> origins = JvmSettings.CORS_ORIGIN.lookupSplittedListOptional().orElse(List.of());
        allowCors = !origins.isEmpty();

        if (allowCors) {
            if (origins.contains("*")) {
                allowAllOrigins = true;
            } else {
                allowedOrigins = Set.copyOf(origins);
            }

            methods = JvmSettings.CORS_METHODS.lookupSplittedListOptional()
                    .map(values -> String.join(", ", values))
                    .orElse("GET, POST, OPTIONS, PUT, DELETE");
            allowHeaders = JvmSettings.CORS_ALLOW_HEADERS.lookupSplittedListOptional()
                    .map(values -> String.join(", ", values))
                    .orElse("Accept, Content-Type, X-Dataverse-key, Range");
            exposeHeaders = JvmSettings.CORS_EXPOSE_HEADERS.lookupSplittedListOptional()
                    .map(values -> String.join(", ", values))
                    .orElse("Accept-Ranges, Content-Range, Content-Encoding");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (allowCors) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String originHeader = request.getHeader("Origin");
            String requestOrigin = originHeader == null ? null : originHeader.trim();

            if (allowAllOrigins) {
                response.setHeader("Access-Control-Allow-Origin", "*");
            } else if (requestOrigin != null && allowedOrigins.contains(requestOrigin)) {
                response.setHeader("Access-Control-Allow-Origin", requestOrigin);
                response.setHeader("Vary", appendVary(response.getHeader("Vary"), "Origin"));
            }

            response.setHeader("Access-Control-Allow-Methods", methods);
            response.setHeader("Access-Control-Allow-Headers", allowHeaders);
            response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    private String appendVary(String existing, String value) {
        if (existing == null || existing.isEmpty()) {
            return value;
        }
        Set<String> tokens = ListSplitUtil.split(existing).stream()
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        tokens.add(value);
        return String.join(", ", tokens);
    }
}
