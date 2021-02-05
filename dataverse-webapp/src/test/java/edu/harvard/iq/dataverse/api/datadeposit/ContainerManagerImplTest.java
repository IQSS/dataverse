package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.imports.ImportGenericServiceBean;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContainerManagerImplTest {

    @InjectMocks
    private ContainerManagerImpl containerManagerImpl;
    
    @Mock
    private EjbDataverseEngine engineSvc;
    @Mock
    private DataverseDao dataverseDao;
    @Mock
    private DatasetDao datasetDao;
    @Mock
    private IndexServiceBean indexService;
    @Mock
    private EntityManager em;
    @Mock
    private ImportGenericServiceBean importGenericService;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private DataFileServiceBean datafileService;
    @Mock
    private SwordAuth swordAuth;
    @Mock
    private UrlManagerServiceBean urlManagerServiceBean;
    @Mock
    private SystemConfig systemConfig;
    @Mock
    private SwordServiceBean swordService;

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private SwordConfiguration swordConfig;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void replaceMetadata__readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        Executable createNewOperation = () -> containerManagerImpl.replaceMetadata("collectionUri", new Deposit(),
                new AuthCredentials("", "", ""), swordConfig);
        // then
        assertThrows(SwordError.class, createNewOperation);
    }
    
    @Test
    public void deleteContainer__readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        Executable createNewOperation = () -> containerManagerImpl.deleteContainer("collectionUri",
                new AuthCredentials("", "", ""), swordConfig);
        // then
        assertThrows(SwordError.class, createNewOperation);
    }
}
