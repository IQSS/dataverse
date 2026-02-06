package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for CORS headers on API endpoints. These tests verify that the expected CORS
 * headers are present and contain the correct values for preflight OPTIONS requests to key
 * API endpoints.
 *
 * For this to work CORS has to be enabled. Eg. in docker-compose-dev.yml add
 *       DATAVERSE_CORS_ORIGIN: "*"
 * env to `dev_dataverse`.
 */
public class CorsIT
{
    private static final String ORIGIN_NULL = "null";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testPreflightOptionsCorsHeaders() {
        assertPreflightCorsHeaders("/api/dataverses/root/datasets");
        assertPreflightCorsHeaders("/api/v1/dataverses/root/datasets");
    }

    private void assertPreflightCorsHeaders(String path) {
        Response response = given()
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9,es;q=0.8,hu;q=0.7")
            .header("Access-Control-Request-Headers", "content-type,x-dataverse-key")
            .header("Access-Control-Request-Method", "POST")
            .header("Origin", ORIGIN_NULL)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .options(path);

        int statusCode = response.getStatusCode();
        assertTrue(statusCode == 200 || statusCode == 204, "Expected 200 or 204 but got " + statusCode);

        assertHeaderSetEquals("Access-Control-Allow-Methods", getExpectedCorsMethods(), response);
        assertHeaderSetEquals("Access-Control-Allow-Headers", getExpectedCorsAllowHeaders(), response);
        assertHeaderSetEquals("Access-Control-Expose-Headers", getExpectedCorsExposeHeaders(), response);
    }

    private static List<String> getExpectedCorsMethods() {
        return List.of("GET", "POST", "OPTIONS", "PUT", "DELETE");
    }

    private static List<String> getExpectedCorsAllowHeaders() {
        return List.of("Accept", "Content-Type", "X-Dataverse-key", "Range");
    }

    private static List<String> getExpectedCorsExposeHeaders() {
        return List.of("Accept-Ranges", "Content-Range", "Content-Encoding");
    }

    private static void assertHeaderSetEquals(String headerName, List<String> expectedTokens, Response response) {
        String headerValue = response.getHeader(headerName);
        assertTrue(headerValue != null && !headerValue.isBlank(), "Missing header: " + headerName);
        Set<String> actual = normalizeTokens(headerValue);
        Set<String> expected = expectedTokens.stream()
                .map(CorsIT::normalizeToken)
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(expected, actual, "Unexpected value for header: " + headerName);
    }

    private static Set<String> normalizeTokens(String headerValue) {
        return Arrays.stream(headerValue.split(","))
                .map(CorsIT::normalizeToken)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
