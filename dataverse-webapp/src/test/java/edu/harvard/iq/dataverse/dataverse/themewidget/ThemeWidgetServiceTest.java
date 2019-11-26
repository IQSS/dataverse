package edu.harvard.iq.dataverse.dataverse.themewidget;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ThemeWidgetServiceTest {
    @InjectMocks
    private ThemeWidgetService themeWidgetService;

    @Mock
    private EjbDataverseEngine commandEngine;
    @Mock
    private DataverseRequestServiceBean dvRequestService;

    private Dataverse dataverse;

    @BeforeEach
    public void setUp() {
        dataverse = new Dataverse();
        dataverse.setName("testDv");
        dataverse.setId(1L);

        when(commandEngine.submit(any(UpdateDataverseThemeCommand.class))).thenReturn(this.dataverse);
    }

    @Test
    public void saveOrUpdateUploadedTheme() {
        // given
        File file = new File (getClass().getClassLoader()
                .getResource("images/banner.png").getFile());
        // when
        themeWidgetService.saveOrUpdateUploadedTheme(dataverse, file);

        // then
        verify(commandEngine, times(1)).submit(any(UpdateDataverseThemeCommand.class));
    }

    @Test
    public void inheritThemeFromRoot() {
        // given
        dataverse.setDataverseTheme(new DataverseTheme());

        // when
        themeWidgetService.inheritThemeFromRoot(dataverse);

        // then
        verify(commandEngine, times(1)).submit(any(UpdateDataverseThemeCommand.class));
        assertNull(dataverse.getDataverseTheme());
    }
}
