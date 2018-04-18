package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.junit.Test;

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

    @Test(expected = IllegalArgumentException.class)
    public void testMissingHostname() throws Exception {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, "rsync");
        StorageSiteUtil.parse(job.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadProtocol() throws Exception {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(StorageSite.HOSTNAME, "myHostname");
        job.add(StorageSite.NAME, "myName");
        job.add(StorageSite.PRIMARY_STORAGE, true);
        job.add(StorageSite.TRANSFER_PROTOCOLS, "junk");
        StorageSiteUtil.parse(job.build());
    }

}
