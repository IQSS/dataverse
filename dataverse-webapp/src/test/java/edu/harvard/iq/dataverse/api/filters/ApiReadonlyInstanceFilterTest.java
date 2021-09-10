package edu.harvard.iq.dataverse.api.filters;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiReadonlyInstanceFilterTest {

    @InjectMocks
    private ApiReadonlyInstanceFilter readonlyInstanceFilter;

    @Mock
    private SystemConfig systemConfig;

    @Captor
    private ArgumentCaptor<Response> responseCaptor;

    @Mock
    private ContainerRequestContext requestContext;


    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should do nothing when instance is not readonly")
    public void filter__not_readonly_instance() throws IOException {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(false);
        // when
        readonlyInstanceFilter.filter(requestContext);
        // then
        verifyZeroInteractions(requestContext);
    }

    @Test
    @DisplayName("Should abort processing with error response when instance is readonly")
    public void filter__readonly_instance() throws IOException {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        readonlyInstanceFilter.filter(requestContext);
        // then
        verify(requestContext).abortWith(responseCaptor.capture());
        Response response = responseCaptor.getValue();
        assertErrorResponse(response, 500, "Instance is readonly", null);
        verifyNoMoreInteractions(requestContext);
    }

    // -------------------- PRIVATE --------------------

    private void assertErrorResponse(Response response, int expectedStatusCode, String expectedMessage, String expectedIncidentId) {
        assertEquals(expectedStatusCode, response.getStatus());
        assertTrue(response.getEntity() instanceof ApiErrorResponseDTO);

        ApiErrorResponseDTO apiErrorResponse = (ApiErrorResponseDTO)response.getEntity();
        assertEquals(expectedStatusCode, apiErrorResponse.getCode());
        assertEquals(AbstractApiBean.STATUS_ERROR, apiErrorResponse.getStatus());
        assertEquals(expectedMessage, apiErrorResponse.getMessage());
        assertEquals(expectedIncidentId, apiErrorResponse.getIncidentId());
    }
}
