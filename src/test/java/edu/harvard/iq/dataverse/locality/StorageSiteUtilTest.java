package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class StorageSiteUtilTest {

    @Test
    public void testParse() throws Exception {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.HOSTNAME, "myHostname");
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, "rsync");
        StorageSite result = StorageSiteUtil.parse(job.build());
        result.setId(42l);
        String output = JsonUtil.prettyPrint(result.toJsonObjectBuilder().build().toString());
        System.out.println("output: " + output);
    }

    @Test
    void testMissingHostname() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, "rsync");
        JsonObject sut = job.build();
        assertThrows(IllegalArgumentException.class, () -> StorageSiteUtil.parse(sut));
    }

    @Test
    void testBadProtocol() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.HOSTNAME, "myHostname");
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, "junk");
        JsonObject sut = job.build();
        assertThrows(IllegalArgumentException.class, () -> StorageSiteUtil.parse(sut));
    }

    @Test
    void testNonBoolean() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.HOSTNAME, "myHostname");
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, "not a boolean");
        job.add(StorageSite.TRANSFER_PROTOCOLS, "rsync");
        JsonObject sut = job.build();
        assertThrows(IllegalArgumentException.class, () -> StorageSiteUtil.parse(sut));
    }

    @Test
    void testSecondPrimaryNotAllowed() {
        StorageSite newStorageSite = new StorageSite();
        newStorageSite.setPrimaryStorage(true);
        List<StorageSite> exitingSites = new ArrayList<>();
        StorageSite existingSite1 = new StorageSite();
        existingSite1.setPrimaryStorage(true);
        exitingSites.add(existingSite1);
        assertThrows(Exception.class, () -> StorageSiteUtil.ensureOnlyOnePrimary(newStorageSite, exitingSites));
    }

    @Test
    public void testSecondNonPrimaryIsAllowed() throws Exception {
        StorageSite newStorageSite = new StorageSite();
        List<StorageSite> exitingSites = new ArrayList<>();
        StorageSite existingSite1 = new StorageSite();
        existingSite1.setPrimaryStorage(true);
        exitingSites.add(existingSite1);
        StorageSiteUtil.ensureOnlyOnePrimary(newStorageSite, exitingSites);
    }

    @Test
    public void testCanAddPrimaryWhenNoExistingPrimary() throws Exception {
        StorageSite newStorageSite = new StorageSite();
        newStorageSite.setPrimaryStorage(true);
        List<StorageSite> exitingSites = new ArrayList<>();
        StorageSite existingSite1 = new StorageSite();
        exitingSites.add(existingSite1);
        StorageSiteUtil.ensureOnlyOnePrimary(newStorageSite, exitingSites);
    }

    @Test
    public void testJustForCodeCoverage() {
        new StorageSiteUtil();
    }

}
