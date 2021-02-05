package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MediaResourceManagerImplTest {

    @InjectMocks
    private MediaResourceManagerImpl mediaResourceManagerImpl;
    
    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DatasetDao datasetDao;
    @Mock
    private DataFileServiceBean dataFileService;
    @Mock
    private IngestServiceBean ingestService;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private SettingsServiceBean settingsSvc;
    @Mock
    private SystemConfig systemConfig;
    @Mock
    private SwordAuth swordAuth;
    @Mock
    private UrlManagerServiceBean urlManagerServiceBean;

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private SwordConfiguration swordConfig;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void deleteMediaResource__readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        Executable deleteMediaResourceOperation = () -> mediaResourceManagerImpl.deleteMediaResource("collectionUri",
                new AuthCredentials("", "", ""), swordConfig);
        // then
        assertThrows(SwordError.class, deleteMediaResourceOperation);
    }
    
    @Test
    public void addResource__readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        Executable addResourceOperation = () -> mediaResourceManagerImpl.addResource("collectionUri", new Deposit(),
                new AuthCredentials("", "", ""), swordConfig);
        // then
        assertThrows(SwordError.class, addResourceOperation);
    }
}
