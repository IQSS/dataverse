/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.oai;

import io.gdcc.xoai.serviceprovider.client.OAIClient;

import io.gdcc.xoai.serviceprovider.exceptions.OAIRequestException;
import io.gdcc.xoai.serviceprovider.parameters.Parameters;
import java.io.IOException;
import java.io.InputStream;
import static java.net.HttpURLConnection.HTTP_OK;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.ListIterator;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.Header;

/**
 * Sane default OAI Client implementation using JDK HTTP Client. Can only be used via builder in
 * calling code.
 * (this is essentially a copy of the final class JdkHttpOaiClient provided by 
 * gdcc.xoai, with the custom http headers added. proof of concept!
 */
public final class CustomJdkHttpXoaiClient extends OAIClient {

    private static final Logger log = LoggerFactory.getLogger(OAIClient.class.getCanonicalName());

    // As these vars will be feed via the builder and those provide defaults and null-checks,
    // we may assume FOR INTERNAL USE these are not null.
    private final String baseUrl;
    private final String userAgent;
    private final Duration requestTimeout;
    private final HttpClient httpClient;
    // Custom headers are optional though, ok to be null:
    private final List<Header> customHeaders;

    
    CustomJdkHttpXoaiClient(
            String baseUrl, String userAgent, Duration requestTimeout, List<Header> customHeaders, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.requestTimeout = requestTimeout;
        this.httpClient = httpClient;
        this.customHeaders = customHeaders; 
    }

    @Override
    public InputStream execute(Parameters parameters) throws OAIRequestException {
        try {
            URI requestURI = URI.create(parameters.toUrl(this.baseUrl));

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(requestURI)
                    .GET()
                    .header("User-Agent", this.userAgent)
                    .timeout(requestTimeout);
            
            // add custom headers, if present:
            if (customHeaders != null) {
                ListIterator<Header> iterator = customHeaders.listIterator();
                while (iterator.hasNext()) {
                    Header customHeader = iterator.next();
                    httpRequestBuilder.header(customHeader.getName(), customHeader.getValue());
                }
            }
            
            HttpRequest request = httpRequestBuilder.build();

            HttpResponse<InputStream> response =
                    this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == HTTP_OK) {
                return response.body();
            } else {
                // copy body of the response to string and send as exception message
                throw new OAIRequestException(
                        "Query faild with status code "
                                + response.statusCode()
                                + ": "
                                + new String(
                                        response.body().readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException | IOException | InterruptedException ex) {
            // Hint by SonarCloud:
            // https://sonarcloud.io/organizations/gdcc/rules?open=java%3AS2142&rule_key=java%3AS2142
            Thread.currentThread().interrupt();
            throw new OAIRequestException(ex);
        }
    }
    
    /*@Override 
    JdkHttpBuilder newBuilder() {
        return new CustomJdkHttpXoaiClient.JdkHttpBuilder();
    }*/

    /**
     * Build an {@link OAIClient} using the JDK native HTTP client. You may use your own prepared
     * {@link HttpClient.Builder} instead of the default one.
     *
     * <p>Provides defaults for request timeouts (60s) and user agent. Remember to set the base
     * OAI-PMH URL via {@link #withBaseUrl(URL)}. An exception will occur on first request
     * otherwise.
     */
    public static final class JdkHttpBuilder implements OAIClient.Builder {
        private String baseUrl = "Must be set via Builder.withBaseUrl()";
        private String userAgent = "XOAI Service Provider v5";
        private Duration requestTimeout = Duration.ofSeconds(60);
        private List<Header> customHeaders = null; 
        private final HttpClient.Builder httpClientBuilder;

        JdkHttpBuilder() {
            this.httpClientBuilder = HttpClient.newBuilder();
        }

        /**
         * While the default constructor can be accessed via {@link OAIClient#newBuilder()}, if
         * someone provides a {@link HttpClient.Builder} (which might already contain
         * configuration), happily work with it.
         *
         * @param httpClientBuilder Any (preconfigured) Java 11+ HTTP client builder
         */
        public JdkHttpBuilder(HttpClient.Builder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
        }

        @Override
        public JdkHttpBuilder withBaseUrl(URL baseUrl) {
            return this.withBaseUrl(baseUrl.toString());
        }

        @Override
        public JdkHttpBuilder withBaseUrl(String baseUrl) {
            try {
                new URL(baseUrl).toURI();
                if (!baseUrl.startsWith("http")) {
                    throw new IllegalArgumentException("OAI-PMH supports HTTP/S only");
                }
                this.baseUrl = baseUrl;
                return this;
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public JdkHttpBuilder withConnectTimeout(Duration timeout) {
            // validation is done by client builder!
            httpClientBuilder.connectTimeout(timeout);
            return this;
        }

        @Override
        public JdkHttpBuilder withRequestTimeout(Duration timeout) {
            if (timeout == null || timeout.isNegative()) {
                throw new IllegalArgumentException("Timeout must not be null or negative value");
            }
            this.requestTimeout = timeout;
            return this;
        }

        @Override
        public JdkHttpBuilder withUserAgent(String userAgent) {
            if (userAgent == null || userAgent.isBlank()) {
                throw new IllegalArgumentException("User agent must not be null or empty/blank");
            }
            this.userAgent = userAgent;
            return this;
        }

        @Override
        public JdkHttpBuilder withFollowRedirects() {
            this.httpClientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
            return this;
        }

        @Override
        public JdkHttpBuilder withInsecureSSL() {
            // create insecure context (switch of certificate checks)
            httpClientBuilder.sslContext(insecureContext());

            // warn if the hostname verification is still active
            // (users must do this themselves - it's a global setting and might pose a security
            // risk)
            if (!Boolean.getBoolean("jdk.internal.httpclient.disableHostnameVerification")) {
                log.warn(
                        "You must disable JDK HTTP Client Host Name Verification globally via"
                            + " system property"
                            + " -Djdk.internal.httpclient.disableHostnameVerification=true for"
                            + " XOAI Client connections to insecure SSL servers. Don't do this in"
                            + " a production setup!");
            }
            return this;
        }
        
        public JdkHttpBuilder withCustomHeaders(List<Header> customHeaders) {
            // This can be null, as these headers are optional
            this.customHeaders = customHeaders; 
            return this;
        }

        @Override
        public CustomJdkHttpXoaiClient build() {
            return new CustomJdkHttpXoaiClient(
                    this.baseUrl, this.userAgent, this.requestTimeout, this.customHeaders, httpClientBuilder.build());
        }

        private static SSLContext insecureContext() {
            TrustManager[] noopTrustManager =
                    new TrustManager[] {
                        new X509TrustManager() {
                            // This is insecure by design, we warn users and they need to do sth. to
                            // use it.
                            // Safely ignore the Sonarcloud message.
                            @SuppressWarnings("java:S4830")
                            public void checkClientTrusted(X509Certificate[] xcs, String string) {
                                // we want to accept every certificate - intentionally left blank
                            }
                            // This is insecure by design, we warn users and they need to do sth. to
                            // use it.
                            // Safely ignore the Sonarcloud message.
                            @SuppressWarnings("java:S4830")
                            public void checkServerTrusted(X509Certificate[] xcs, String string) {
                                // we want to accept every certificate - intentionally left blank
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                    };
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, noopTrustManager, null);
                return sc;
            } catch (KeyManagementException | NoSuchAlgorithmException ex) {
                log.error("Could not build insecure SSL context. Might cause NPE.", ex);
                return null;
            }
        }
    }
}
