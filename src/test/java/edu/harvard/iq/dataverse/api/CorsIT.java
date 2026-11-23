package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for CORS headers on API endpoints. These tests verify that the expected CORS
 * headers are present and contain the correct values for preflight OPTIONS requests to key
 * API endpoints.
 *
 * For this to work CORS has to be enabled. Eg. in docker-compose-dev.yml add
 *       DATAVERSE_CORS_ORIGIN: "*"
 * env to `dev_dataverse`.
 */
class CorsIT {
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }
    
    /**
     * Tests the presence of CORS preflight headers on various subsystems by sending HTTP OPTIONS requests
     * to specified paths and validating the responses for expected headers and status.
     * These paths are served by different servlets, filters, and frameworks.
     * Nonetheless, any of them should present CORS headers when configured.
     *
     * <p>
     * TODO: Currently, this test relies on the CI infrastructure executing the test to have set at least
     *       the JVM setting dataverse.cors.origin. Otherwise, no headers will be sent.
     * </p>
     *
     * <p>
     * NOTE: At the time of writing (2026-04), there is no infrastructure available to a) manipulate
     *       these settings in this end-to-end testing scenario nor b) to dynamically reload the test subject.
     *       It is initialized once at deployment time, which would require isolating this test some other way.
     *       Thus, only the <i>presence</i> of headers is checked, but not its content
     *       (which is fine, given the scope of the test).
     * </p>
     *
     * @param path the relative path on the subsystem to which the CORS preflight request is sent
     */
    @ParameterizedTest(name = "CORS preflight headers on {0}")
    @ValueSource(strings = {
        "/api/dataverses/root/datasets",
        "/api/v1/dataverses/root/datasets",
        "/page_doesnt_exist",
        "/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/root"
    })
    void ensurePresenceOnDifferentSubsystems(String path) {
        given()
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9,es;q=0.8,hu;q=0.7")
            .header("Access-Control-Request-Headers", "content-type,x-dataverse-key")
            .header("Access-Control-Request-Method", "POST")
            .header("Origin", "null")
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
        .when()
            .options(path)
        .then()
            .log().ifValidationFails()
            .statusCode(anyOf(is(200), is(204)))
            .header("Access-Control-Allow-Methods", not(blankOrNullString()))
            .header("Access-Control-Allow-Headers", not(blankOrNullString()))
            .header("Access-Control-Expose-Headers", not(blankOrNullString()));
    }
    
    /*
    The following code may be used in a future test enabling assertions of header contents:
    
    Usage:
    assertHeaderSetEquals("Access-Control-Allow-Methods", expectedCorsMethods, response);
    assertHeaderSetEquals("Access-Control-Allow-Headers", expectedCorsAllowHeaders, response);
    assertHeaderSetEquals("Access-Control-Expose-Headers", expectedCorsExposeHeaders, response);
    
    Class fields:
    private final List<String> expectedCorsMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private final List<String> expectedCorsAllowHeaders = List.of("Accept", "Content-Type", "X-Dataverse-key", "Range");
    private final List<String> expectedCorsExposeHeaders = List.of("Accept-Ranges", "Content-Range", "Content-Encoding");

    Assertions methods:
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
    
    */
}
