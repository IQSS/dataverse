package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * DataverseRequestTest
 *
 * Determine if the X-Forwarded-For header is parsed correctly.
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
public class DataverseRequestTest {

    @Test
    public void testDataverseRequest() {

        // Mock a servlet request.
        final HttpServletRequest mockHttpServletRequest = new HttpServletRequest() {

            private Map<String, String> headers = new HashMap<>();

            @Override
            public String getAuthType() {
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String key) {
                return headers.get(key);
            }

            @Override
            public Enumeration getHeaders(String s) {
                return null;
            }

            @Override
            public Enumeration getHeaderNames() {
                return null;
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public String getPathTranslated() {
                return null;
            }

            @Override
            public String getContextPath() {
                return null;
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                return null;
            }

            @Override
            public String getServletPath() {
                return null;
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Enumeration getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return null;
            }

            @Override
            public Enumeration getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map getParameterMap() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public String getServerName() {
                return null;
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return "1.2.3.4";
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public void setAttribute(String key, Object value) {
                if (value == null)
                    headers.remove(key);
                else
                    headers.put(key, String.valueOf(value));
            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            public String getRealPath(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return null;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

        };

        final String XFF = "X-Forwarded-For";
        mockHttpServletRequest.setAttribute(XFF, null);

        // No XXF should default to 1.2.3.4
        final IpAddress address1 = new DataverseRequest(null, mockHttpServletRequest).getSourceAddress();
        final IPv4Address expected1 = new IPv4Address(1, 2, 3, 4);
        assertEquals(expected1, address1);

        // should have an IP 9.10.11.12
        final IPv4Address expected2 = new IPv4Address(9, 10, 11, 12);
        mockHttpServletRequest.setAttribute(XFF, expected2.toString());
        final IpAddress address2 = new DataverseRequest(null, mockHttpServletRequest).getSourceAddress();
        assertEquals(expected2, address2);

        // should have an IP 5.6.7.8
        final IPv4Address expected3 = new IPv4Address(5, 6, 7, 8);
        mockHttpServletRequest.setAttribute(XFF, expected3.toString() + ", 9.10.11.12, and anything else.");
        final IpAddress address3 = new DataverseRequest(null, mockHttpServletRequest).getSourceAddress();
        assertEquals(expected3, address3);

        mockHttpServletRequest.setAttribute(XFF, "nonsense");
        try {
            new DataverseRequest(null, mockHttpServletRequest).getSourceAddress();
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }
}
