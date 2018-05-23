package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.response.Response;
import static org.junit.Assert.assertEquals;
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
    public void testStata14Auto() {
        // curl https://www.stata-press.com/data/r14/auto.dta > /tmp/stata14-auto.dta
        String fileName = "/tmp/stata14-auto.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        boolean stata14Supported = false;
        if (!stata14Supported) {
            return;
        }
        // Not sure if there are 12 vars or not.
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

    @Test
    public void testStata15Auto() {
        // curl https://www.stata-press.com/data/r15/auto.dta > /tmp/stata15-auto.dta
        String fileName = "/tmp/stata15-auto.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        boolean stata15Supported = false;
        if (!stata15Supported) {
            return;
        }
        // Not sure if there are 12 vars or not.
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

}
