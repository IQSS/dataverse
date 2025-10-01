package edu.harvard.iq.dataverse.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CorsFilterTest {

    private final Map<String, String> sysPropsBackup = new HashMap<>();

    @BeforeEach
    void setUp() {
        // backup potentially touched props
        backupAndClear("dataverse.cors.origin");
        backupAndClear("dataverse.cors.methods");
        backupAndClear("dataverse.cors.headers.allow");
        backupAndClear("dataverse.cors.headers.expose");
    }

    @AfterEach
    void tearDown() {
        restore("dataverse.cors.origin");
        restore("dataverse.cors.methods");
        restore("dataverse.cors.headers.allow");
        restore("dataverse.cors.headers.expose");
    }

    @Test
    void wildcardOrigin_allowsAny_noVary() throws Exception {
        System.setProperty("dataverse.cors.origin", "*");

        CorsFilter sut = new CorsFilter();
        injectSettingsAllowCors(sut, true);
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://a.example");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        sut.doFilter(req, res, chain);

        verify(res).setHeader("Access-Control-Allow-Origin", "*");
        // By design, Vary not required for wildcard
        verify(res, never()).setHeader(eq("Vary"), anyString());
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void singleOrigin_echoesAndAddsVary() throws Exception {
        System.setProperty("dataverse.cors.origin", "https://libis.github.io");

        CorsFilter sut = new CorsFilter();
        injectSettingsAllowCors(sut, true);
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://libis.github.io");
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(res.getHeader("Vary")).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        sut.doFilter(req, res, chain);

        verify(res).setHeader("Access-Control-Allow-Origin", "https://libis.github.io");

        ArgumentCaptor<String> varyVal = ArgumentCaptor.forClass(String.class);
        verify(res).setHeader(eq("Vary"), varyVal.capture());
        assertTrue(varyVal.getValue().contains("Origin"));
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void multipleOrigins_echoesMatch_onlyWhenAllowed() throws Exception {
        // Comma-separated list as set via JVM options/Microprofile
        System.setProperty("dataverse.cors.origin", "https://a.example, https://b.example");

        CorsFilter sut = new CorsFilter();
        injectSettingsAllowCors(sut, true);
        sut.init(null);

        // allowed origin
        HttpServletRequest reqAllowed = mock(HttpServletRequest.class);
        when(reqAllowed.getHeader("Origin")).thenReturn("https://b.example");
        HttpServletResponse resAllowed = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        sut.doFilter(reqAllowed, resAllowed, chain);
        verify(resAllowed).setHeader("Access-Control-Allow-Origin", "https://b.example");
        verify(resAllowed).setHeader(eq("Vary"), contains("Origin"));

        // not allowed origin -> no ACAO header set
        HttpServletRequest reqDenied = mock(HttpServletRequest.class);
        when(reqDenied.getHeader("Origin")).thenReturn("https://c.example");
        HttpServletResponse resDenied = mock(HttpServletResponse.class);

        sut.doFilter(reqDenied, resDenied, chain);
        verify(resDenied, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
    }

    @Test
    void whitespaceAndMixedCasingParsing() throws Exception {
        System.setProperty("dataverse.cors.origin",
                "  https://one.example  ,\n\t https://two.example  ,  https://three.example  ");

        CorsFilter sut = new CorsFilter();
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://two.example");
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(res.getHeader("Vary")).thenReturn("Accept-Encoding");

        sut.doFilter(req, res, mock(FilterChain.class));

        verify(res).setHeader("Access-Control-Allow-Origin", "https://two.example");
        // ensure existing Vary preserved and Origin added
        verify(res).setHeader(eq("Vary"), argThat(v -> v.contains("Origin") && v.contains("Accept-Encoding")));
    }

    @Test
    void wildcardAmongOthersTreatsAsWildcard() throws Exception {
        System.setProperty("dataverse.cors.origin", "https://a.example,*,https://b.example");

        CorsFilter sut = new CorsFilter();
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://random.example");
        HttpServletResponse res = mock(HttpServletResponse.class);

        sut.doFilter(req, res, mock(FilterChain.class));

        verify(res).setHeader("Access-Control-Allow-Origin", "*");
        verify(res, never()).setHeader(eq("Vary"), anyString());
    }

    @Test
    void existingVaryMergedWithoutDuplication() throws Exception {
        System.setProperty("dataverse.cors.origin", "https://merge.example");

        CorsFilter sut = new CorsFilter();
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://merge.example");
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(res.getHeader("Vary")).thenReturn("Accept-Encoding, Origin");

        sut.doFilter(req, res, mock(FilterChain.class));

        // Origin should not be duplicated
        verify(res).setHeader(eq("Vary"), argThat(v -> v.indexOf("Origin") == v.lastIndexOf("Origin")));
    }

    @Test
    void quotedHeaderListsPreserved() throws Exception {
        System.setProperty("dataverse.cors.origin", "https://x.example");
        System.setProperty("dataverse.cors.headers.allow", "\"Accept, X-Dataverse-key\"");
        System.setProperty("dataverse.cors.headers.expose", "\"Accept-Ranges, Content-Range\"");
        System.setProperty("dataverse.cors.methods", "GET, POST, OPTIONS");

        CorsFilter sut = new CorsFilter();
        injectSettingsAllowCors(sut, true);
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://x.example");
        HttpServletResponse res = mock(HttpServletResponse.class);

        sut.doFilter(req, res, mock(FilterChain.class));
        
        // With simplified CsvUtil we now preserve surrounding quotes provided by admin config.
        verify(res).setHeader("Access-Control-Allow-Headers", "\"Accept, X-Dataverse-key\"");
        verify(res).setHeader("Access-Control-Expose-Headers", "\"Accept-Ranges, Content-Range\"");
        verify(res).setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    @Test
    void disabledCors_skipsHeaders() throws Exception {
        // no origin set -> CORS disabled
        CorsFilter sut = new CorsFilter();
        sut.init(null);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://any.example");
        HttpServletResponse res = mock(HttpServletResponse.class);

        sut.doFilter(req, res, mock(FilterChain.class));

        verify(res, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(res, never()).setHeader(eq("Access-Control-Allow-Methods"), anyString());
        verify(res, never()).setHeader(eq("Access-Control-Allow-Headers"), anyString());
        verify(res, never()).setHeader(eq("Access-Control-Expose-Headers"), anyString());
    }

    // No-op since filter no longer depends on SettingsServiceBean
    private void injectSettingsAllowCors(CorsFilter sut, boolean allowCors) {
        /* legacy path removed */ }

    private void backupAndClear(String key) {
        String old = System.getProperty(key);
        if (old != null) {
            sysPropsBackup.put(key, old);
        }
        System.clearProperty(key);
    }

    private void restore(String key) {
        System.clearProperty(key);
        if (sysPropsBackup.containsKey(key)) {
            System.setProperty(key, sysPropsBackup.get(key));
        }
    }
}
