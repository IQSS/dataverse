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
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RequestRsyncScriptCommandTest {

    private TestDataverseEngine testEngine;
    Dataset dataset;

    public RequestRsyncScriptCommandTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
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
                return new MockSettingsSvc();
            }
        });
    }

    @AfterEach
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

    private static class MockSettingsSvc extends SettingsServiceBean {

        @Override
        public String getValueForKey(SettingsServiceBean.Key key) {
            switch (key) {
                case DataCaptureModuleUrl:
                    return "http://localhost:8888";
                default:
                    return null;
            }
        }
    }
}
