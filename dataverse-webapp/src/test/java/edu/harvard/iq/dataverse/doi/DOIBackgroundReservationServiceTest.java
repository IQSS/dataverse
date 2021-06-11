package edu.harvard.iq.dataverse.doi;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.globalid.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DOIBackgroundReservationServiceTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;

    @Mock
    private Timer timer;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private DOIDataCiteServiceBean doiDataCiteServiceBean;

    @Mock
    private IndexServiceBean indexServiceBean;

    @InjectMocks
    private DOIBackgroundReservationService doiBackgroundReservationService;

    @Test
    void registerDoiPeriodically_WithDifferentProvider() {
        //when
        when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider)).thenReturn("FAKE");
        doiBackgroundReservationService.reserveDoiPeriodically(timer);

        //then
        verify(timer, times(0)).schedule(any(TimerTask.class), any(Long.class), any(Long.class));
    }

    @Test
    void registerDoiPeriodically_WithMissingInterval() {
        //when
        doReturn("DataCite").when(settingsServiceBean).getValueForKey(SettingsServiceBean.Key.DoiProvider);
        doReturn("").when(settingsServiceBean).getValueForKey(SettingsServiceBean.Key.DoiBackgroundReservationInterval);
        doiBackgroundReservationService.reserveDoiPeriodically(timer);

        //then
        verify(timer, times(0)).schedule(any(TimerTask.class), any(Long.class), any(Long.class));
    }

    @Test
    void registerDoiPeriodically() {
        //when
        doReturn("DataCite").when(settingsServiceBean).getValueForKey(SettingsServiceBean.Key.DoiProvider);
        doReturn("20").when(settingsServiceBean).getValueForKey(SettingsServiceBean.Key.DoiBackgroundReservationInterval);

        doiBackgroundReservationService.reserveDoiPeriodically(timer);

        //then
        verify(timer, times(1)).schedule(any(TimerTask.class), any(Long.class), any(Long.class));
    }

    @Test
    void registerDataCiteIdentifier() {
        //when
        final Dataset dataset = prepareDataset();
        when(datasetRepository.findByNonRegisteredIdentifier()).thenReturn(Lists.newArrayList(dataset));
        when(doiDataCiteServiceBean.alreadyExists(any(GlobalId.class))).thenReturn(false);
        when(datasetRepository.save(any(Dataset.class))).thenReturn(dataset);

        doiBackgroundReservationService.registerDataCiteIdentifier();

        //then
        Assertions.assertThat(dataset.isIdentifierRegistered()).isTrue();

    }

    private Dataset prepareDataset(){
        Dataset dataset = new Dataset();

        dataset.setIdentifier("TestID");
        dataset.setProtocol("doi");
        dataset.setAuthority("FK");

        return dataset;
    }
}