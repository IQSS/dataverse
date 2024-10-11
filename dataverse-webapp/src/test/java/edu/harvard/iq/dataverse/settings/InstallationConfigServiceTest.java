package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
public class InstallationConfigServiceTest {

    @InjectMocks
    private InstallationConfigService installationConfigService;

    @Mock
    private SettingsServiceBean settingService;

    @Mock
    private DataverseDao dataverseDao;


    @BeforeEach
    public void setup() {
        Dataverse dataverse = new Dataverse();
        dataverse.setName("rootName");
        when(dataverseDao.findRootDataverse()).thenReturn(dataverse);

    }

    // -------------------- TESTS --------------------

    @Test
    public void getNameOfInstallation() {
        // when
        String installationName = installationConfigService.getNameOfInstallation();
        // then
        assertEquals("rootName", installationName);
    }

    @Test
    public void getSupportTeamName() {
        // given
        when(settingService.getValueForKey(Key.SystemEmail)).thenReturn("fake@domain.com");
        // when
        String supportTeamName = installationConfigService.getSupportTeamName();
        // then
        assertEquals("rootName Support", supportTeamName);
    }

    @Test
    public void getSupportTeamName_FROM_SYSTEM_EMAIL() {
        // given
        when(settingService.getValueForKey(Key.SystemEmail)).thenReturn("Fake Name <fake@domain.com>");
        // when
        String supportTeamName = installationConfigService.getSupportTeamName();
        // then
        assertEquals("Fake Name", supportTeamName);
    }
}
