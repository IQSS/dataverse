package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectGuestBookServiceTest {

    @InjectMocks
    private SelectGuestBookService selectGuestBookService;

    @Mock
    private DatasetService datasetService;

    private static final long UTC_CLOCK_TIME = 1111111111L;

    private Clock utcClock = Clock.fixed(Instant.ofEpochMilli(UTC_CLOCK_TIME), ZoneId.of("UTC"));

    @BeforeEach
    public void beforeEach() {
        selectGuestBookService.setSystemTime(utcClock);
    }

    // -------------------- TESTS --------------------

    @Test
    public void saveGuestbookChanges_WithRemovedGuestbook() {
        //given
        DatasetVersion datasetVersion = preparedDatasetVersion();

        Guestbook oldGuestbook = new Guestbook();
        datasetVersion.getDataset().setGuestbook(oldGuestbook);

        //when
        when(datasetService.updateDatasetGuestbook(datasetVersion.getDataset())).thenReturn(datasetVersion.getDataset());
        Dataset savedDataset = selectGuestBookService.saveGuestbookChanges(datasetVersion.getDataset(),
                                                                           Option.none());

        //then
        Assert.assertEquals(utcClock.instant(), savedDataset.getGuestbookChangeTime().get().toInstant());
        Assert.assertNull(savedDataset.getGuestbook());
    }

    @Test
    public void saveGuestbookChanges_WithAddedGuestbook() {
        //given
        DatasetVersion datasetVersion = preparedDatasetVersion();

        Guestbook freshGuestbook = new Guestbook();

        //when
        when(datasetService.updateDatasetGuestbook(datasetVersion.getDataset())).thenReturn(datasetVersion.getDataset());
        Dataset savedDataset = selectGuestBookService.saveGuestbookChanges(datasetVersion.getDataset(),
                                                                           Option.of(freshGuestbook));

        //then
        Assert.assertEquals(utcClock.instant(), savedDataset.getGuestbookChangeTime().get().toInstant());
        Assert.assertEquals(freshGuestbook, savedDataset.getGuestbook());
    }

    @Test
    public void saveGuestbookChanges_WithNothingChanged() {
        //given
        DatasetVersion datasetVersion = preparedDatasetVersion();

        //when
        Dataset savedDataset = selectGuestBookService.saveGuestbookChanges(datasetVersion.getDataset(),
                                                                           Option.none());

        //then
        Assert.assertEquals(Option.none(), savedDataset.getGuestbookChangeTime());
        Assert.assertNull(savedDataset.getGuestbook());
    }

    @Test
    public void saveGuestbookChanges() {
        //given
        DatasetVersion datasetVersion = preparedDatasetVersion();

        Guestbook addedGuestbook = new Guestbook();
        addedGuestbook.setId(2L);

        //when
        when(datasetService.updateDatasetGuestbook(datasetVersion.getDataset())).thenReturn(datasetVersion.getDataset());


        Dataset savedDataset = selectGuestBookService.saveGuestbookChanges(datasetVersion.getDataset(),
                                                                           Option.of(addedGuestbook));

        //then
        Assert.assertEquals(addedGuestbook, savedDataset.getGuestbook());

    }

    // -------------------- PRIVATE --------------------

    private DatasetVersion preparedDatasetVersion() {
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);

        return datasetVersion;
    }
}