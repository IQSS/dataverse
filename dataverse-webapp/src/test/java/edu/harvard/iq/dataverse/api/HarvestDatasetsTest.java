package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class HarvestDatasetsTest {

    @Mock
    private DatasetService datasetService;

    @Mock
    private DatasetDao datasetDao;

    @Mock
    private HttpServletRequest request;

    @Mock
    protected SystemConfig systemConfig;

    @Mock
    private AuthenticationServiceBean authSvc;

    @InjectMocks
    private HarvestDatasets harvestDatasets = new HarvestDatasets(datasetService);

    AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @BeforeEach
    void setUp() {
        String token = "123456";
        when(request.getParameter("key")).thenReturn(token);
        authenticatedUser.setSuperuser(true);
        when(authSvc.lookupUser(token)).thenReturn(authenticatedUser);
        when(systemConfig.isReadonlyMode()).thenReturn(true);
    }

    // -------------------- TESTS --------------------

    @Test
    void updateAllDatasetsLastChangeTime() {
        // when
        harvestDatasets.updateAllDatasetsLastChangeTime();

        // then
        verify(datasetService, times(1)).updateAllLastChangeForExporterTime();
    }

    @Test
    void updateAllDatasetsLastChangeTime__noSuperuser() {
        // given
        authenticatedUser.setSuperuser(false);

        // when
        Response response = harvestDatasets.updateAllDatasetsLastChangeTime();

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        verify(datasetService, never()).updateAllLastChangeForExporterTime();
    }

    @Test
    void updateDatasetLastChangeTime() {
        // given
        Dataset dataset = new Dataset();
        when(datasetDao.find(1L)).thenReturn(dataset);

        // when
        harvestDatasets.updateDatasetLastChangeTime("1");

        // then
        verify(datasetService, times(1)).updateLastChangeForExporterTime(dataset);
    }

    @Test
    void updateDatasetLastChangeTime__noSuperuser() {
        // given
        authenticatedUser.setSuperuser(false);

        // when
        Response response = harvestDatasets.updateDatasetLastChangeTime("1");

        // then
        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        verify(datasetService, never()).updateLastChangeForExporterTime(any(Dataset.class));
    }
}