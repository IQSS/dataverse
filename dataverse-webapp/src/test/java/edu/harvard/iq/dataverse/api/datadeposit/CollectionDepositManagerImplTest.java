package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.imports.ImportGenericServiceBean;
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
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CollectionDepositManagerImplTest {

    @InjectMocks
    private CollectionDepositManagerImpl collectionDepositManagerImpl;

    @Mock
    private DataverseDao dataverseDao;
    @Mock
    private DatasetDao datasetDao;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private SwordAuth swordAuth;
    @Mock
    private UrlManagerServiceBean urlManagerServiceBean;
    @Mock
    private EjbDataverseEngine engineSvc;
    @Mock
    private DatasetFieldServiceBean datasetFieldService;
    @Mock
    private ImportGenericServiceBean importGenericService;
    @Mock
    private SwordServiceBean swordService;
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private SystemConfig systemConfig;

    @Mock
    private HttpServletRequest request;
    @Mock
    private SwordConfiguration swordConfig;

    // -------------------- TESTS --------------------

    @Test
    public void createNew__readonly_mode() throws SwordError, SwordServerException, SwordAuthException {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        // when
        Executable createNewOperation = () -> collectionDepositManagerImpl.createNew("collectionUri", new Deposit(),
                new AuthCredentials("", "", ""), swordConfig);
        // then
        assertThrows(SwordError.class, createNewOperation);
    }
}
