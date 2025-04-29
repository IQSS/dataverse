package edu.harvard.iq.dataverse;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@WebFilter("/*")
public class CorsFilter implements Filter {


    @Inject
    private SettingsServiceBean settingsSvc;

    private boolean allowCors;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        allowCors = settingsSvc.isTrueForKey(SettingsServiceBean.Key.AllowCors, true);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (allowCors) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "Accept, Content-Type, X-Dataverse-Key, Range");
            response.addHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Range, Content-Encoding");
        }
        chain.doFilter(servletRequest, servletResponse);
    }
}
