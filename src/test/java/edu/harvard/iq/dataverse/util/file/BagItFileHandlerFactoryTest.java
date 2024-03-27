package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagValidator;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author adaybujeda
 */
@ExtendWith(MockitoExtension.class)
public class BagItFileHandlerFactoryTest {

    @Mock
    private SettingsServiceBean settingsService;

    @InjectMocks
    private BagItFileHandlerFactory target;

    @Test
    public void initialize_should_set_BagItFileHandler_to_empty_if_BagItHandler_is_not_enabled() {
        Mockito.when(settingsService.isTrue(BagItFileHandlerFactory.BAGIT_HANDLER_ENABLED_SETTING, false)).thenReturn(false);

        target.initialize();

        MatcherAssert.assertThat(target.getBagItFileHandler().isEmpty(), Matchers.is(true));
        Mockito.verify(settingsService).isTrue(BagItFileHandlerFactory.BAGIT_HANDLER_ENABLED_SETTING, false);
        Mockito.verifyNoMoreInteractions(settingsService);
    }

    @Test
    public void initialize_should_set_BagItFileHandler_if_BagItHandler_is_enabled() {
        Mockito.when(settingsService.isTrue(BagItFileHandlerFactory.BAGIT_HANDLER_ENABLED_SETTING, false)).thenReturn(true);

        target.initialize();

        MatcherAssert.assertThat(target.getBagItFileHandler().isEmpty(), Matchers.is(false));
        Mockito.verify(settingsService).get(BagValidator.BagValidatorSettings.JOB_POOL_SIZE.getSettingsKey());
        Mockito.verify(settingsService).get(BagValidator.BagValidatorSettings.MAX_ERRORS.getSettingsKey());
        Mockito.verify(settingsService).get(BagValidator.BagValidatorSettings.JOB_WAIT_INTERVAL.getSettingsKey());
    }

}