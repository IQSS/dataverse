package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author madryk
 */
@RunWith(MockitoJUnitRunner.class)
public class InstallationConfigServiceTest {

    @InjectMocks
    private InstallationConfigService installationConfigService;

    @Mock
    private SettingsServiceBean settingService;

    @Mock
    private DataverseServiceBean dataverseService;


    @Before
    public void setup() {
        Dataverse dataverse = new Dataverse();
        dataverse.setName("rootName");
        when(dataverseService.findRootDataverse()).thenReturn(dataverse);

        when(settingService.getValueForKey(Key.SystemEmail)).thenReturn("fake@domain.com");
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
