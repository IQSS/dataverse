package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import static java.lang.Thread.sleep;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;
import junit.framework.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DataCaptureModuleServiceBeanIT {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getCanonicalName());

    @Test
    public void testUploadRequestWorking() {
        AuthenticatedUser user = makeAuthenticatedUser("Lauren", "Ipsum");
        Dataset dataset = new Dataset();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long timeInMillis = calendar.getTimeInMillis();
        dataset.setId(timeInMillis);
        UploadRequestResponse uploadRequestResponse = DataCaptureModuleServiceBean.makeUploadRequest("http://localhost:8888", user, dataset);
        assertEquals(200, uploadRequestResponse.getHttpStatusCode());
        assertTrue(uploadRequestResponse.getResponse().contains("recieved"));
        assertEquals("\nrecieved\n", uploadRequestResponse.getResponse());
    }

    @Test
    public void testScriptRequestWorking() {
        long expectedToWork = 3813;
        ScriptRequestResponse scriptRequestResponseGood = DataCaptureModuleServiceBean.getRsyncScriptForDataset("http://localhost:8888", expectedToWork);
        System.out.println("script: " + scriptRequestResponseGood.getScript());
        Assert.assertTrue(scriptRequestResponseGood.getScript().startsWith("#!"));
    }

    @Test
    public void testScriptRequestNotWorking() {
        long notExpectedToWork = Long.MAX_VALUE;
        ScriptRequestResponse scriptRequestResponseBad = DataCaptureModuleServiceBean.getRsyncScriptForDataset("http://localhost:8888", notExpectedToWork);
        assertNull(scriptRequestResponseBad.getScript());
    }

    @Test
    public void testBothSteps() throws InterruptedException {
        // Step 1: Upload request
        AuthenticatedUser user = makeAuthenticatedUser("Lauren", "Ipsum");
        Dataset dataset = new Dataset();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long timeInMillis = calendar.getTimeInMillis();
        dataset.setId(timeInMillis);
        UploadRequestResponse uploadRequestResponse = DataCaptureModuleServiceBean.makeUploadRequest("http://localhost:8888", user, dataset);
        assertEquals(200, uploadRequestResponse.getHttpStatusCode());
        assertTrue(uploadRequestResponse.getResponse().contains("recieved"));
        assertEquals("\nrecieved\n", uploadRequestResponse.getResponse());

        sleep(DataCaptureModuleServiceBean.millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls);

        // Step 2: Script request.
        ScriptRequestResponse scriptRequestResponseGood = DataCaptureModuleServiceBean.getRsyncScriptForDataset("http://localhost:8888", dataset.getId());
        System.out.println("script: " + scriptRequestResponseGood.getScript());
        assertNotNull(scriptRequestResponseGood.getScript());
        assertTrue(scriptRequestResponseGood.getScript().startsWith("#!"));

    }

}
