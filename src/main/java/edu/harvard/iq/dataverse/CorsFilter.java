package edu.harvard.iq.dataverse;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@WebFilter("/*")
public class CorsFilter implements Filter {

    @Inject
    private SettingsServiceBean settingsSvc;

    private boolean allowCors;
    private String origin;
    private String methods;
    private String allowHeaders;
    private String exposeHeaders;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        origin = JvmSettings.CORS_ORIGIN.lookupOptional().orElse(null);
        boolean corsSetting = settingsSvc.isTrueForKey(SettingsServiceBean.Key.AllowCors, true);

        if (origin == null && !corsSetting) {
            allowCors = false;
        } else {
            allowCors = true;
            origin = (origin != null) ? origin : "*";
        }

        if (allowCors) {
            methods = JvmSettings.CORS_METHODS.lookupOptional().orElse("PUT, GET, POST, DELETE, OPTIONS");
            allowHeaders = JvmSettings.CORS_ALLOW_HEADERS.lookupOptional()
                    .orElse("Accept, Content-Type, X-Dataverse-Key, Range");
            exposeHeaders = JvmSettings.CORS_EXPOSE_HEADERS.lookupOptional()
                    .orElse("Accept-Ranges, Content-Range, Content-Encoding");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (allowCors) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.addHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Access-Control-Allow-Methods", methods);
            response.addHeader("Access-Control-Allow-Headers", allowHeaders);
            response.addHeader("Access-Control-Expose-Headers", exposeHeaders);
        }
        chain.doFilter(servletRequest, servletResponse);
    }
}
