package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import static java.lang.Thread.sleep;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * These tests are not expected to pass unless you have a Data Capture Module
 * (DCM) installed and configured properly. They are intended to help a
 * developer get set up for DCM development.
 */
public class DataCaptureModuleServiceBeanIT {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBeanIT.class.getCanonicalName());

    @Test
    public void testUploadRequestAndScriptRequest() throws InterruptedException, DataCaptureModuleException {
        String dcmBaseUrl = "http://localhost:8888";
        DataCaptureModuleServiceBean dataCaptureModuleServiceBean = new DataCaptureModuleServiceBean();

        // Step 1: Upload request
        AuthenticatedUser authenticatedUser = makeAuthenticatedUser("Lauren", "Ipsum");
        Dataset dataset = new Dataset();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long timeInMillis = calendar.getTimeInMillis();
        dataset.setId(timeInMillis);
        String jsonString = DataCaptureModuleUtil.generateJsonForUploadRequest(authenticatedUser, dataset).toString();
        logger.info("jsonString: " + jsonString);
        UploadRequestResponse uploadRequestResponse = dataCaptureModuleServiceBean.requestRsyncScriptCreation(jsonString, dcmBaseUrl + DataCaptureModuleServiceBean.uploadRequestPath);
        assertEquals(200, uploadRequestResponse.getHttpStatusCode());
        assertTrue(uploadRequestResponse.getResponse().contains("recieved"));
        assertEquals("\nrecieved\n", uploadRequestResponse.getResponse());

        // If you comment this out, expect to see a 404 when you try to download the script.
        sleep(DataCaptureModuleServiceBean.millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls);

        // Step 2: Script request.
        ScriptRequestResponse scriptRequestResponseGood = dataCaptureModuleServiceBean.retreiveRequestedRsyncScript(dataset.getId(), dcmBaseUrl + DataCaptureModuleServiceBean.scriptRequestPath);
        System.out.println("script: " + scriptRequestResponseGood.getScript());
        assertNotNull(scriptRequestResponseGood.getScript());
        assertTrue(scriptRequestResponseGood.getScript().startsWith("#!"));

    }

}
