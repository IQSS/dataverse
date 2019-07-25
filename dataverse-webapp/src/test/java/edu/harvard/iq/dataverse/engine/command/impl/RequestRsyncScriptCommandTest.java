package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datacapturemodule.UploadRequestResponse;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RequestRsyncScriptCommandTest {

    @Mock
    private SettingsServiceBean settingsService;

    private TestDataverseEngine testEngine;
    Dataset dataset;

    public RequestRsyncScriptCommandTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.DataCaptureModuleUrl)).thenReturn("http://localhost:8888");

        testEngine = new TestDataverseEngine(new TestCommandContext() {

            @Override
            public DataCaptureModuleServiceBean dataCaptureModule() {
                return new DataCaptureModuleServiceBean() {

                    @Override
                    public UploadRequestResponse requestRsyncScriptCreation(String jsonString, String dcmBaseUrl) {
                        return new UploadRequestResponse(200, "myResponse");
                    }

                    @Override
                    public ScriptRequestResponse retreiveRequestedRsyncScript(String datasetIdentifier, String dcmBaseUrl) {
                        int httpStatusCode = 200;
                        long userId = 123l;
                        String script = "theScript";
                        ScriptRequestResponse scriptRequestResponse = new ScriptRequestResponse(httpStatusCode, datasetIdentifier, userId, script);
                        return scriptRequestResponse;
                    }
                };

            }

            @Override
            public SettingsServiceBean settings() {
                return settingsService;
            }
        });
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testHappyPath() throws Exception {
        dataset = new Dataset();
        dataset.setIdentifier("42");
        HttpServletRequest aHttpServletRequest = null;
        DataverseRequest dataverseRequest = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"), aHttpServletRequest);
        ScriptRequestResponse scriptRequestResponse = testEngine.submit(new RequestRsyncScriptCommand(dataverseRequest, dataset));
        assertEquals("theScript", scriptRequestResponse.getScript());
    }
}
