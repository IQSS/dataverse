package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.locality.StorageSite;
import edu.harvard.iq.dataverse.util.SystemConfig;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

public class StorageSitesIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testListAll() {
        Response list = UtilIT.listStorageSites();
        list.prettyPrint();
    }

    @Test
    public void testGet() {
        int id = 2;
        Response storageSite = UtilIT.getStorageSitesById(id);
        storageSite.prettyPrint();
    }

    @Test
    public void testAddSite() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.HOSTNAME, "myHostname");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, SystemConfig.TransferProtocols.RSYNC.toString());
        Response add = UtilIT.addStorageSite(job.build());
        add.prettyPrint();
    }

    @Test
    public void testSetPrimary() {
        int id = 2;
        String changeTo = "false";
        Response set = UtilIT.setPrimaryLocationBoolean(id, changeTo);
        set.prettyPrint();
    }

    @Test
    public void testDelete() {
        int doomed = 1;
        Response delete = UtilIT.deleteStorageSite(doomed);
        delete.prettyPrint();
    }

}
