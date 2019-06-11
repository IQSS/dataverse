package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class UsersIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
        
        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

    }

    @Test
    public void convertNonBcryptUserFromBuiltinToShib() {

        Response createUserToConvert = UtilIT.createRandomUser();
        createUserToConvert.prettyPrint();
//
        long AuthenticatedUserIdOfBcryptUserToConvert = createUserToConvert.body().jsonPath().getLong("data.authenticatedUser.id");
        long BuiltinUserIdOfBcryptUserToConvert = createUserToConvert.body().jsonPath().getLong("data.user.id");
        String emailOfNonBcryptUserToConvert = createUserToConvert.body().jsonPath().getString("data.authenticatedUser.email");
        String usernameOfNonBcryptUserToConvert = UtilIT.getUsernameFromResponse(createUserToConvert);
        System.out.println("usernameOfBcryptUserToConvert: " + usernameOfNonBcryptUserToConvert);
        String newEmailAddressToUse = "builtin2shib." + UUID.randomUUID().toString().substring(0, 8) + "@mailinator.com";
//        String password = "sha-1Pass";
        String password = usernameOfNonBcryptUserToConvert;
        Response convertToSha1 = convertUserFromBcryptToSha1(BuiltinUserIdOfBcryptUserToConvert, password);
        convertToSha1.prettyPrint();
        convertToSha1.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response setAllowApiTokenLookupViaApi = UtilIT.setSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi, "true");
        setAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(OK.getStatusCode());
        
        password = "sha-1Pass";
        Response getApiTokenUsingUsername = UtilIT.getApiTokenUsingUsername(usernameOfNonBcryptUserToConvert, password);
        assertEquals(200, getApiTokenUsingUsername.getStatusCode());
        
        Response removeAllowApiTokenLookupViaApi = UtilIT.deleteSetting(SettingsServiceBean.Key.AllowApiTokenLookupViaApi);
        removeAllowApiTokenLookupViaApi.then().assertThat()
                .statusCode(200);

        String data = emailOfNonBcryptUserToConvert + ":" + password + ":" + newEmailAddressToUse;
        System.out.println("data: " + data);

        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        String dataWithBadPassword = emailOfNonBcryptUserToConvert + ":" + "badPassword" + ":" + newEmailAddressToUse;
        Response makeShibUserWrongSha1Password = UtilIT.migrateBuiltinToShib(dataWithBadPassword, superuserApiToken);
        makeShibUserWrongSha1Password.prettyPrint();
        makeShibUserWrongSha1Password.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("[\"User doesn't know password.\"]"));

        Response makeShibUser = UtilIT.migrateBuiltinToShib(data, superuserApiToken);
        makeShibUser.prettyPrint();
        makeShibUser.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.affiliation", equalTo("TestShib Test IdP"));

    }

    private Response convertUserFromBcryptToSha1(long idOfBcryptUserToConvert, String password) {
        JsonObjectBuilder data = Json.createObjectBuilder();
        data.add("builtinUserId", idOfBcryptUserToConvert);
        data.add("password", password);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(data.build().toString())
                .post("/api/admin/convertUserFromBcryptToSha1");
        return response;
    }

}
