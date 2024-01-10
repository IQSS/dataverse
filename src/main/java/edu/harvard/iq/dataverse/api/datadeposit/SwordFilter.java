package edu.harvard.iq.dataverse.api.datadeposit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class SwordFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        MutateHeaders requestWrapper = new MutateHeaders(req);
        chain.doFilter(requestWrapper, response);
    }

    /**
     * We are mutating headers because Payara 6 is more strict than Paraya 5 and
     * wants "attachment; filename=" instead of just "filename=". In order to
     * not break backward compatibility, we add "attachment; " for our (SWORD)
     * API users. (This only seem to affect our SWORD API.) That is, the can
     * continue to send '-H "Content-Disposition: filename=example.zip"' as
     * we've documented for years.
     */
    public class MutateHeaders extends HttpServletRequestWrapper {

        public MutateHeaders(HttpServletRequest request) {
            super(request);
        }

        // inspired by https://stackoverflow.com/questions/2811769/adding-an-http-header-to-the-request-in-a-servlet-filter/2811841#2811841
        @Override
        public String getHeader(String name) {
            String header = super.getHeader(name);
            if ("Content-Disposition".equalsIgnoreCase(name)) {
                if (header.startsWith("filename=")) {
                    header = header.replaceFirst("filename=", "attachment; filename=");
                }
            }
            return (header != null) ? header : super.getParameter(name);
        }

    }

}
