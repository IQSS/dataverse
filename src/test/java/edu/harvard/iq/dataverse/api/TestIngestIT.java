package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;

public class TestIngestIT {

    @Test
    public void test50by1000() {
        // cp scripts/search/data/tabular/50by1000.dta /tmp
        String fileName = "/tmp/50by1000.dta";
        String fileType = "application/x-stata";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 50", response.body().asString().split("\n")[0]);
    }

    @Test
    public void testStata13Auto() {
        // curl https://www.stata-press.com/data/r13/auto.dta > /tmp/stata13-auto.dta
        String fileName = "/tmp/stata13-auto.dta";
        String fileType = "application/x-stata-13";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

    @Test
    public void testStata14Aggregated() {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        String fileName = "/tmp/2018_04_06_Aggregated_dataset_v2.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata-14";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 227", response.body().asString().split("\n")[0]);
    }

    @Ignore
    @Test
    public void testStata14MmPublic() {
        // TODO: This file was downloaded at random. We could keep trying to get it to ingest.
        // https://dataverse.harvard.edu/file.xhtml?fileId=2775556 Stata 14: mm_public_120615_v14.dta
        String fileName = "/tmp/mm_public_120615_v14.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata-14";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        // We don't know how many variables it has. Probably not 12.
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

    @Ignore
    @Test
    public void testStata15() {
        // TODO: Find a Stata 15 file to test with
    }

}
