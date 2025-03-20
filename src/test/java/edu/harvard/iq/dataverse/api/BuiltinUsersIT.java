package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BuiltinUsersIT {

    private static final Logger logger = Logger.getLogger(BuiltinUsersIT.class.getCanonicalName());

    private static final String builtinUserKey = "burrito";
    private static final String idKey = "id";
    private static final String usernameKey = "userName";
    private static final String emailKey = "email";

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

    }

    @Test
    public void testCreateTimeAndApiLastUse() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response getUserAsJson = UtilIT.getAuthenticatedUser(username, apiToken);
        getUserAsJson.prettyPrint();
        getUserAsJson.then().assertThat()
                // Checking that it's 2017 or whatever. Not y3k compliant! 
                .body("data.createdTime", startsWith("2"))
                .body("data.authenticationProviderId", equalTo("builtin"))
                .statusCode(OK.getStatusCode());

        Response getUserAsJsonAgain = UtilIT.getAuthenticatedUser(username, apiToken);
        getUserAsJsonAgain.prettyPrint();
        getUserAsJsonAgain.then().assertThat()
                // ApiUseTime should be null, user has not used API yet here
                .body("data.lastApiUseTime", equalTo(null))
                .statusCode(OK.getStatusCode());

    }
    
        @Test
    public void testFindByToken() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());

        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response getUserAsJsonByToken = UtilIT.getAuthenticatedUserByToken(apiToken);

        getUserAsJsonByToken.then().assertThat()
                .statusCode(OK.getStatusCode());

        getUserAsJsonByToken = UtilIT.getAuthenticatedUserByToken("badcode");
        getUserAsJsonByToken.then().assertThat()
                .body("status", equalTo("ERROR"))
                .body("message", equalTo(ApiKeyAuthMechanism.RESPONSE_MESSAGE_BAD_API_KEY))
                .statusCode(UNAUTHORIZED.getStatusCode());

    }


    @Test
    public void testLastApiUse() {
        Response createApiUser = UtilIT.createRandomUser();
        String apiUsername = UtilIT.getUsernameFromResponse(createApiUser);
        String secondApiToken = UtilIT.getApiTokenFromResponse(createApiUser);

        Response createDataverse = UtilIT.createRandomDataverse(secondApiToken);
        String alias = UtilIT.getAliasFromResponse(createDataverse);
        Response createDatasetViaApi = UtilIT.createRandomDatasetViaNativeApi(alias, secondApiToken);
        Response getApiUserAsJson = UtilIT.getAuthenticatedUser(apiUsername, secondApiToken);

        getApiUserAsJson.prettyPrint();
        getApiUserAsJson.then().assertThat()
                // Checking that it's 2017 or whatever. Not y3k compliant! 
                .body("data.lastApiUseTime", startsWith("2"))
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void testUserId() {

        String email = null;
        Response createUserResponse = createUser(getRandomUsername(), "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        int builtInUserIdFromJsonCreateResponse = createdUser.getInt("data.user." + idKey);
        int authenticatedUserIdFromJsonCreateResponse = createdUser.getInt("data.authenticatedUser." + idKey);
        String username = createdUser.getString("data.user." + usernameKey);

        Response getUserResponse = getUserFromDatabase(username);
        getUserResponse.prettyPrint();
        assertEquals(200, getUserResponse.getStatusCode());

        JsonPath getUserJson = JsonPath.from(getUserResponse.body().asString());
        int userIdFromDatabase = getUserJson.getInt("data.id");

        Response deleteUserResponse = deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
        deleteUserResponse.prettyPrint();

        System.out.println(userIdFromDatabase + " was the id from the database");
        System.out.println(builtInUserIdFromJsonCreateResponse + " was the id of the BuiltinUser from JSON response on create");
        System.out.println(authenticatedUserIdFromJsonCreateResponse + " was the id of the AuthenticatedUser from JSON response on create");
        assertEquals(userIdFromDatabase, authenticatedUserIdFromJsonCreateResponse);
    }

    @Test
    public void testLeadingWhitespaceInEmailAddress() {
        String randomUsername = getRandomUsername();
        String email = " " + randomUsername + "@mailinator.com";
        Response createUserResponse = createUser(randomUsername, "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.statusCode());
        String emailActual = JsonPath.from(createUserResponse.body().asString()).getString("data.authenticatedUser." + emailKey);
        // the backend will trim the email address
        String emailExpected = email.trim();
        assertEquals(emailExpected, emailActual);
    }

    @Test
    public void testLeadingWhitespaceInUsername() {
        String randomUsername = " " + getRandomUsername();
        String email = randomUsername + "@mailinator.com";
        Response createUserResponse = createUser(randomUsername, "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(400, createUserResponse.statusCode());
    }

    @Test
    public void testBadCharacterInUsername() {
        String randomUsername = getRandomUsername() + "/";
        String email = randomUsername + "@mailinator.com";
        Response createUserResponse = createUser(randomUsername, "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(400, createUserResponse.statusCode());
    }

    @Test
    public void testAccentInUsername() {
        String randomUsername = getRandomUsername();
        String randomUsernameWeird = "õÂ" + randomUsername;
        String email = randomUsername + "@mailinator.com";
        Response createUserResponse = createUser(randomUsernameWeird, "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(400, createUserResponse.statusCode());
    }

    @Test
    public void testLogin() {

        String usernameToCreate = getRandomUsername();
        String email = null;
        Response createUserResponse = createUser(usernameToCreate, "firstName", "lastName", email);
        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        String createdUsername = createdUser.getString("data.user." + usernameKey);
        assertEquals(usernameToCreate, createdUsername);
        String createdToken = createdUser.getString("data.apiToken");
        logger.info(createdToken);

        Response getApiTokenShouldFail = UtilIT.getApiTokenUsingUsername(usernameToCreate, usernameToCreate);
        getApiTokenShouldFail.then().assertThat()
                .body("message", equalTo("This API endpoint has been disabled."))
                .statusCode(FORBIDDEN.getStatusCode());

        Response setAllowApiTokenLookupViaApi = UtilIT.setSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi, "true");
        setAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getApiTokenUsingUsername = UtilIT.getApiTokenUsingUsername(usernameToCreate, usernameToCreate);
        getApiTokenUsingUsername.prettyPrint();
        assertEquals(200, getApiTokenUsingUsername.getStatusCode());
        String retrievedTokenUsingUsername = JsonPath.from(getApiTokenUsingUsername.asString()).getString("data.message");
        assertEquals(createdToken, retrievedTokenUsingUsername);

        //TODO: This chunk was for testing email login via API, 
        // but is disabled as it could not be used without a code change and
        // then the code to use it was removed in https://github.com/IQSS/dataverse/pull/4993 .
        // We should consider a better way to test email login --MAD 4.9.3
        
        //if (BuiltinUsers.retrievingApiTokenViaEmailEnabled) {
        //    Response getApiTokenUsingEmail = getApiTokenUsingEmail(usernameToCreate + "@mailinator.com", usernameToCreate);
        //    getApiTokenUsingEmail.prettyPrint();
        //    assertEquals(200, getApiTokenUsingEmail.getStatusCode());
        //    String retrievedTokenUsingEmail = JsonPath.from(getApiTokenUsingEmail.asString()).getString("data.message");
        //    assertEquals(createdToken, retrievedTokenUsingEmail);
        //}
        
        Response failExpected = UtilIT.getApiTokenUsingUsername("junk", "junk");
        failExpected.prettyPrint();
        assertEquals(400, failExpected.getStatusCode());

        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

    }

    @Test
    public void testValidatePasswordScrewsTightened() {

        Arrays.stream(SettingsServiceBean.Key.values())
                .filter(key -> key.name().startsWith("PV"))
                .forEach(key -> given().delete("/api/admin/settings/" + key));

        Response setCharRules = UtilIT.setSetting(SettingsServiceBean.Key.PVCharacterRules, "UpperCase:1,LowerCase:1,Digit:1,Special:1");
        setCharRules.then().assertThat()
                .statusCode(200);
        Response setMinLength = UtilIT.setSetting(SettingsServiceBean.Key.PVMinLength, "8");
        setMinLength.then().assertThat()
                .statusCode(200);
        Response setNumCharacteristics = UtilIT.setSetting(SettingsServiceBean.Key.PVNumberOfCharacteristics, "4");
        setNumCharacteristics.then().assertThat()
                .statusCode(200);
        Response setNumConsecutiveDigitsAllowed = UtilIT.setSetting(SettingsServiceBean.Key.PVNumberOfConsecutiveDigitsAllowed, "4");
        setNumConsecutiveDigitsAllowed.then().assertThat()
                .statusCode(200);

        Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>(" ", Arrays.asList( // All is wrong here:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_LOWERCASE",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_UPPERCASE",
                        "NO_GOODSTRENGTH",
                        "TOO_SHORT"
                )),
                new AbstractMap.SimpleEntry<>("potato", Arrays.asList( // Lowercase ok, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_UPPERCASE",
                        "NO_GOODSTRENGTH",
                        "TOO_SHORT"
                )),
                new AbstractMap.SimpleEntry<>("POTATO", Arrays.asList( // Uppercase ok, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_LOWERCASE",
                        "NO_GOODSTRENGTH",
                        "TOO_SHORT"
                )),
                new AbstractMap.SimpleEntry<>("potat  o", Arrays.asList( // Length and lowercase ok, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_UPPERCASE",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("POTAT  O", Arrays.asList( // Length and uppercase ok, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_LOWERCASE",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("PoTaT  O", Arrays.asList( // correct length ,lower and upper case, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_SPECIAL",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("potat1 o", Arrays.asList( // correct length and digit, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_SPECIAL",
                        "INSUFFICIENT_UPPERCASE",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("potat$ o", Arrays.asList( // correct length and special character, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_UPPERCASE",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("Pot@t00000", Arrays.asList( // correct length, case, special char, but exceeds repeated character limit (illegal match error)
                        "ILLEGAL_MATCH",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("Potat$ 1234!", Collections.<String>emptyList()), // 4 digits in a row is ok
                new AbstractMap.SimpleEntry<>("Potat$ 01!", Collections.<String>emptyList()), // correct length, lowercase, special character and digit. All ok...
                new AbstractMap.SimpleEntry<>("POTAT$ o1!", Collections.<String>emptyList()), // correct length, uppercase, special character and digit. All ok...
                new AbstractMap.SimpleEntry<>("Potat$ o1!", Collections.<String>emptyList()), // correct length, uppercase, lowercase and and special character. All ok...
                new AbstractMap.SimpleEntry<>("Potat  0!", Collections.<String>emptyList()), // correct length, uppercase, lowercase and digit. All ok...
                new AbstractMap.SimpleEntry<>("twentycharactershere", Collections.<String>emptyList())) // 20 character password length. All ok...
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))).forEach(
                (password, expectedErrors) -> {
                    final Response response = given().body(password).when().post("/api/admin/validatePassword");
                    response.prettyPrint();
                    final List<String> actualErrors = JsonPath.from(response.body().asString()).get("data.errors");
                    assertTrue(actualErrors.containsAll(expectedErrors));
                    assertTrue(expectedErrors.containsAll(actualErrors)); 
                }
        );
    }

    @Test
    public void testValidatePasswordsOutOfBoxSettings() {

        Arrays.stream(SettingsServiceBean.Key.values())
                .filter(key -> key.name().startsWith("PV"))
                .forEach(key -> given().delete("/api/admin/settings/" + key));

        Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>(" ", Arrays.<String>asList( // All is wrong here:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "INSUFFICIENT_ALPHABETICAL",
                        "NO_GOODSTRENGTH",
                        "TOO_SHORT"
                )),
                new AbstractMap.SimpleEntry<>("potato", Arrays.<String>asList( // Alpha ok, but:
                        "INSUFFICIENT_CHARACTERISTICS",
                        "INSUFFICIENT_DIGIT",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("123456", Arrays.<String>asList( // correct length and special character, but:
                        "INSUFFICIENT_ALPHABETICAL",
                        "INSUFFICIENT_CHARACTERISTICS",
                        "NO_GOODSTRENGTH"
                )),
                new AbstractMap.SimpleEntry<>("potat1", Collections.<String>emptyList()), // Strong enough for Dataverse 4.0.
                new AbstractMap.SimpleEntry<>("Potat  0", Collections.<String>emptyList()), // All ok...
                new AbstractMap.SimpleEntry<>("                    ", Collections.<String>emptyList())) // 20 character password length. All ok...
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))).forEach(
                (password, expectedErrors) -> {
                    final Response response = given().body(password).when().post("/api/admin/validatePassword");
                    response.prettyPrint();
                    final List<String> actualErrors = JsonPath.from(response.body().asString()).get("data.errors");
                    assertTrue(actualErrors.containsAll(expectedErrors));
                    assertTrue(expectedErrors.containsAll(actualErrors)); 
                }
        );
    }

    private Response createUser(String username, String firstName, String lastName, String email) {
        String userAsJson = getUserAsJsonString(username, firstName, lastName, email);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + builtinUserKey + "&password=" + password);
        return response;
    }

    private Response getUserFromDatabase(String username) {
        Response getUserResponse = given()
                .get("/api/admin/authenticatedUsers/" + username + "/");
        return getUserResponse;
    }

    private static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    private static String getRandomUsername() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName, String email) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(usernameKey, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        if (email == null) {
            builder.add(emailKey, getEmailFromUserName(username));
        } else {
            builder.add(emailKey, email);
        }
        String userAsJson = builder.build().toString();
        logger.fine("User to create: " + userAsJson);
        return userAsJson;
    }

    private static String getPassword(String jsonStr) {
        String password = JsonPath.from(jsonStr).get(usernameKey);
        return password;
    }

    private static String getEmailFromUserName(String username) {
        return username + "@mailinator.com";
    }

}
