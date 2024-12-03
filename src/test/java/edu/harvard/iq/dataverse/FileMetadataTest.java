package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileMetadataTest {
  FileMetadata metadata;

  @BeforeEach
  void setUp() {
    metadata = new FileMetadata();
  }

  @Test
  void testGetFileDateToDisplay_missingFile() {
    assertEquals("", metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithoutDates() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(null);
    datafile.setCreateDate(null);
    metadata.setDataFile(datafile);
    assertEquals("", metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithoutPublicationDate() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(null);
    datafile.setCreateDate(Timestamp.valueOf("2019-01-01 00:00:00"));
    metadata.setDataFile(datafile);
    assertEquals(DateFormat.getDateInstance(DateFormat.DEFAULT, BundleUtil.getCurrentLocale()).format(Date.from(Instant.parse("2019-01-01T00:00:00.00Z"))), metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithPublicationDate() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(Timestamp.valueOf("2019-02-02 00:00:00"));
    datafile.setCreateDate(Timestamp.valueOf("2019-01-02 00:00:00"));
    metadata.setDataFile(datafile);
    assertEquals(DateFormat.getDateInstance(DateFormat.DEFAULT, BundleUtil.getCurrentLocale()).format(Date.from(Instant.parse("2019-02-02T00:00:00.00Z"))), metadata.getFileDateToDisplay());
  }

  
  static boolean restrict = false;
  static String apiToken = "b59051fa-5902-4e90-922c-9b3be8f83215";
  @Test
  void testRapidRestrict() {
      String origFileId = "10420";
      //unrestrict file good
      Response unrestrictResponse = restrictFile(origFileId.toString(), restrict, apiToken);
      unrestrictResponse.prettyPrint();
      unrestrictResponse.then().assertThat()
              .body("data.message", equalTo("File DSC_2870_th.jpg unrestricted."))
              .statusCode(OK.getStatusCode());

      //unrestrict already restricted file bad
      Response unrestrictResponseBad = restrictFile(origFileId.toString(), restrict, apiToken);
      unrestrictResponseBad.prettyPrint();
      unrestrictResponseBad.then().assertThat()
              .body("message", equalTo("Problem trying to update restriction status on DSC_2870_th.jpg: File DSC_2870_th.jpg is already unrestricted"))
              .statusCode(BAD_REQUEST.getStatusCode());

      restrict = true; // revert to original state
      //restrict file good
      Response restrictResponse = restrictFile(origFileId.toString(), restrict, apiToken);
      restrictResponse.prettyPrint();
      restrictResponse.then().assertThat()
              .body("data.message", equalTo("File DSC_2870_th.jpg restricted."))
              .statusCode(OK.getStatusCode());

      //restrict already restricted file bad
      Response restrictResponseBad = restrictFile(origFileId.toString(), restrict, apiToken);
      restrictResponseBad.prettyPrint();
      restrictResponseBad.then().assertThat()
              .body("message", equalTo("Problem trying to update restriction status on DSC_2870_th.jpg: File DSC_2870_th.jpg is already restricted"))
              .statusCode(BAD_REQUEST.getStatusCode());

  }
  
  @Test
  void testFileMetadataChange() {
      String origFileId = "10423";
      String updateDescription = "New description.";
      String updateCategory = "New category";
      String updateDataFileTag = "Survey";
      String updateLabel = "newName.tab";
  String updateJsonString = "{\"description\":\""+updateDescription+"\",\"label\":\""+updateLabel+"\",\"categories\":[\""+updateCategory+"\"],\"dataFileTags\":[\""+updateDataFileTag+"\"],\"forceReplace\":false ,\"junk\":\"junk\"}";
  Response updateMetadataResponse = updateFileMetadata(origFileId.toString(), updateJsonString, apiToken);
  msg("Error msg:" + updateMetadataResponse.body().asPrettyString());
  assertEquals(OK.getStatusCode(), updateMetadataResponse.getStatusCode());  

  //String updateMetadataResponseString = updateMetadataResponse.body().asString();
  //Response getUpdatedMetadataResponse = UtilIT.getDataFileMetadataDraft(origFileId, apiToken);
  //String getUpMetadataResponseString = getUpdatedMetadataResponse.body().asString();
  msg("Draft (should be updated):");
  //msg(getUpMetadataResponseString);
  }
  
  public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";
  
static Response restrictFile(String fileIdOrPersistentId, boolean restrict, String apiToken) {
    String idInPath = fileIdOrPersistentId; // Assume it's a number.
    String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
    if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
        idInPath = ":persistentId";
        optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
    }
    Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .body(restrict)
            .put("http://ec2-3-238-245-253.compute-1.amazonaws.com/api/files/" + idInPath + "/restrict" + optionalQueryParam);
    return response;
}

static Response updateFileMetadata(String fileIdOrPersistentId, String jsonAsString, String apiToken) {
    String idInPath = fileIdOrPersistentId; // Assume it's a number.
    String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
    if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
        idInPath = ":persistentId";
        optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
    }
    RequestSpecification requestSpecification = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken);
    if (jsonAsString != null) {
        requestSpecification.multiPart("jsonData", jsonAsString);
    }
    return requestSpecification
            .post("http://ec2-3-238-245-253.compute-1.amazonaws.com/api/files/" + idInPath + "/metadata" + optionalQueryParam);
}
private void msg(String m){
    System.out.println(m);
}
}