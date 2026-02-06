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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
public class CorsIT {
    private static final String ORIGIN_NULL = "null";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @ParameterizedTest(name = "CORS preflight headers on {0}")
    @ValueSource(strings = {
            "/api/dataverses/root/datasets",
            "/api/v1/dataverses/root/datasets",
            "/page_doesnt_exist",
            "/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/root"
    })
    public void testPreflightOptionsCorsHeaders(String path) {
        assertPreflightCorsHeaders(path);
    }

    private void assertPreflightCorsHeaders(String path) {
        Response response = given()
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9,es;q=0.8,hu;q=0.7")
            .header("Access-Control-Request-Headers", "content-type,x-dataverse-key")
            .header("Access-Control-Request-Method", "POST")
            .header("Origin", ORIGIN_NULL)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .when()
            .options(path)
            .then()
            .log().ifValidationFails()
            .statusCode(anyOf(is(200), is(204)))
            .header("Access-Control-Allow-Methods", not(blankOrNullString()))
            .header("Access-Control-Allow-Headers", not(blankOrNullString()))
            .header("Access-Control-Expose-Headers", not(blankOrNullString()))
            .extract()
            .response();

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
