package edu.harvard.iq.dataverse.workflow.internalspi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizedExternalStepTest {

    @Test
    void testRequestEntityIsSetCorrectly() throws Exception {
        // Mock the static field before any code that uses it
        // Mock and set BrandingUtil.dataverseService
        edu.harvard.iq.dataverse.DataverseServiceBean mockDataverseService = mock(edu.harvard.iq.dataverse.DataverseServiceBean.class);
        when(mockDataverseService.getRootDataverseName()).thenReturn("Root");

        Field dataverseServiceField = BrandingUtil.class.getDeclaredField("dataverseService");
        dataverseServiceField.setAccessible(true);
        dataverseServiceField.set(null, mockDataverseService);

        SettingsServiceBean mockSettings = mock(SettingsServiceBean.class);
        when(mockSettings.getValueForKey(any())).thenReturn(null);

        Field field = BrandingUtil.class.getDeclaredField("settingsService");
        field.setAccessible(true);
        field.set(null, mockSettings);


        // Prepare parameters
        Map<String, String> params = new HashMap<>();
        params.put("method", "POST");
        params.put("url", "http://example.com/api");
        params.put("contentType", "application/json");
        params.put("body", "{\"invocationId\": \"${invocationId}\"}");

        // Mock context
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getInvocationId()).thenReturn("12345");
        when(context.getDataset()).thenReturn(MockDataset.create());

        // Create the step
        AuthorizedExternalStep step = new AuthorizedExternalStep(params);


        // Directly test buildMethod to verify the request entity
        HttpPost request = (HttpPost) step.buildMethod(false, context);
        StringEntity entity = (StringEntity) request.getEntity();
        assertNotNull(entity);
        assertEquals("application/json", entity.getContentType());
        String body = new String(entity.getContent().readAllBytes());
        assertTrue(body.contains("\"invocationId\": \"12345\""));
    }

    static class MockDataverse extends edu.harvard.iq.dataverse.Dataverse {
        @Override
        public java.util.List getCitationDatasetFieldTypes() {
            return new ArrayList<>();
        }
    }

    // Helper mock dataset
    static class MockDataset extends edu.harvard.iq.dataverse.Dataset {
        private edu.harvard.iq.dataverse.GlobalId globalId;
        private edu.harvard.iq.dataverse.Dataverse owner = mock(edu.harvard.iq.dataverse.Dataverse.class);

        static MockDataset create() {
            MockDataset ds = new MockDataset();
            ds.setId(1L);
            ds.setIdentifier("ds1");
            ds.globalId = new edu.harvard.iq.dataverse.GlobalId("doi", "10.1234/DS1", null, null, null, null);
            return ds;
        }

        @Override
        public edu.harvard.iq.dataverse.GlobalId getGlobalId() {
            return globalId;
        }

        @Override
        public edu.harvard.iq.dataverse.Dataverse getOwner() {
            return owner;
        }
    }
}