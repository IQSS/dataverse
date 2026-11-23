package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DatasetServiceBeanTest {

    @InjectMocks
    private DatasetServiceBean datasetService;

    @Mock
    private DvObjectServiceBean dvObjectService;

    @Mock
    private PermissionServiceBean permissionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindDatasetUserCanSeeByGlobalId_NormalDataset() {
        String pid = "doi:10.5072/FK2/NORMAL";
        Dataset dataset = new Dataset();
        
        when(dvObjectService.findByGlobalId(pid, DvObject.DType.Dataset)).thenReturn(dataset);
        
        DataverseRequest req = mock(DataverseRequest.class);
        
        Dataset result = datasetService.findDatasetUserCanSeeByGlobalId(pid, req);
        
        assertNotNull(result);
        assertEquals(dataset, result);
        verify(permissionService, never()).hasLocallyFAIRAccess(any(), any());
    }

    @Test
    public void testFindDatasetUserCanSeeByGlobalId_LocallyFair_HasAccess() {
        String pid = "doi:10.5072/FK2/FAIR";
        Dataset dataset = mock(Dataset.class);
        when(dataset.isLocallyFAIR()).thenReturn(true);
        
        when(dvObjectService.findByGlobalId(pid, DvObject.DType.Dataset)).thenReturn(dataset);
        
        DataverseRequest req = mock(DataverseRequest.class);
        when(permissionService.hasLocallyFAIRAccess(req, dataset)).thenReturn(true);
        
        Dataset result = datasetService.findDatasetUserCanSeeByGlobalId(pid, req);
        
        assertNotNull(result);
        assertEquals(dataset, result);
    }

    @Test
    public void testFindDatasetUserCanSeeByGlobalId_LocallyFair_NoAccess() {
        String pid = "doi:10.5072/FK2/FAIR";
        Dataset dataset = mock(Dataset.class);
        when(dataset.isLocallyFAIR()).thenReturn(true);
        
        when(dvObjectService.findByGlobalId(pid, DvObject.DType.Dataset)).thenReturn(dataset);
        
        DataverseRequest req = mock(DataverseRequest.class);
        when(permissionService.hasLocallyFAIRAccess(req, dataset)).thenReturn(false);
        
        Dataset result = datasetService.findDatasetUserCanSeeByGlobalId(pid, req);
        
        assertNull(result, "Dataset should be hidden if Locally FAIR and no access");
    }
}
